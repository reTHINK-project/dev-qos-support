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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.rethink.lhcb.utils.message.exception.InvalidMessageException;

/**
 * Created by Robert Ende on 01.02.17.
 */
public class ExecuteMessage extends Message {

    public ExecuteMessage(String client, JsonElement value) throws InvalidMessageException {
        super(client, value, Type.execute);

        try {
            value.getAsJsonObject().get("name").getAsString(); // just checking if it works
            if (value.getAsJsonObject().has("args")) {
                value.getAsJsonObject().get("args").getAsJsonArray(); // just checking if it works
            }
        } catch (Exception e) {
            //e.printStackTrace();
            throw new InvalidMessageException("Unable to parse ExecuteMessage", e);
        }
    }

    public ExecuteMessage(String client, String name, JsonArray args) throws InvalidMessageException {
        super(client, Type.execute);

        if (name == null)
            throw new InvalidMessageException("No function name specified.");

        this.value = createValue(name, args);
    }

    public String getName() {
        return value.getAsJsonObject().get("name").getAsString();
    }

    public JsonArray getArgs() {
        return value.getAsJsonObject().has("args") ? value.getAsJsonObject().get("args").getAsJsonArray() : null;
    }

    private JsonObject createValue(String name, JsonArray args) {
        JsonObject value = new JsonObject();
        value.addProperty("name", name);
        if (args != null)
            value.add("args", args);
        return value;
    }
}
