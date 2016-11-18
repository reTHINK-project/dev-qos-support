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

import eu.rethink.lhcb.broker.message.Message;
import eu.rethink.lhcb.broker.message.exception.InvalidMessageException;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.rethink.lhcb.utils.Utils.gson;

/**
 * Created by Robert Ende on 10.11.16.
 */
public class RequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

    private LeshanServer server;

    public RequestHandler(LeshanServer server) {
        this.server = server;
    }

    public void handleRequest(Message msg, RequestCallback cb) {
        LOG.debug("handleRequest: {}", msg.toString());

        if (msg.type == null) {
            String error = "Message has no type";
            LOG.debug(error);
            try {
                cb.response(msg.errorResponse(error));
            } catch (InvalidMessageException e) {
                cb.error(e);
            }
        } else if (msg.type != Message.Type.read && msg.type != Message.Type.write) {
            String error = "Unsupported message type: " + msg.type;
            LOG.debug(error);
            try {
                cb.response(msg.errorResponse(error));
            } catch (InvalidMessageException e) {
                cb.error(e);
            }
        } else if (msg.getClient() == null) {
            LOG.debug("No Client provided -> Returning all clients");
            try {
                cb.response(msg.response(server.getClientRegistry().allClients()));
            } catch (InvalidMessageException e) {
                cb.error(e);
            }
        } else {
            Client client = server.getClientRegistry().get(msg.getClient());
            if (client != null) {
                if (msg.type == Message.Type.read) {
                    ReadRequest request = new ReadRequest(4, 0);

                    server.send(client, request, (response) -> {
                        LOG.debug("Got response for request: {}", request);
                        response.getContent().accept(new LwM2mNodeVisitor() {
                            @Override
                            public void visit(LwM2mObject object) {
                                String error = "Got object instead of instance! This should not happen!";
                                LOG.error(error);
                                try {
                                    cb.response(msg.errorResponse(error));
                                } catch (InvalidMessageException e) {
                                    cb.error(e);
                                }
                            }

                            @Override
                            public void visit(LwM2mObjectInstance instance) {
                                // requested complete instance
                                String responseText = gson.toJson(instance);
                                LOG.trace("visit instance: {}", responseText);
                                try {
                                    cb.response(msg.response(gson.toJsonTree(instance.getResources())));
                                } catch (InvalidMessageException e) {
                                    cb.error(e);
                                }
                            }

                            @Override
                            public void visit(LwM2mResource resource) {
                                String error = "Got resource instead of instance! This should not happen!";
                                LOG.error(error);
                                try {
                                    cb.response(msg.errorResponse(error));
                                } catch (InvalidMessageException e) {
                                    cb.error(e);
                                }
                            }
                        });
                    }, e -> {
                        LOG.error("ReadRequest " + request.toString() + " to " + client.getEndpoint() + " failed", e);
                        try {
                            cb.response(msg.errorResponse(e));
                        } catch (InvalidMessageException e1) {
                            cb.error(e);
                        }
                    });
                }
            } else {
                String error = "Client " + msg.getClient() + " not found";
                LOG.debug(error);
                try {
                    cb.response(msg.errorResponse(error));
                } catch (InvalidMessageException e) {
                    cb.error(e);
                }
            }
        }
    }

    public interface RequestCallback {
        void response(Message msg);

        void error(Exception e);
    }
}