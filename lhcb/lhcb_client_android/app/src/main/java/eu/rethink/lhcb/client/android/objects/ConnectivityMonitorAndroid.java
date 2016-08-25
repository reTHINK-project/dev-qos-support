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
import eu.rethink.lhcb.client.objects.ConnectivityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extension of ConnectivityMonitor with Android specific Runnables.
 */
public class ConnectivityMonitorAndroid extends ConnectivityMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitorAndroid.class);

    // Context of App that runs the LHCB Client (needed for wifi Runner)
    private Context context = null;

    /**
     * Retrieves signal strength & link quality from Wifi Manager.
     */
    private Runnable wifiRunner = new Runnable() {
        @Override
        public void run() {
            Set<Integer> changedResources = new LinkedHashSet<>(2);
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            int newSignalStrength = WifiManager.calculateSignalLevel(wifiManager.getConnectionInfo().getRssi(), 64);
            int newLinkQuality = wifiManager.getConnectionInfo().getLinkSpeed();

            if (signalStrength != newSignalStrength) {
                LOG.trace("signalStrength has changed: {} -> {}", signalStrength, newSignalStrength);
                signalStrength = newSignalStrength;
                changedResources.add(2);
            }

            if (linkQuality != newLinkQuality) {
                LOG.trace("linkQuality has changed: {} -> {}", linkQuality, newLinkQuality);
                linkQuality = newLinkQuality;
                changedResources.add(3);
            }

            if (changedResources.size() > 0)
                fireResourcesChange(changedResources);
        }
    };

    public ConnectivityMonitorAndroid(Context context) {
        this.context = context;
        addToRunner(ipRunner, gatewayRunner, wifiRunner);
    }
}
