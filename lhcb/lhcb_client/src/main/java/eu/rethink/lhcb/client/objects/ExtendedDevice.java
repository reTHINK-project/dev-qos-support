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

package eu.rethink.lhcb.client.objects;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * A small custom Instance that holds extra information about the LHCB Client
 */
public class ExtendedDevice extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(ExtendedDevice.class);

    private String deviceID = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));

    @Override
    public ReadResponse read(int resourceid) {
        LOG.debug("read on " + resourceid + " -> " + deviceID);
        switch (resourceid) {
            case 0:
                return ReadResponse.success(resourceid, deviceID);
        }
        return super.read(resourceid);
    }
}
