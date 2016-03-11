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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Http Servlet that is supposed to handle any request to /.well-known/*
 */
public class WellKnownServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(WellKnownServlet.class);

    private LeshanServer server;

    private static final String WELLKNOWN_PREFIX = "/.well-known";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Creates a new WellKnownServlet that translates requests to /well-known/*
     * to requests to clients of the provided Leshan server
     *
     * @param server - Leshan server that LHCB clients connect to
     */
    public WellKnownServlet(LeshanServer server) {
        this.server = server;
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
        String[] pathParts = path.length() > 1 ? path.substring(1).split("/") : new String[0];

        //LOG.debug("pathParts: " + gson.toJson(pathParts));
        String endpoint;
        final String[] responseText = new String[1];
        int responseCode;

        if (pathParts.length > 0) {
            endpoint = pathParts[0];

            Client client = server.getClientRegistry().get(endpoint);
            if (client != null) {
                try {
                    ReadResponse response;

                    // get connectivity monitoring instance
                    if (pathParts.length > 1) {
                        int resourceId = Integer.parseInt(pathParts[1]);
                        response = server.send(client, new ReadRequest(4, 0, resourceId));
                    } else {
                        response = server.send(client, new ReadRequest(4, 0));
                    }

                    if (response.getCode().equals(ResponseCode.CONTENT)) {
                        responseCode = HttpServletResponse.SC_OK;

                        response.getContent().accept(new LwM2mNodeVisitor() {
                            @Override
                            public void visit(LwM2mObject object) {
                                // requested all instances
                                responseText[0] = gson.toJson(object);
                                LOG.debug("visit object: {}", responseText[0]);
                            }

                            @Override
                            public void visit(LwM2mObjectInstance instance) {
                                // requested complete instance
                                responseText[0] = gson.toJson(instance);
                                LOG.debug("visit instance: {}", responseText[0]);

                            }

                            @Override
                            public void visit(LwM2mResource resource) {
                                // requested specific resource
                                responseText[0] = gson.toJson(resource);
                                LOG.debug("visit resource: {}", responseText[0]);
                            }
                        });
                    } else {
                        responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                        responseText[0] = gson.toJson(response);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    responseCode = HttpServletResponse.SC_GATEWAY_TIMEOUT;
                    responseText[0] = e.getMessage();
                }
            } else {
                responseCode = HttpServletResponse.SC_NOT_FOUND;
                responseText[0] = "Unable to find client " + endpoint;
            }

        } else {
            //responseCode = HttpServletResponse.SC_BAD_REQUEST;
            //responseText = "Please provide one of the following device IDs: " + nameClientMap.keySet();
            // TODO: might change this so you need to clarify that you want a list, e.g. /.well-known/list
            responseCode = HttpServletResponse.SC_OK;
            Collection<Client> clients = server.getClientRegistry().allClients();
            Iterator<Client> iterator = clients.iterator();
            String[] endpoints = new String[clients.size()];
            int i = 0;
            while (iterator.hasNext()) {
                Client next = iterator.next();
                endpoints[i++] = next.getEndpoint();
            }
            responseText[0] = gson.toJson(endpoints);
        }

        //LOG.debug("returning response: " + responseText);
        if (responseCode == HttpServletResponse.SC_OK) {
            resp.setStatus(responseCode);
            resp.addHeader("Access-Control-Allow-Origin", "*");
            resp.getWriter().write(responseText[0]);
            resp.getWriter().flush();
        } else {
            resp.sendError(responseCode, responseText[0]);
        }

    }
}
