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

import eu.rethink.lhcb.utils.message.Message;
import eu.rethink.lhcb.utils.message.exception.InvalidMessageException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Robert Ende on 02.02.17.
 */
public class SendReplyWebsocketAdapter extends WebSocketAdapter {
    private static final Logger LOG = Log.getLogger(SendReplyWebsocketAdapter.class);

    Map<Integer, CompletableFuture<Message>> futureMap = new HashMap<>(1);

    @Override
    public void onWebSocketText(String message) {
        LOG.debug("Received response: {}", message);
        try {
            Message msg = Message.fromString(message);
            CompletableFuture<Message> promise = futureMap.remove(msg.mid);
            if (promise != null)
                promise.complete(msg);
            else {
                LOG.debug("No promise matching incoming message: {}", msg);
            }
        } catch (InvalidMessageException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Message> send(Message msg) {
        LOG.debug("Sending out and waiting for reply for: {}", msg);
        CompletableFuture<Message> promise = new CompletableFuture<>();
        if (isNotConnected())
            promise.completeExceptionally(new IllegalStateException("No connection"));
        else {
            futureMap.put(msg.mid, promise);
            try {
                getRemote().sendString(msg.toString());
            } catch (IOException e) {
                //e.printStackTrace();
                promise.completeExceptionally(e);
            }
        }
        return promise;
    }
}
