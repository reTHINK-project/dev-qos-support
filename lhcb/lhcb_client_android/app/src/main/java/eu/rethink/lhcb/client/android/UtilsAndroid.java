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

package eu.rethink.lhcb.client.android;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Robert Ende on 06.02.17.
 */
public class UtilsAndroid {
    private static final Logger LOG = LoggerFactory.getLogger(UtilsAndroid.class);

    public static String changeIface(WifiManager wifiManager, String name, String password) {
        //return super.changeIface(name, password);
        LOG.debug("Trying to connect to {} with password {}", name, password);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", name);
        wifiConfig.preSharedKey = String.format("\"%s\"", password);

        //WifiManager wifiManager = (WifiManager) this.context.getSystemService(WIFI_SERVICE);
        //remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        boolean bDisconnect = wifiManager.disconnect();
        boolean bEnableNetwork = wifiManager.enableNetwork(netId, true);
        boolean bReconnect = wifiManager.reconnect();

        LOG.debug("connection attempt done: " + bDisconnect + bEnableNetwork + bReconnect);

        return "Tried it";
    }
}
