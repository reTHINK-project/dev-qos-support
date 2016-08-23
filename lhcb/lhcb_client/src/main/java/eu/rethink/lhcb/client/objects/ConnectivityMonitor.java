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
import eu.rethink.lhcb.client.util.Utils;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of the Connectivity Monitoring LwM2M Object
 */
public class ConnectivityMonitor extends BaseInstanceEnabler {
    /*
    ID = Name (Instances, Mandatory, Type, Range/Unit)

    00 = Network Bearer (Single, Mandatory, Integer)
            Indicates the network bearer used for the
            current LWM2M communication session
            from the below network bearer list.

            0 ~20 are Cellular Bearers
            00:GSM cellular network

            01:TD - SCDMA cellular network
            02:WCDMA cellular network
            03:CDMA2000 cellular network
            04:WiMAX cellular network
            05:LTE - TDD cellular network
            06:LTE - FDD cellular network

            07~20: Reserved for other type cellular network
            21~40: Wireless Bearers

            21:WLAN network
            22:Bluetooth network
            23:IEEE 802.15 .4 network

            24~40: Reserved for other type local wireless network
            41~50: Wireline Bearers

            41:Ethernet
            42:DSL
            43:PLC

            44 ~50:reserved for others type wireline networks.

    01 = Available Network Bearer (Multiple, Mandatory, Integer)
            Indicates list of current available network bearer.
            Each Resource Instance has a value from the network bearer list.

    02 = Radio Signal Strength (Single, Mandatory, Integer, dBm)
            This node contains the average value of
            the received signal strength indication used
            in the current network bearer in case Network Bearer Resource indicates a
            Cellular Network (RXLEV range 0.. .64)0 is < 110dBm, 64 is > -48 dBm).
            Refer to[ 3 GPP 44.018]for more details on
            Network Measurement Report encoding
            and[3 GPP 45.008]or for Wireless
            Networks refer to the appropriate wireless
            standard.

    03 = Link Quality (Single, Optional, Integer)
            This contains received link quality e.g., LQI
            for IEEE 802.15 .4, (Range(0...255)),
            RxQual Downlink ( for GSM range is 0...7).
            Refer to[ 3 GPP 44.018]for more details on
            Network Measurement Report encoding.

    04 = IP Addresses (Multiple, Mandatory, String)
            The IP addresses assigned to the
            connectivity interface.(e.g.IPv4, IPv6, etc.)

    05 = Router IP Address (Multiple, Optional, String)
            The IP address of the next-hop IP router.
            Note: This IP Address doesn’t indicate the
            Server IP address.

    06 = Link Utilization (Single, Optional, Integer, 1-100%)
            The average utilization of the link to the
            next-hop IP router in %.

    07 = APN (Multiple, Optional, String)
            Access Point Name in case Network
            Bearer Resource is a Cellular Network.

    08 = Cell ID (Single, Optional, Integer)
            Serving Cell ID in case Network Bearer
            Resource is a Cellular Network.

            As specified in TS [3GPP 23.003] and in
            [3GPP. 24.008]. Range (0...65535) in
            GSM/EDGE

            UTRAN Cell ID has a length of 28 bits.
            Cell Identity in WCDMA/TD-SCDMA.
            Range: (0..268435455).

            LTE Cell ID has a length of 28 bits.

            Parameter definitions in [3GPP 25.331].

    09 = SMNC (Single, Optional, Integer, 0-999)
            Serving Mobile Network Code. In case
            Network Bearer Resource has 0(cellular
            network). Range (0...999).
            As specified in TS [3GPP 23.003].

    10 = SMCC (Single, Optional, Integer, 0-999)
            Serving Mobile Country Code. In case
            Network Bearer Resource has 0 (cellular
            network). Range (0...999).
            As specified in TS [3GPP 23.003].
    */
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitor.class);

    public int currentBearer = -1;
    public Bearers currentBearers = null;
    public Map<Integer, Long> currentAvailableBearers = new HashMap<>();
    public int signalStrength = 64;
    public int linkQuality = -1;

    /*
    Runners
     */
    public final Runnable iwconfigRunner = new Runnable() {
        @Override
        public void run() {
            //LOG.info("iwconfigRunner running");
            ArrayList<Integer> changedResources = new ArrayList<>();
            if (currentBearers != null) {
                Tuple<String, Integer> cb = currentBearers.getCurrentBearer();
                if (cb != null) {
                    Tuple<Integer, Integer> lqss = Utils.getLinkQualityAndSignalStrength(cb.x);

                    if (linkQuality != lqss.x) {
                        linkQuality = lqss.x;
                        changedResources.add(3);
                    }

                    if (signalStrength != lqss.y) {
                        signalStrength = lqss.y;
                        changedResources.add(2);
                    }

                    if (changedResources.size() > 0)
                        fireResourcesChange(changedResources);
                }
            }
            //LOG.info("iwconfigRunner done");
        }
    };

    public Map<Integer, String> currentIPs = new HashMap<>();
    public final Runnable ipRunner = new Runnable() {
        @Override
        public void run() {
            //LOG.info("ipRunner running");

            ArrayList<Integer> changedResources = new ArrayList<>();
            Bearers bearers = Utils.getBearers();
            currentBearers = bearers;
            if (!bearers.ips.equals(currentIPs)) {
                currentIPs = bearers.ips;
                LOG.trace("current IPs have changed, set to {}", currentIPs);
                changedResources.add(4);
            }

            // check if current bearer is 1st element in bearers
            if (bearers.bearers.size() == 0) {
                if (currentBearer != -1) {
                    currentBearer = -1;
                    LOG.trace("no current bearer, set to -1");
                    changedResources.add(0);
                }
            } else if (!bearers.bearers.get(0).y.equals(currentBearer)) {
                currentBearer = bearers.bearers.get(0).y;
                LOG.trace("current bearer has changed, set to {}", currentBearer);
                changedResources.add(0);
            }

            // make bearers to currentAvailableBearers
            Map<Integer, Long> map = new HashMap<>();
            int j = 0;
            for (Tuple<String, Integer> bearer : bearers.bearers) {
                map.put(j++, (long) bearer.y);
            }

            if (!map.equals(currentAvailableBearers)) {
                currentAvailableBearers = map;
                LOG.trace("available bearers have changed, set to {}", currentAvailableBearers);
                changedResources.add(1);
            }

            if (changedResources.size() > 0)
                fireResourcesChange(changedResources);
            //LOG.info("ipRunner done");
        }
    };

    public Map<Integer, String> routerIps = new HashMap<>();
    public final Runnable gatewayRunner = new Runnable() {
        @Override
        public void run() {
            //LOG.info("gatewayRunner running");

            Map<Integer, String> newGatewayIPs = Utils.getGatewayIPs();
            if (!newGatewayIPs.equals(routerIps)) {
                routerIps = newGatewayIPs;
                try {
                    fireResourcesChange(5);
                } catch (Exception e) {
                    //e.printStackTrace();
                    LOG.warn("Unable to fire ResourceChange for Gateway IPs (#5)", e);
                }
            }
            //LOG.info("gatewayRunner done");
        }
    };

    // cellular stuff
    public int linkUtilization = -1;
    public Map<Integer, String> apn = new HashMap<>();
    public int cellId = -1;
    public int smnc = -1;
    public int smcc = -1;
    public int sleepTime = 2000;

    // runner related
    private List<Runnable> runnables = new CopyOnWriteArrayList<>();
    private Thread runnerThread = null;

    /**
     * Adds one or more Runnables to the list of Runnables that the Runner Thread is supposed to run.
     *
     * @param runnables - One or more Runnables to add to the list
     */
    public void addToRunner(Runnable... runnables) {
        for (Runnable runnable : runnables) {
            if (this.runnables.contains(runnable)) {
                LOG.trace("Runnable already in runnables list {}", runnable);
            } else {
                this.runnables.add(runnable);
            }
        }
    }

    /**
     * Removes one or more Runnables from the list of Runnables that the Runner Thread is supposed to run.
     *
     * @param runnables
     */
    public void removeFromRunner(Runnable... runnables) {
        for (Runnable runnable : runnables) {
            this.runnables.remove(runnable);
        }
    }

    /**
     * Start the Runner Thread.
     */
    public void startRunner() {
        if (runnerThread != null) {
            LOG.info("Runner Thread is already running");
            return;
            //stopRunner();
        }

        runnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime, endTime, diff;
                int skips;
                while (!Thread.interrupted()) {
                    startTime = System.currentTimeMillis();
                    for (int i = 0; i < runnables.size(); i++) {
                        try {
                            runnables.get(i).run();
                        } catch (Exception e) {
                            //e.printStackTrace();
                            LOG.warn("Runnable #" + i + " crashed:", e);
                        }
                    }
                    endTime = System.currentTimeMillis();
                    diff = endTime - startTime;
                    LOG.trace("RunnerThread: Needed {}ms for {} runnables", diff, runnables.size());
                    skips = 0;
                    while (sleepTime - diff < 0) {
                        skips++;
                        diff -= sleepTime;
                    }

                    if (skips > 0) {
                        LOG.trace("RunnerThread: Skipping sleep {} time(s)", skips);
                    }
                    try {
                        Thread.sleep(sleepTime - diff);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        runnerThread.start();
    }

    /**
     * Stop the Runner Thread.
     */
    public void stopRunner() {
        if (runnerThread != null) {
            runnerThread.interrupt();
            runnerThread = null;
        }
    }

    @Override
    public ReadResponse read(int resourceid) {
        switch (resourceid) {
            case 0: // current network bearer
                return ReadResponse.success(resourceid, currentBearer); // Ethernet
            case 1: // network bearers
                return ReadResponse.success(resourceid, currentAvailableBearers, ResourceModel.Type.INTEGER);
            case 2: // signal strength
                if (signalStrength != -1) {
                    return ReadResponse.success(resourceid, signalStrength);
                } else {
                    return super.read(resourceid);
                }
            case 3: // link quality
                if (linkQuality != -1) {
                    return ReadResponse.success(resourceid, linkQuality);
                } else {
                    return super.read(resourceid);
                }
            case 4: // ip addresses
                if (currentIPs.size() > 0) {
                    return ReadResponse.success(resourceid, currentIPs, ResourceModel.Type.STRING);
                } else {
                    return super.read(resourceid);
                }
            case 5: // router ip
                if (routerIps.size() > 0) {
                    return ReadResponse.success(resourceid, routerIps, ResourceModel.Type.STRING);
                } else {
                    return super.read(resourceid);
                }
            case 6: // link utilization
                if (linkUtilization != -1) {
                    return ReadResponse.success(resourceid, linkUtilization);
                } else {
                    return super.read(resourceid);
                }
            case 7: // APN
                if (apn.size() > 0) {
                    return ReadResponse.success(resourceid, apn, ResourceModel.Type.STRING);
                } else {
                    return super.read(resourceid);
                }
            case 8: // Cell ID
                if (cellId != -1) {
                    return ReadResponse.success(resourceid, cellId);
                } else {
                    return super.read(resourceid);
                }
            case 9: // SMNC
                if (smnc != -1) {
                    return ReadResponse.success(resourceid, smnc);
                } else {
                    return super.read(resourceid);
                }
            case 10: // SMCC
                if (smcc != -1) {
                    return ReadResponse.success(resourceid, smcc);
                } else {
                    return super.read(resourceid);
                }
            default:
                return super.read(resourceid);
        }
    }

    /**
     * Allows to call fireResourcesChange to accept a List Object.
     *
     * @param changedResources - List of resource IDs that changed
     */
    public void fireResourcesChange(List<Integer> changedResources) {
        if (changedResources.size() > 0) {
            int[] intArray = new int[changedResources.size()];
            for (int i = 0; i < changedResources.size(); i++) {
                intArray[i] = changedResources.get(i);
            }
            try {
                LOG.trace("firing Resource Change for {}", Arrays.toString(intArray));
                fireResourcesChange(intArray);
            } catch (Exception e) {
                //e.printStackTrace();
                LOG.warn("Unable to fire ResourceChange for " + Arrays.toString(intArray), e);
            }
        }
    }

    /**
     * Returns a Map of the variables this class provides.
     *
     * @return - A map of variables
     */
    public Map<String, Object> getVarMap() {
        Map<String, Object> varMap = new LinkedHashMap<>();
        varMap.put("currentBearer", currentBearer);
        varMap.put("currentAvailableBearers", currentAvailableBearers);
        varMap.put("signalStrength", signalStrength);
        varMap.put("linkQuality", linkQuality);
        varMap.put("currentIPs", currentIPs);
        varMap.put("routerIps", routerIps);
        varMap.put("linkUtilization", linkUtilization);
        varMap.put("apn", apn);
        varMap.put("cellId", cellId);
        varMap.put("smnc", smnc);
        varMap.put("smcc", smcc);

        return varMap;
    }

    /**
     * Convert this instance to a JSON String.
     *
     * @return JSON String representation of this instance
     */
    public String toJson() {
        return Utils.gson.toJson(getVarMap());
    }
}
