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

import com.google.gson.JsonObject;
import eu.rethink.lhcb.broker.LHCBBroker;
import eu.rethink.lhcb.utils.RequestCallback;
import eu.rethink.lhcb.utils.message.ExecuteMessage;
import eu.rethink.lhcb.utils.message.Message;
import eu.rethink.lhcb.utils.message.ReadMessage;
import eu.rethink.lhcb.utils.message.exception.InvalidMessageException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * WebSocketListener of the Broker that handles incoming messages with the RequestHandler
 */
public class BrokerWebSocketListener implements WebSocketListener {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerWebSocketListener.class);
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
        //LOG.trace("getLocalAddress: {}", session.getLocalAddress());
        //LOG.trace("getRemoteAddress: {}", session.getRemoteAddress());
        //LOG.debug("getRemote: {}", Utils.gson.toJson(session.getRemote()));
        UpgradeRequest upgradeRequest = session.getUpgradeRequest();
        //LOG.trace("upgradeRequest.getExtensions(): {}", upgradeRequest.getExtensions());
        //LOG.trace("upgradeRequest.getHeaders(): {}", upgradeRequest.getHeaders());
        //LOG.trace("upgradeRequest.getHost(): {}", upgradeRequest.getHost());
        //LOG.trace("upgradeRequest.getHeader('Host'): {}", upgradeRequest.getHeader("Host"));
        //LOG.trace("upgradeRequest.getMethod(): {}", upgradeRequest.getMethod());
        //LOG.trace("upgradeRequest.getOrigin(): {}", upgradeRequest.getOrigin());
        //LOG.trace("upgradeRequest.getParameterMap(): {}", upgradeRequest.getParameterMap());
        //LOG.trace("upgradeRequest.getRequestURI(): {}", upgradeRequest.getRequestURI());
        //LOG.debug("getUpgradeResponse: {}", Utils.gson.toJson(session.getUpgradeResponse()));

        //this.outbound.getRemote().sendString("You are now connected to " + this.getClass().getName(), null);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOG.warn("WebSocket Error: {}", cause.getLocalizedMessage());

    }

    @Override
    public void onWebSocketText(String message) {
        if ((outbound != null) && (outbound.isOpen()) && LHCBBroker.requestHandler != null) {
            //LOG.info("Echoing back text message [{}]", message);
            try {
                // parse message
                Message msg = Message.fromString(message);
                LOG.debug("got Message: {}", msg.toString());

                if (msg instanceof ReadMessage) {
                    // handle ReadRequests
                    RequestCallback cb = new RequestCallback() {

                        @Override
                        public void response(Message msg) {
                            try {
                                outbound.getRemote().sendString(msg.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void error(Exception e) {
                            String error = "Request failed.\r\nOriginal request: " + message + "\r\nreason: " + e.getLocalizedMessage();
                            LOG.error(error, e instanceof InvalidMessageException ? null : e);
                            try {
                                outbound.getRemote().sendString(new Message(null, error, Message.Type.error).toString());
                            } catch (IOException | InvalidMessageException e1) {
                                e1.printStackTrace();
                            }
                        }
                    };

                    // handle request, respond with result handled by callback
                    LHCBBroker.requestHandler.handleRequest(msg, cb);
                } else if (msg instanceof ExecuteMessage) {
                    LOG.debug("Is ExecuteMessage");
                    // handle ExecuteRequests
                    switch (((ExecuteMessage) msg).getName().toLowerCase()) {
                        case "getbrokerinfo": // getBrokerInfo returns host and port of Broker HTTP interface
                            JsonObject response = new JsonObject();

                            String[] splitHost = null;
                            if (LHCBBroker.externalHost != null)
                                splitHost = LHCBBroker.externalHost.split(":");
                            response.addProperty("host", splitHost != null ? splitHost[0] : outbound.getLocalAddress().getHostName());
                            response.addProperty("port", splitHost != null && splitHost.length > 1 ? splitHost[1] : String.valueOf(LHCBBroker.httpsPort));
                            try {
                                outbound.getRemote().sendString(msg.response(response).toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "changeiface": // changeIface is a request to connect to a specific WiFi network
                            RequestCallback cb = new RequestCallback() {

                                @Override
                                public void response(Message msg) {
                                    try {
                                        LOG.debug("got response: {}", msg);
                                        outbound.getRemote().sendString(msg.toString());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void error(Exception e) {
                                    String error = "Request failed.\r\nOriginal request: " + message + "\r\nreason: " + e.getLocalizedMessage();
                                    LOG.error(error, e instanceof InvalidMessageException ? null : e);
                                    try {
                                        outbound.getRemote().sendString(new Message(null, error, Message.Type.error).toString());
                                    } catch (IOException | InvalidMessageException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            };
                            LHCBBroker.requestHandler.handleRequest(msg, cb);
                            break;
                    }
                }
            } catch (InvalidMessageException e) {
                e.printStackTrace();
            }

        }
    }
}
