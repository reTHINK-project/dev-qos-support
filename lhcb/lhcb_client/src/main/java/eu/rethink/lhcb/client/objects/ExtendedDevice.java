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

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.Value;
import org.eclipse.leshan.core.response.ValueResponse;

/**
 * Created by Robert Ende on 02.03.16.
 */
public class ExtendedDevice extends BaseInstanceEnabler {
    private String deviceID = "0000";


    @Override
    public ValueResponse read(int resourceid) {
        switch (resourceid) {
            case 0:
                return createResponse(resourceid, deviceID);
            default:
                return super.read(resourceid);
        }
    }

    /**
     * Returns ValueResponse containing a single String.
     *
     * @param resourceid - resource ID that is being read
     * @param value      - the String value to be put into the ValueResponse
     * @return ValueResponse containing the specified String
     */
    private ValueResponse createResponse(int resourceid, String value) {
        return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                Value.newStringValue(value)));
    }
}
