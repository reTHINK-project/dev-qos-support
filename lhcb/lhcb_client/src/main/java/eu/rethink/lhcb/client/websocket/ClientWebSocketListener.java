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

package eu.rethink.lhcb.client.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.rethink.lhcb.client.LHCBClient;
import eu.rethink.lhcb.utils.Utils;
import eu.rethink.lhcb.utils.message.ExecuteMessage;
import eu.rethink.lhcb.utils.message.Message;
import eu.rethink.lhcb.utils.message.ReadMessage;
import eu.rethink.lhcb.utils.message.exception.InvalidMessageException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * WebSocketListener of the Client that handles incoming messages
 */
public class ClientWebSocketListener implements WebSocketListener {
    private static final Logger LOG = Log.getLogger(ClientWebSocketListener.class);
    private Session outbound;

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        /* ignore */
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        this.outbound = null;
        LOG.info("WebSocket Close: {} - {}", statusCode, reason);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.outbound = session;
        LOG.info("WebSocket Connect: {}", session);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOG.warn("WebSocket Error: {}", cause.getLocalizedMessage());

    }

    @Override
    public void onWebSocketText(String message) {
        LOG.info("got a message: {}", message);

        if ((outbound != null) && (outbound.isOpen())) {

            try {
                Message m = Message.fromString(message);
                //LOG.info("parsed message: {}", m);
                if (m instanceof ReadMessage) {
                    if (LHCBClient.connectivityMonitorEnabler != null) {
                        ReadResponse response = LHCBClient.connectivityMonitorEnabler.read(ServerIdentity.SYSTEM, new ReadRequest(0, 0));
                        Utils.parseCMReadResponse(LHCBClient.connectivityMonitorEnabler.getObjectModel(), response).thenAccept(new Consumer<JsonObject>() {
                            @Override
                            public void accept(JsonObject jsonObject) {
                                try {
                                    outbound.getRemote().sendString(m.response(jsonObject).toString());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (InvalidMessageException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {
                        outbound.getRemote().sendString(m.errorResponse("no connectivityMonitorInstance").toString(), null);
                    }
                } else if (m instanceof ExecuteMessage) {
                    LOG.debug("ExecuteMessage for: {}", ((ExecuteMessage) m).getName());
                    switch (((ExecuteMessage) m).getName().toLowerCase()) {
                        case "changeiface":
                            JsonObject valueObj = m.getValue().getAsJsonObject();
                            JsonArray args = valueObj.get("args").getAsJsonArray();
                            String ssid = args.get(0).getAsString();
                            String pw = null;
                            try {
                                pw = args.get(1).getAsString();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                outbound.getRemote().sendString(m.response(new JsonPrimitive(Utils.changeIface(ssid, pw))).toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "getbrokerinfo":
                            try {
                                //TODO get real values from Broker
                                SendReplyWebsocketAdapter adapter = new SendReplyWebsocketAdapter();
                                LOG.debug("Connecting...");
                                try {
                                    Future<Session> promise = LHCBClient.webSocketClient.connect(adapter, URI.create("wss://" + LHCBClient.serverHost + ":" + 8443 + "/ws"));
                                    LOG.debug("Waiting for session");
                                    Session session = promise.get();
                                } catch (ExecutionException e) {
                                    try {
                                        LOG.debug("Connecting with port 8443 failed, trying again with port 443");
                                        Future<Session> promise = LHCBClient.webSocketClient.connect(adapter, URI.create("wss://" + LHCBClient.serverHost + ":" + 443 + "/ws"));
                                        LOG.debug("Waiting for session");
                                        Session session = promise.get();
                                    } catch (ExecutionException e1) {
                                        //e1.printStackTrace();
                                        LOG.warn("All connection attempts failed. Not connected to Broker?");
                                        outbound.getRemote().sendString(m.errorResponse("Unable to connect to LHCBBroker").toString());
                                        return;
                                    }
                                }
                                LOG.debug("got session -> connected? {}", adapter.isConnected());

                                Message msg = new ExecuteMessage(LHCBClient.name, "getBrokerInfo", null);
                                CompletableFuture<Message> msgPromise = adapter.send(msg);
                                msgPromise.thenAccept(new Consumer<Message>() {
                                    @Override
                                    public void accept(Message response) {
                                        JsonObject value = response.getValue().getAsJsonObject();
                                        value.addProperty("name", LHCBClient.name);
                                        try {
                                            outbound.getRemote().sendString(m.response(value).toString());
                                        } catch (InvalidMessageException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            } catch (InvalidMessageException e) {
                e.printStackTrace();
            }
        }
    }
}
