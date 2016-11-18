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

import eu.rethink.lhcb.client.LHCBClient;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 * Created by Robert Ende on 15.11.16.
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
        this.outbound.getRemote().sendString("You are now connected to " + this.getClass().getName(), null);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOG.warn("WebSocket Error", cause);

    }

    @Override
    public void onWebSocketText(String message) {

        if ((outbound != null) && (outbound.isOpen())) {
            LOG.info("got a message: {}", message);
            if (LHCBClient.connectivityMonitorInstance != null) {
                outbound.getRemote().sendString(LHCBClient.connectivityMonitorInstance.toJson(), null);

            } else {
                outbound.getRemote().sendString("{'error':'no connectivityMonitorInstance'}", null);

            }
            //outbound.getRemote().sendString(message, null);
        }
    }
}
