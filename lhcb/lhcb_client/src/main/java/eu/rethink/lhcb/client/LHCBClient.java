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
import eu.rethink.lhcb.client.objects.Device;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

import static org.eclipse.leshan.LwM2mId.*;
import static org.eclipse.leshan.client.object.Security.noSec;

/**
 * Last Hop Connectivity Broker - Client module implementation
 */
public class LHCBClient {
    private static final Logger LOG = LoggerFactory.getLogger(LHCBClient.class);
    private String serverHost = "localhost";
    private int serverPort = 5683;
    private Class<? extends LwM2mInstanceEnabler> connectivityMonitorClass = ConnectivityMonitor.class;

    public static void main(String[] args) {
        LHCBClient client = new LHCBClient();
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
                    client.setConnectivityMonitorClass(ConnectivityMonitorDummy.class);
                default:
                    i++;
            }
        }

        client.start();
    }

    public void setConnectivityMonitorClass(Class<? extends LwM2mInstanceEnabler> connectivityMonitorClass) {
        this.connectivityMonitorClass = connectivityMonitorClass;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void start() {
        start(null);
    }

    public void start(List<ObjectModel> objectModels) {
        // get default models
        if (objectModels == null) {
            objectModels = ObjectLoader.loadDefault();
        }

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(objectModels));

        // set dummy Device
        initializer.setClassForObject(3, Device.class);
        initializer.setClassForObject(4, connectivityMonitorClass);
        //initializer.setInstancesForObject(3000, new ExtendedDevice());

        String serverURI = String.format("coap://%s:%s", serverHost, serverPort);

        initializer.setInstancesForObject(SECURITY, noSec(serverURI, 123));
        initializer.setInstancesForObject(SERVER, new Server(123, 30, BindingMode.U, false));

        List<LwM2mObjectEnabler> enablers = initializer.create(SECURITY, SERVER, DEVICE, CONNECTIVITY_MONITORING); // 0 = ?, 1 = accessControl, 3 = Device, 4 = ConMon

        // Create client
        LeshanClientBuilder builder = new LeshanClientBuilder(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
        //builder.setLocalAddress(localAddress, localPort);
        //builder.setLocalSecureAddress(secureLocalAddress, secureLocalPort);
        builder.setObjects(enablers);
        final LeshanClient client = builder.build();
        // Start the client
        client.start();
    }

}
