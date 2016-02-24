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

package eu.rethink.lhcb.broker.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * This request handler handles requests to a /.well-known/* path and translates them to LwM2M requests to the
 * Leshan Server
 */
public class RequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

    private static final String WELLKNOWN_PREFIX = "/.well-known/";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private List<Client> clientList = new LinkedList<>();

    private LeshanServer server;

    public RequestHandler(LeshanServer server) {
        this.server = server;
        server.getClientRegistry().addListener(new ClientRegistryListener() {
            @Override
            public void registered(Client client) {
                LOG.info("Client registered:\n" + gson.toJson(client));
                clientList.add(client);
            }

            @Override
            public void updated(Client client) {
                LOG.info("Client updated:\n" + gson.toJson(client));
            }

            @Override
            public void unregistered(Client client) {
                LOG.info("Client unregistered:\n" + gson.toJson(client));
                clientList.remove(client);
            }
        });
    }

    public String doGET(String path) {
        LOG.info("Handling GET for: " + path);

        //path should start with /.well-known/
        //but coap has no slash at the start, so check for it and prepend it if necessary.
        if (!path.startsWith("/"))
            path = "/" + path;

        //remove /.well-known/ from path
        path = StringUtils.removeStart(path, WELLKNOWN_PREFIX);
        LOG.debug("adapted path: " + path);
        //split path up
        String[] pathParts = StringUtils.split(path, '/');

        return gson.toJson(clientList);
    }
}
