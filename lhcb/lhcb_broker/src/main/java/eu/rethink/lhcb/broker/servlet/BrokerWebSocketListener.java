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

import eu.rethink.lhcb.broker.RequestHandler;
import eu.rethink.lhcb.broker.message.Message;
import eu.rethink.lhcb.broker.message.exception.InvalidMessageException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static eu.rethink.lhcb.broker.servlet.BrokerWebSocketServlet.requestHandler;

/**
 * Created by Robert Ende on 15.11.16.
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
        LOG.debug("getLocalAddress: {}", session.getLocalAddress());
        LOG.debug("getRemoteAddress: {}", session.getRemoteAddress());
        //LOG.debug("getRemote: {}", Utils.gson.toJson(session.getRemote()));
        UpgradeRequest upgradeRequest = session.getUpgradeRequest();
        LOG.debug("upgradeRequest.getExtensions(): {}", upgradeRequest.getExtensions());
        LOG.debug("upgradeRequest.getHeaders(): {}", upgradeRequest.getHeaders());
        LOG.debug("upgradeRequest.getHost(): {}", upgradeRequest.getHost());
        LOG.debug("upgradeRequest.getMethod(): {}", upgradeRequest.getMethod());
        LOG.debug("upgradeRequest.getOrigin(): {}", upgradeRequest.getOrigin());
        LOG.debug("upgradeRequest.getParameterMap(): {}", upgradeRequest.getParameterMap());
        LOG.debug("upgradeRequest.getRequestURI(): {}", upgradeRequest.getRequestURI());
        //LOG.debug("getUpgradeResponse: {}", Utils.gson.toJson(session.getUpgradeResponse()));

        this.outbound.getRemote().sendString("You are now connected to " + this.getClass().getName(), null);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOG.warn("WebSocket Error", cause);

    }

    @Override
    public void onWebSocketText(String message) {

        if ((outbound != null) && (outbound.isOpen()) && requestHandler != null) {
            LOG.info("Echoing back text message [{}]", message);
            RequestHandler.RequestCallback cb = new RequestHandler.RequestCallback() {

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

            try {
                requestHandler.handleRequest(Message.fromString(message), cb);
            } catch (InvalidMessageException e) {
                cb.error(e);
            }

        }
    }
}
