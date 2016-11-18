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

package eu.rethink.lhcb.broker.message;

import com.google.gson.JsonSyntaxException;
import eu.rethink.lhcb.broker.message.exception.InvalidMessageException;
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
    private int mid = -1;
    private String client;
    private Object value;

    private static synchronized void incNextId() {
        // make sure nextId is never negative
        nextId = ++nextId % Integer.MAX_VALUE;
    }

    public Message(String client, Object value, Type type) throws InvalidMessageException {
        this.mid = nextId;
        this.client = client;
        this.value = value;
        this.type = type;

        validate();
        incNextId();
    }

    private Message(int mid, String client, Object value, Type type) throws InvalidMessageException {
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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Message response(Object value) throws InvalidMessageException {
        return new Message(mid, client, value, Type.response);
    }

    public Message errorResponse(Exception value) throws InvalidMessageException {
        return new Message(mid, client, value.getLocalizedMessage(), Type.error);
    }

    public Message errorResponse(String value) throws InvalidMessageException {
        return new Message(mid, client, value, Type.error);
    }

    public enum Type {
        read, write, observe, notify, response, error
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
        try {
            Message msg = gson.fromJson(json, Message.class);
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
        Message msg = gson.fromJson(gson.toJsonTree(converted), Message.class);
        if (msg.mid == -1) {
            msg.mid = nextId;
            incNextId();
        }
        msg.validate();
        return msg;
    }
}
