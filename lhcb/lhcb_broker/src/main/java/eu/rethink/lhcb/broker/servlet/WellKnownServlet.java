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

package eu.rethink.lhcb.broker.servlet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Http Servlet that is supposed to handle any request to /.well-known/*
 */
public class WellKnownServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(WellKnownServlet.class);

    private LeshanServer server;

    private static final String WELLKNOWN_PREFIX = "/.well-known";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private BiMap<String, Client> nameClientMap = HashBiMap.create();

    private static final int EXTENDED_MODEL_ID = 3000;
    private static final int DEVICE_ID_RESOURCE_ID = 0;

    /**
     * Creates a new WellKnownServlet that translates requests to /well-known/*
     * to requests to clients of the provided Leshan server
     *
     * @param server - Leshan server that LHCB clients connect to
     */
    public WellKnownServlet(LeshanServer server) {
        this.server = server;
        server.getClientRegistry().addListener(new ClientRegistryListener() {
            @Override
            public void registered(Client client) {
                LOG.info("Client registered: " + client);
                analyzeClient(client);
            }

            @Override
            public void updated(Client client) {
                LOG.info("Client updated: " + client);
                removeClient(client);
                analyzeClient(client);
            }

            @Override
            public void unregistered(Client client) {
                LOG.info("Client unregistered: " + client);
                removeClient(client);
            }
        });
    }

    /**
     * Analyze the provided client if it is a LHCB Client and extract its device ID
     *
     * @param client - newly registered/updated client
     */
    private void analyzeClient(final Client client) {
        //LOG.debug("analyzing client: " + client);

        // check object links
        boolean foundExtended = false;
        for (LinkObject linkObject : client.getObjectLinks()) {
            //LOG.debug(String.format("checking link:\n%s", gson.toJson(linkObject)));
            String[] urlParts = linkObject.getUrl().split("/");
            //LOG.debug("urlParts: " + gson.toJson(urlParts));

            if (urlParts.length > 0 && EXTENDED_MODEL_ID == Integer.valueOf(urlParts[1])) {
                // found extended device model id, making this client a lhcb client
                foundExtended = true;
                break;
            }
        }

        if (foundExtended) {
            // custom lhcb model was found, now extract device ID
            ReadRequest rr = new ReadRequest(EXTENDED_MODEL_ID, 0);
            try {
                ReadResponse response = server.send(client, rr);
                if (response.getCode() == ResponseCode.CONTENT) {
                    response.getContent().accept(new LwM2mNodeVisitor() {
                        @Override
                        public void visit(LwM2mObject object) {
                            LOG.warn("object visit: " + object + " (this should not happen)");
                        }

                        @Override
                        public void visit(LwM2mObjectInstance instance) {
                            // extract information
                            LwM2mResource resource = instance.getResource(DEVICE_ID_RESOURCE_ID);
                            String deviceId = (String) resource.getValue();
                            nameClientMap.put(deviceId, client);
                        }

                        @Override
                        public void visit(LwM2mResource resource) {
                            LOG.warn("resource visit: " + resource + " (this should not happen)");
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove the recently disconnected client from the internal map
     *
     * @param client
     */
    private void removeClient(Client client) {
        //LOG.debug("removing client: " + client);
        nameClientMap.inverse().remove(client);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI();
        LOG.info("Handling GET for: " + path);

        //path should start with /.well-known
        // remove /.well-known from path
        path = StringUtils.removeStart(path, WELLKNOWN_PREFIX);

        //LOG.debug("adapted path: " + path);
        //LOG.debug("path length: " + path.length());

        // split path up
        String[] pathParts = path.length() > 1 ? path.substring(1).split(path, '/') : new String[0];

        //LOG.debug("pathParts: " + gson.toJson(pathParts));
        String deviceId;
        String responseText;
        int responseCode;

        if (pathParts.length > 0) {
            deviceId = pathParts[0];

            Client client = nameClientMap.get(deviceId);
            if (client != null) {
                try {
                    // get connectivity monitoring instance
                    ReadResponse response = server.send(client, new ReadRequest(4, 0));

                    if (response.getCode().equals(ResponseCode.CONTENT)) {
                        responseCode = HttpServletResponse.SC_OK;

                        // get models from folder
                        LwM2mModel objectModels = server.getModelProvider().getObjectModel(client);
                        ObjectModel objectModel = objectModels.getObjectModel(4);
                        //LOG.debug("objectModel: " + gson.toJson(objectModel));
                        Map<Integer, ResourceModel> modelResources = objectModel.resources;
                        JsonObject json = gson.toJsonTree(response).getAsJsonObject();
                        JsonObject content = json.getAsJsonObject("content");
                        content.remove("id");
                        content.addProperty("name", objectModel.name);
                        JsonObject resources = content.getAsJsonObject("resources");
                        List<String> removeList = new LinkedList<>();
                        Map<String, JsonElement> addMap = new LinkedHashMap<>();
                        for (Map.Entry<String, JsonElement> entry : resources.entrySet()) {
                            JsonObject resource = entry.getValue().getAsJsonObject();
                            resource.remove("type");
                            resource.remove("id");
                            //resources.add(modelResources.get(Integer.valueOf(entry.getKey())).name, resource);
                            addMap.put(modelResources.get(Integer.valueOf(entry.getKey())).name, resource);
                            //resources.remove(entry.getKey());
                            removeList.add(entry.getKey());
                        }
                        for (String s : removeList) {
                            resources.remove(s);
                        }
                        for (Map.Entry<String, JsonElement> entry : addMap.entrySet()) {
                            resources.add(entry.getKey(), entry.getValue());
                        }
                        responseText = gson.toJson(json);
                    } else {
                        responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                        responseText = gson.toJson(response);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    responseCode = HttpServletResponse.SC_GATEWAY_TIMEOUT;
                    responseText = e.getMessage();
                }
            } else {
                responseCode = HttpServletResponse.SC_NOT_FOUND;
                responseText = "Unable to find client with device ID " + deviceId;
            }

        } else {
            responseCode = HttpServletResponse.SC_BAD_REQUEST;
            responseText = "Please specify one of the available devices: " + nameClientMap.keySet();
        }

        //LOG.debug("returning response: " + responseText);
        if (responseCode == HttpServletResponse.SC_OK) {
            resp.setStatus(responseCode);
            resp.addHeader("Access-Control-Allow-Origin", "*");
            resp.getWriter().write(responseText);
            resp.getWriter().flush();
        } else {
            resp.sendError(responseCode, responseText);
        }

    }
}
