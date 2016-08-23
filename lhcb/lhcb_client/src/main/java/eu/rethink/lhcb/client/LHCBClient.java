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

import eu.rethink.lhcb.client.objects.ConnectivityMonitor;
import eu.rethink.lhcb.client.objects.ConnectivityMonitorDummy;
import eu.rethink.lhcb.client.objects.ConnectivityMonitorSimple;
import eu.rethink.lhcb.client.objects.Device;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.List;
import java.util.Random;

import static org.eclipse.leshan.LwM2mId.*;
import static org.eclipse.leshan.client.object.Security.noSec;

/**
 * Last Hop Connectivity Broker - Client module implementation
 */
public class LHCBClient {
    private static final Logger LOG = LoggerFactory.getLogger(LHCBClient.class);

    private LeshanClient client = null;

    // adjustable parameters
    private String serverHost = "localhost";
    private int serverPort = 5683;
    private ConnectivityMonitor connectivityMonitorInstance = null;
    private String name = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));

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
     * Instance to be used for the ConnectivityMonitoring.
     *
     * @param connectivityMonitorInstance - ConnectivityMonitor instance to be used
     */
    public void setConnectivityMonitorInstance(ConnectivityMonitor connectivityMonitorInstance) {
        this.connectivityMonitorInstance = connectivityMonitorInstance;
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

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(objectModels));

        // set dummy Device
        initializer.setClassForObject(3, Device.class);
        //initializer.setClassForObject(4, connectivityMonitorClass);
        if (connectivityMonitorInstance == null)
            connectivityMonitorInstance = new ConnectivityMonitorSimple();

        connectivityMonitorInstance.startRunner();

        initializer.setInstancesForObject(4, connectivityMonitorInstance);
        //initializer.setInstancesForObject(3000, new ExtendedDevice());

        String serverURI = String.format("coap://%s:%s", serverHost, serverPort);

        initializer.setInstancesForObject(SECURITY, noSec(serverURI, 123));
        initializer.setInstancesForObject(SERVER, new Server(123, 30, BindingMode.U, false));

        List<LwM2mObjectEnabler> enablers = initializer.create(SECURITY, SERVER, DEVICE, CONNECTIVITY_MONITORING); // 0 = ?, 1 = accessControl, 3 = Device, 4 = ConMon

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(name);
        //builder.setLocalAddress(localAddress, localPort);
        //builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setObjects(enablers);
        client = builder.build();
        // Start the client
        client.start();
    }

    /**
     * Stop the LHCB Client.
     */
    public void stop() {
        if (client != null) {
            client.stop(true);
        }
    }

}
