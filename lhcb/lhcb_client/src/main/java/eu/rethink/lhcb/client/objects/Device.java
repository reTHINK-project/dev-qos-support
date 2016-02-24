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
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

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

    private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());

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
    public ValueResponse read(int resourceid) {
        LOG.debug("Read on Device Resource " + resourceid);
        switch (resourceid) {
            case 0:
                return createResponse(resourceid, manufacturer);
            case 1:
                return createResponse(resourceid, model);
            case 2:
                return createResponse(resourceid, serial);
            case 3:
                return createResponse(resourceid, firmware);
            case 9:
                return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                        Value.newIntegerValue(getBatteryLevel())));
            case 10:
                return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                        Value.newIntegerValue(getMemoryFree())));
            case 11:
                return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                        new Value<?>[]{Value.newIntegerValue(0)}));
            case 13:
                return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                        Value.newDateValue(date)));
            case 14:
                return createResponse(resourceid, getUtcOffset());
            case 15:
                return createResponse(resourceid, getTimezone());
            case 16:
                return createResponse(resourceid, binding);
            default:
                return super.read(resourceid);
        }
    }

    @Override
    public LwM2mResponse execute(int resourceid, byte[] params) {
        LOG.debug("Execute on Device resource " + resourceid);
        if (params != null && params.length != 0)
            LOG.debug("\t params " + new String(params));
        return new LwM2mResponse(ResponseCode.CHANGED);
    }

    @Override
    public LwM2mResponse write(int resourceid, LwM2mResource value) {
        LOG.debug("Write on Device Resource " + resourceid + " value " + value);
        switch (resourceid) {
            case 13:
                return new LwM2mResponse(ResponseCode.NOT_FOUND);
            case 14:
                setUtcOffset((String) value.getValue().value);
                fireResourceChange(resourceid);
                return new LwM2mResponse(ResponseCode.CHANGED);
            case 15:
                setTimezone((String) value.getValue().value);
                fireResourceChange(resourceid);
                return new LwM2mResponse(ResponseCode.CHANGED);
            default:
                return super.write(resourceid, value);
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