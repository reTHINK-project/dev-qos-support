/*
 * Copyright [2015-2017] Fraunhofer Gesellschaft e.V., Institute for
 * Open Communication Systems (FOKUS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.rethink.lhcb.client;

import eu.rethink.lhcb.client.objects.*;
import eu.rethink.lhcb.client.websocket.ClientWebSocketServlet;
import eu.rethink.lhcb.utils.Utils;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.security.KeyStore;
import java.util.List;
import java.util.Random;

import static eu.rethink.lhcb.utils.Utils.gson;
import static org.eclipse.leshan.LwM2mId.*;
import static org.eclipse.leshan.client.object.Security.noSec;

/**
 * Last Hop Connectivity Broker - Client module implementation
 */
public class LHCBClient {
    private static final Logger LOG = LoggerFactory.getLogger(LHCBClient.class);

    private LeshanClient client = null;

    // adjustable parameters
    public static String serverHost = "localhost";
    private int serverPort = 5683;
    private ConnectivityMonitor connectivityMonitorInstance = null;
    public static LwM2mObjectEnabler connectivityMonitorEnabler;
    public static String name = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    private Server server;
    private KeyStore keyStore = null;
    private String keyStorePassword = "OBF:1vub1vnw1shm1y851vgl1vg91y7t1shw1vn61vuz";
    private String keyManagerPassword = "OBF:1vub1vnw1shm1y851vgl1vg91y7t1shw1vn61vuz";
    private String trustStorePassword = "OBF:1vub1vnw1shm1y851vgl1vg91y7t1shw1vn61vuz";
    public static WebSocketClient webSocketClient;
    private ExtendedDevice extendedDevice = null;

    public LHCBClient() {
        LOG.info("LHCB Client Version {}", getClass().getPackage().getImplementationVersion());
    }

    public static void main(String[] args) {
        final LHCBClient client = new LHCBClient();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h":
                case "-host":
                    client.setServerHost(args[++i]);
                    break;
                case "-p":
                case "-port":
                    client.setServerPort(Integer.parseInt(args[++i]));
                    break;
                case "-d":
                case "-dummy":
                    client.setConnectivityMonitorInstance(new ConnectivityMonitorDummy());
                    break;
                case "-n":
                case "-name":
                    client.setName(args[++i]);
                    break;
                default:
                    LOG.info("Unable to handle arg '{}' from {}", arg, args);
                    i++;
                    break;
            }
        }

        client.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                client.stop();
            }
        }));
    }

    /**
     * Set LHCB Client Endpoint name.
     *
     * @param name - Name of this LHCB Client
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set instance to be used for the ConnectivityMonitoring.
     *
     * @param connectivityMonitorInstance - ConnectivityMonitor instance to be used
     */
    public void setConnectivityMonitorInstance(ConnectivityMonitor connectivityMonitorInstance) {
        this.connectivityMonitorInstance = connectivityMonitorInstance;
    }

    /**
     * Get instance to be used for the ConnectivityMonitoring.
     *
     * @return a ConnectivityMonitor instance, or null
     */
    public ConnectivityMonitor getConnectivityMonitorInstance() {
        return connectivityMonitorInstance;
    }

    public ExtendedDevice getExtendedDevice() {
        return extendedDevice;
    }

    public void setExtendedDevice(ExtendedDevice extendedDevice) {
        this.extendedDevice = extendedDevice;
    }

    /**
     * Set LHCB Broker hostname/IP
     *
     * @param serverHost - hostname or IP of LHCB Broker
     */
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    /**
     * Set CoAP port of LHCB Broker
     *
     * @param serverPort - CoAP port of LHCB Broker
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Start the LHCB Client.
     */
    public void start() {
        // setup SLF4JBridgeHandler needed for proper logging
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        // don't let Californium use file
        NetworkConfig.createStandardWithoutFile();
        // get default models
        List<ObjectModel> objectModels = ObjectLoader.loadDefault();
        objectModels.addAll(Utils.getCustomObjectModels());

        LOG.debug("objectModels: {}", gson.toJson(objectModels));

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(objectModels));

        // set dummy Device
        initializer.setClassForObject(3, Device.class);
        //initializer.setClassForObject(3000, ExtendedDevice.class);
        //initializer.setClassForObject(4, connectivityMonitorClass);
        if (connectivityMonitorInstance == null)
            connectivityMonitorInstance = new ConnectivityMonitorSimple();

        connectivityMonitorInstance.startRunner();

        // Now prepare and start jetty
        server = new Server();

        // HTTP Configuration
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.addCustomizer(new SecureRequestCustomizer());

        // === jetty-http.xml ===
        ServerConnector http = new ServerConnector(server,
                new HttpConnectionFactory(http_config));
        http.setHost("0.0.0.0");
        http.setPort(9080);
        http.setIdleTimeout(30000);
        server.addConnector(http);

        // === jetty-https.xml ===
        // SSL Context Factory
        SslContextFactory sslContextFactory = new SslContextFactory(true) {
            @Override
            public void customize(SSLEngine sslEngine) {
                SSLParameters sslParams = sslEngine.getSSLParameters();
                //sslParams.setEndpointIdentificationAlgorithm(_endpointIdentificationAlgorithm);
                sslEngine.setSSLParameters(sslParams);

                if (getWantClientAuth())
                    sslEngine.setWantClientAuth(getWantClientAuth());
                if (getNeedClientAuth())
                    sslEngine.setNeedClientAuth(getNeedClientAuth());

                sslEngine.setEnabledCipherSuites(selectCipherSuites(
                        sslEngine.getEnabledCipherSuites(),
                        sslEngine.getSupportedCipherSuites()));

                sslEngine.setEnabledProtocols(selectProtocols(sslEngine.getEnabledProtocols(), sslEngine.getSupportedProtocols()));
            }
        };
        if (keyStore == null) {
            Resource ks = Resource.newClassPathResource("/keystore.jks");
            sslContextFactory.setKeyStoreResource(ks);
            sslContextFactory.setTrustStoreResource(ks);
        } else {
            sslContextFactory.setKeyStore(keyStore);
        }

        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setTrustStorePassword(trustStorePassword);
        sslContextFactory.setKeyManagerPassword(keyManagerPassword);
        sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

        // SSL HTTP Configuration
        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.addCustomizer(new SecureRequestCustomizer());

        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        sslConnector.setHost("0.0.0.0");
        sslConnector.setPort(9443);
        server.addConnector(sslConnector);

        ServletContextHandler sch = new ServletContextHandler();
        sch.setContextPath("/");
        sch.setResourceBase(".");
        //root.setParentLoaderPriority(true);

        sch.addServlet(new ServletHolder(new ClientWebSocketServlet()), "/ws/*");
        server.setHandler(sch);
        try {
            LOG.info("WebSocketServer should be available at: " + server.getURI());
            LOG.info("Starting server...");
            server.start();
        } catch (Exception e) {
            LOG.error("HTTP server error", e);
        }
        initializer.setInstancesForObject(4, connectivityMonitorInstance);
        //initializer.setInstancesForObject(3000, new ExtendedDevice());

        String serverURI = String.format("coap://%s:%s", serverHost, serverPort);

        initializer.setInstancesForObject(SECURITY, noSec(serverURI, 123));
        initializer.setInstancesForObject(SERVER, new org.eclipse.leshan.client.object.Server(123, 30, BindingMode.U, false));

        if (extendedDevice == null)
            extendedDevice = new ExtendedDevice();

        initializer.setInstancesForObject(3000, extendedDevice);

        List<LwM2mObjectEnabler> enablers = initializer.create(SECURITY, SERVER, DEVICE, CONNECTIVITY_MONITORING, 3000); // 0 = ?, 1 = accessControl, 3 = Device, 4 = ConMon
        for (LwM2mObjectEnabler enabler : enablers) {
            if (enabler.getObjectModel().id == CONNECTIVITY_MONITORING) {
                LOG.info("found correct ENABLER");
                connectivityMonitorEnabler = enabler;
                break;
            }
        }

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(name);
        //builder.setLocalAddress(localAddress, localPort);
        //builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setObjects(enablers);

        client = builder.build();
        // Start the client
        client.start();

        // initialize WebSocketClient
        webSocketClient = new WebSocketClient(sslContextFactory);
        try {
            webSocketClient.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop the LHCB Client.
     */
    public void stop() {
        try {
            connectivityMonitorInstance.stopRunner();
        } catch (Exception e) {
            //e.printStackTrace();
            LOG.warn("error while trying to stop connectivityMonitorInstance", e);
        }

        if (client != null) {
            client.stop(true);
        }

        try {
            server.stop();
        } catch (Exception e) {
            //e.printStackTrace();
            LOG.warn("error while trying to stop http server", e);
        }
    }

    public void setKeyManagerPassword(String keyManagerPassword) {
        this.keyManagerPassword = keyManagerPassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
}
