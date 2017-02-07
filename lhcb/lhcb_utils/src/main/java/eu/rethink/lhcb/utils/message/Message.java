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

package eu.rethink.lhcb.utils.message;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import eu.rethink.lhcb.utils.message.exception.InvalidMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static eu.rethink.lhcb.utils.Utils.gson;

/**
 * Created by Robert Ende on 03.11.16.
 */
public class Message {
    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    private static int nextId = 0;
    public Type type;
    public int mid = -1;
    public String client;
    public JsonElement value;

    private static synchronized void incNextId() {
        // make sure nextId is never negative
        nextId = ++nextId % Integer.MAX_VALUE;
    }

    public Message(String client, Type type) throws InvalidMessageException {
        this.mid = nextId;
        this.client = client;
        this.type = type;

        validate();
        incNextId();
    }

    public Message(String client, JsonElement value, Type type) throws InvalidMessageException {
        this.mid = nextId;
        this.client = client;
        this.value = value;
        this.type = type;

        validate();
        incNextId();
    }

    public Message(String client, String value, Type type) throws InvalidMessageException {
        this(client, new JsonPrimitive(value), type);
    }

    public Message(String client, Exception e, Type type) throws InvalidMessageException {
        this(client, e.getLocalizedMessage(), type);
    }

    public Message(int mid, String client, JsonElement value, Type type) throws InvalidMessageException {
        this.mid = mid;
        this.client = client;
        this.value = value;
        this.type = type;

        validate();
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public JsonElement getValue() {
        return value;
    }

    public Message response(JsonElement value) throws InvalidMessageException {
        return new Message(mid, client, value, Type.response);
    }

    public Message response(Object value) throws InvalidMessageException {
        return new Message(mid, client, gson.toJsonTree(value), Type.response);
    }

    public Message errorResponse(Exception value) throws InvalidMessageException {
        return new Message(mid, client, new JsonPrimitive(value.getLocalizedMessage()), Type.error);
    }

    public Message errorResponse(String value) throws InvalidMessageException {
        return new Message(mid, client, new JsonPrimitive(value), Type.error);
    }

    public enum Type {
        read, write, execute, observe, notify, response, error
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

    private void validate() throws InvalidMessageException {
        if (mid < 0)
            throw new InvalidMessageException("Message ID must not be negative");
    }

    public static Message fromString(String json) throws InvalidMessageException {
        JsonObject genericJson = gson.fromJson(json, JsonObject.class);
        // checking if mid is present
        // if not, set it to -1 before converting to Message
        if (!genericJson.has("mid"))
            genericJson.addProperty("mid", -1);

        try {
            Message msg = gson.fromJson(genericJson, Message.class);
            if (msg.type.equals(Type.execute))
                msg = gson.fromJson(genericJson, ExecuteMessage.class);
            else if (msg.type.equals(Type.read))
                msg = gson.fromJson(genericJson, ReadMessage.class);

            if (msg.mid == -1) {
                msg.mid = nextId;
                incNextId();
            }
            msg.validate();
            return msg;
        } catch (JsonSyntaxException e) {
            //e.printStackTrace();
            throw new InvalidMessageException("Message not in JSON format");
        }
    }

    public static Message fromParams(Map<String, String[]> params) throws InvalidMessageException {
        LOG.debug("fromParams: {}", params);
        LinkedHashMap<String, String> converted = new LinkedHashMap<>(params.size());
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            converted.put(entry.getKey(), entry.getValue()[0]);
        }
        JsonElement json = gson.toJsonTree(converted);
        Message msg = gson.fromJson(json, Message.class);
        if (msg.type.equals(Type.execute))
            msg = gson.fromJson(json, ExecuteMessage.class);
        else if (msg.type.equals(Type.read))
            msg = gson.fromJson(json, ReadMessage.class);

        if (msg.mid == -1) {
            msg.mid = nextId;
            incNextId();
        }
        msg.validate();
        return msg;
    }
}
