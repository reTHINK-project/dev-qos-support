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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Implementation of the Connectivity Monitoring LwM2M Object with Dummy values
 */
public class ConnectivityMonitorDummy extends ConnectivityMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitorDummy.class);
    private static final Random r = new Random();

    private Runnable randomizerRunner = new Runnable() {
        @Override
        public void run() {
            LOG.info("randomizerRunner running");
            linkQuality = r.nextInt(8);
            signalStrength = r.nextInt(64);
            linkUtilization = r.nextInt(100);
            try {
                fireResourcesChange(2, 3, 6);
            } catch (Exception e) {
                //e.printStackTrace();
            }
            LOG.info("randomizerRunner done");
        }
    };

    public ConnectivityMonitorDummy() {

        // 00
        currentBearer = 2;

        // 01
        currentAvailableBearers.put(0, (long) 1);
        currentAvailableBearers.put(1, (long) 2);
        currentAvailableBearers.put(3, (long) 5);
        currentAvailableBearers.put(4, (long) 21);
        currentAvailableBearers.put(5, (long) 41);

        // 02
        signalStrength = r.nextInt(64);

        // 03
        linkQuality = r.nextInt(8);

        // 04
        currentIPs.put(0, "192.168.133.37");
        currentIPs.put(1, "192.168.133.38");
        currentIPs.put(2, "192.168.133.39");

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
