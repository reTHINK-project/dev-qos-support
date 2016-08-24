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

import eu.rethink.lhcb.client.util.Bearers;
import eu.rethink.lhcb.client.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation of the Connectivity Monitoring LwM2M Object with Dummy values
 */
public class ConnectivityMonitorDummy extends ConnectivityMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitorDummy.class);
    private static final Random r = new Random();

    private Runnable randomizerRunner = new Runnable() {
        @Override
        public void run() {
            linkQuality = r.nextInt(8);
            signalStrength = r.nextInt(64);
            linkUtilization = r.nextInt(100);
            try {
                fireResourcesChange(2, 3, 6);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    };

    public ConnectivityMonitorDummy() {

        // 00 && 01
        List<Tuple<String, Integer>> bearers = new LinkedList<>();
        bearers.add(new Tuple<>("cel0", 2));
        bearers.add(new Tuple<>("eth0", 41));
        bearers.add(new Tuple<>("eth1", 41));
        bearers.add(new Tuple<>("wlan0", 21));
        bearers.add(new Tuple<>("cel1", 1));
        bearers.add(new Tuple<>("cel2", 5));

        // 04
        Map<Integer, String> currentIPs = new HashMap<>();
        currentIPs.put(0, "192.168.133.37");
        currentIPs.put(1, "192.168.133.38");
        currentIPs.put(2, "192.168.133.39");

        currentBearers = new Bearers(bearers, currentIPs);

        // 02
        signalStrength = r.nextInt(64);

        // 03
        linkQuality = r.nextInt(8);

        // 05
        routerIps.put(0, "192.168.133.1");

        // 06
        linkUtilization = r.nextInt(100);

        // 07
        apn.put(0, "internet");
        apn.put(1, "mywap");
        apn.put(2, "bigbank-intranet");
        apn.put(3, "mycompany.mnc02.mcc283.gprs");

        // 08
        cellId = 24908;

        // 09
        smnc = 243;

        // 10
        smcc = 6;

        addToRunner(randomizerRunner);
    }
}
