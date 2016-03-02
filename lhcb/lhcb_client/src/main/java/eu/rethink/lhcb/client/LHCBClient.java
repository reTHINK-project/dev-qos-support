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
import eu.rethink.lhcb.client.objects.Device;
import eu.rethink.lhcb.client.objects.ExtendedDevice;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Robert Ende on 24.02.16.
 */
public class LHCBClient {
    private static final Logger LOG = LoggerFactory.getLogger(LHCBClient.class);
    private String serverHost = "localhost";
    private int serverPort = 5683;

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void start() {
        // get default models
        List<ObjectModel> objectModels = ObjectLoader.loadDefault();

        // add custom models from model.json
        InputStream modelStream = getClass().getResourceAsStream("/model.json");
        objectModels.addAll(ObjectLoader.loadJsonStream(modelStream));

        // map object models by ID
        HashMap<Integer, ObjectModel> map = new HashMap<>();
        for (ObjectModel objectModel : objectModels) {
            map.put(objectModel.id, objectModel);
        }

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer(new LwM2mModel(map));

        // set dummy Device
        initializer.setClassForObject(3, Device.class);
        initializer.setClassForObject(4, ConnectivityMonitor.class);
        initializer.setInstancesForObject(3000, new ExtendedDevice());
        List<ObjectEnabler> enablers = initializer.createMandatory(); // 0 = ?, 1 = accessControl, 3 = Device, 4 = ConMon
        enablers.add(initializer.create(4));
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);
        final LeshanClient leshanClient = new LeshanClient(serverAddress, new ArrayList<LwM2mObjectEnabler>(enablers));

        // Start the client
        leshanClient.start();

        // Register to the server
        //LOG.info(String.format("Registering on %s...", serverAddress));
        final String endpointIdentifier = UUID.randomUUID().toString();
        RegisterResponse response = leshanClient.send(new RegisterRequest(endpointIdentifier));
        if (response == null) {
            LOG.warn("Registration request timeout");
            return;
        }

        LOG.debug("Device Registration (Success? " + response.getCode() + ")");
        if (response.getCode() != ResponseCode.CREATED) {
            System.err.println("If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
            return;
        }

        final String registrationID = response.getRegistrationID();

        LOG.info("Device: Registered Client Catalogue '" + registrationID + "'");

        // Deregister on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (registrationID != null) {
                    LOG.info("Device: Deregistering Client '" + registrationID + "'");
                    leshanClient.send(new DeregisterRequest(registrationID), 1000);
                    leshanClient.stop();
                }
            }
        });
    }

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
                default:
                    i++;
            }
        }

        client.start();
    }

}
