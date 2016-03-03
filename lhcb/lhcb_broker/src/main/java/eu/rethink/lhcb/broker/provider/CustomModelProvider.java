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

package eu.rethink.lhcb.broker.provider;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * An implementation of LwM2mModelProvider (very similar to StandardModelProvider)
 * that adds the custom models from model.json to the list of models
 */
public class CustomModelProvider implements LwM2mModelProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CustomModelProvider.class);

    private final LwM2mModel model;

    public CustomModelProvider() {
        // build a single model with default objects
        List<ObjectModel> models = ObjectLoader.loadDefault();

        // add custom models from model.json
        InputStream modelStream = getClass().getResourceAsStream("/model.json");
        models.addAll(ObjectLoader.loadJsonStream(modelStream));
        this.model = new LwM2mModel(models);
    }

    public LwM2mModel getObjectModel(Client client) {
        // same model for all clients
        return this.model;
    }
}