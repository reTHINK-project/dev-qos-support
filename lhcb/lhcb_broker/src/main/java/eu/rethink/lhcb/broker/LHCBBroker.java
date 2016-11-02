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

package eu.rethink.lhcb.broker;

import eu.rethink.lhcb.broker.provider.CustomModelProvider;
import eu.rethink.lhcb.broker.servlet.EventServlet;
import eu.rethink.lhcb.broker.servlet.WellKnownServlet;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Last Hop Connectivity Broker - Broker module implementation
 */
public class LHCBBroker {
    private static final Logger LOG = LoggerFactory.getLogger(LHCBBroker.class);

    private LeshanServer leshanServer;
    private Server server;

    private int httpPort = 8080;
    private int httpsPort = 8443;
    private int coapPort = 5683;
    private int coapsPort = 5684;

    public LHCBBroker() {
        LOG.info("LHCB Broker Version {}", getClass().getPackage().getImplementationVersion());
    }

    public static void main(String[] args) {
        LHCBBroker broker = new LHCBBroker();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            arg = arg.toLowerCase();

            switch (arg) {
                case "-httpport":
                case "-http":
                case "-h":
                    String rawHttpPort = args[++i];
                    int httpPort = 0;
                    try {
                        httpPort = Integer.parseInt(rawHttpPort);
                    } catch (Exception e) {
                        LOG.error("Unable to parse HTTP port '" + rawHttpPort + "'", e);
                    }
                    broker.setHttpPort(httpPort);
                    break;
                case "-sslport":
                case "-ssl":
                case "-s":
                case "-httpsport":
                case "-https":
                case "-hs":
                    String rawHttpsPort = args[++i];
                    int httpsPort = 0;
                    try {
                        httpsPort = Integer.parseInt(rawHttpsPort);
                    } catch (Exception e) {
                        LOG.error("Unable to parse HTTPS/SSL port '" + rawHttpsPort + "'", e);
                    }
                    broker.setHttpsPort(httpsPort);
                    break;
                case "-coapport":
                case "-coap":
                case "-c":
                    String rawCoapPort = args[++i];
                    int coapPort = 0;
                    try {
                        coapPort = Integer.parseInt(rawCoapPort);
                    } catch (Exception e) {
                        LOG.error("Unable to parse CoAP port '" + rawCoapPort + "'", e);
                    }
                    broker.setCoapPort(coapPort);
                    break;
                case "-coapsport":
                case "-coaps":
                case "-cs":
                    String rawCoapsPort = args[++i];
                    int coapsPort = 0;
                    try {
                        coapsPort = Integer.parseInt(rawCoapsPort);
                    } catch (Exception e) {
                        LOG.error("Unable to parse CoAPs port '" + rawCoapsPort + "'", e);
                    }
                    broker.setCoapsPort(coapsPort);
                    break;
                //case "-keystorePath":
                //case "-kp":
                //    // TODO implementation
                //    break;
                //case "-truststorePath":
                //case "-tp":
                //    // TODO implementation
                //    break;
                //case "-keystorePassword":
                //case "-kpw":
                //    // TODO implementation
                //    break;
                //case "-keyManagerPassword":
                //case "-kmpw":
                //    // TODO implementation
                //    break;
                //case "-truststorePassword":
                //case "-tpw":
                //    // TODO implementation
                //    break;
                default:
                    // unknown arg
                    i++;
            }
        }

        broker.start();
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public void setCoapPort(int coapPort) {
        this.coapPort = coapPort;
    }

    public void setCoapsPort(int coapsPort) {
        this.coapsPort = coapsPort;
    }

    public void start() {
        // setup SLF4JBridgeHandler needed for proper logging
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        // don't let Californium use file
        NetworkConfig.createStandardWithoutFile();
        // create Leshan server
        LeshanServerBuilder lsb = new LeshanServerBuilder();
        lsb.setLocalAddress("0", coapPort);
        lsb.setLocalSecureAddress("0", coapsPort);
        lsb.setObjectModelProvider(new CustomModelProvider());
        leshanServer = lsb.build();
        leshanServer.start();

        // Now prepare and start jetty
        server = new Server();

        // HTTP Configuration
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.addCustomizer(new SecureRequestCustomizer());

        // === jetty-http.xml ===
        ServerConnector http = new ServerConnector(server,
                new HttpConnectionFactory(http_config));
        http.setHost("0.0.0.0");
        http.setPort(httpPort);
        http.setIdleTimeout(30000);
        server.addConnector(http);

        // === jetty-https.xml ===
        // SSL Context Factory
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath("ssl/keystore");
        sslContextFactory.setKeyStorePassword("OBF:1vub1vnw1shm1y851vgl1vg91y7t1shw1vn61vuz");
        sslContextFactory.setKeyManagerPassword("OBF:1vub1vnw1shm1y851vgl1vg91y7t1shw1vn61vuz");
        sslContextFactory.setTrustStorePath("ssl/keystore");
        sslContextFactory.setTrustStorePassword("OBF:1vub1vnw1shm1y851vgl1vg91y7t1shw1vn61vuz");
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
        sslConnector.setPort(httpsPort);
        server.addConnector(sslConnector);

        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(LHCBBroker.class.getClassLoader().getResource("webapp").toExternalForm());
        //root.setParentLoaderPriority(true);
        server.setHandler(root);

        //ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", true, false);
        ServletHolder servletHolder = new ServletHolder(new WellKnownServlet(leshanServer));
        root.addServlet(servletHolder, "/.well-known/*");

        ServletHolder eventHolder = new ServletHolder(new EventServlet(leshanServer));
        root.addServlet(eventHolder, "/event/*");

        try {
            LOG.info("Server should be available at: " + server.getURI());
            LOG.info("Starting server...");
            server.start();
        } catch (Exception e) {
            LOG.error("HTTP server error", e);
        }
    }
}
