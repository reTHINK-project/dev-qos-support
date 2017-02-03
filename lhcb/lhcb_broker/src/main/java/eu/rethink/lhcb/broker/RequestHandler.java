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

import com.google.gson.JsonObject;
import eu.rethink.lhcb.utils.RequestCallback;
import eu.rethink.lhcb.utils.message.ExecuteMessage;
import eu.rethink.lhcb.utils.message.Message;
import eu.rethink.lhcb.utils.message.exception.InvalidMessageException;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static eu.rethink.lhcb.utils.Utils.gson;
import static eu.rethink.lhcb.utils.Utils.parseCMReadResponse;

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
        } else if (msg.getClient() == null) {
            LOG.debug("No Client provided -> Returning all clients");
            try {
                cb.response(msg.response(gson.toJsonTree(server.getClientRegistry().allClients())));
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

                        CompletableFuture<JsonObject> promise = parseCMReadResponse(LHCBBroker.cMObjectModel, response);
                        promise.thenAccept(jsonObject -> {
                            try {
                                cb.response(msg.response(jsonObject));
                            } catch (InvalidMessageException e) {
                                cb.error(e);
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
                } else if (msg instanceof ExecuteMessage) {
                    ExecuteRequest request = new ExecuteRequest(3000, 0, 0, ((ExecuteMessage) msg).getArgs().toString());
                    server.send(client, request, (response) -> {
                        LOG.debug("Got response: {}", response);
                        try {
                            cb.response(msg.response(null));
                        } catch (InvalidMessageException e) {
                            e.printStackTrace();
                        }
                    }, e -> {
                        LOG.error("ExecuteRequest " + request.toString() + " to " + client.getEndpoint() + " failed", e);
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
}