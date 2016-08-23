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
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementation of the Device LwM2M Object
 */
public class Device extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(Device.class);

    private static String manufacturer = "reTHINK Last Hop Connectivity Broker - Client module";
    private static String model = "Model 1337";
    private static String serial = "RT-500-000-0001";
    private static String firmware = "0.1.0";
    private static String binding = "U";
    private static Date date = new Date();

    private int getBatteryLevel() {
        final Random rand = new Random();
        return rand.nextInt(100);
    }

    private int getMemoryFree() {
        final Random rand = new Random();
        return rand.nextInt(50) + 114;
    }

    private String utcOffset = new SimpleDateFormat().format(Calendar.getInstance().getTime());

    private String getUtcOffset() {
        return utcOffset;
    }

    private void setUtcOffset(String t) {
        utcOffset = t;
    }

    private String timeZone = TimeZone.getDefault().getID();

    private String getTimezone() {
        return timeZone;
    }

    private void setTimezone(String t) {
        timeZone = t;
    }

    @Override
    public ReadResponse read(int resourceid) {
        LOG.debug("Read on Device Resource " + resourceid);
        switch (resourceid) {
            case 0:
                return ReadResponse.success(resourceid, manufacturer);
            case 1:
                return ReadResponse.success(resourceid, model);
            case 2:
                return ReadResponse.success(resourceid, serial);
            case 3:
                return ReadResponse.success(resourceid, firmware);
            case 9:
                return ReadResponse.success(resourceid, getBatteryLevel());
            case 10:
                return ReadResponse.success(resourceid, getMemoryFree());
            case 11:
                Map<Integer, Long> errorCodes = new HashMap<>();
                errorCodes.put(0, (long) 0);
                return ReadResponse.success(resourceid, errorCodes, ResourceModel.Type.INTEGER);
            case 13:
                return ReadResponse.success(resourceid, date);
            case 14:
                return ReadResponse.success(resourceid, getUtcOffset());
            case 15:
                return ReadResponse.success(resourceid, getTimezone());
            case 16:
                return ReadResponse.success(resourceid, binding);
            default:
                return super.read(resourceid);
        }
    }
}