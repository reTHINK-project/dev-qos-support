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
 */

package eu.rethink.lhcb.client.android.objects;

import android.content.Context;
import android.net.wifi.WifiManager;
import com.google.gson.JsonArray;
import eu.rethink.lhcb.client.android.UtilsAndroid;
import eu.rethink.lhcb.client.objects.ExtendedDevice;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.rethink.lhcb.utils.Utils.gson;

/**
 * Created by Robert Ende on 06.02.17.
 */
public class ExtendedDeviceAndroid extends ExtendedDevice {
    private static final Logger LOG = LoggerFactory.getLogger(ExtendedDeviceAndroid.class);

    private Context context;

    public ExtendedDeviceAndroid(Context context) {
        this.context = context;
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {
        LOG.debug("execute on " + resourceid + " with params " + params);
        JsonArray array = gson.fromJson(params, JsonArray.class);
        switch (resourceid) {
            case 0:
                String ssid = array.get(0).getAsString();
                String pw = null;
                if (array.size() > 1)
                    pw = array.get(1).getAsString();

                WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
                UtilsAndroid.changeIface(wifiManager, ssid, pw);
                return ExecuteResponse.success();
        }
        return super.execute(resourceid, params);
    }
}
