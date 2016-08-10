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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

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


    private static final Random r = new Random();
    private static final String routeCmd = "route -n";

    private int linkQuality = r.nextInt(256);
    private int signalStrength = r.nextInt(256);
    private Map<Integer, String> ips = new HashMap<>();
    private Map<Integer, String> routerIps = new HashMap<>();

    private Integer currentBearer = 0;
    private Map<Integer, Long> availableBearers = new HashMap<>();
    private int sleepTime = 2000;

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Integer> IFaceNameToId = new HashMap<>();

    static {
        IFaceNameToId.put("eth", 41);
        IFaceNameToId.put("ens", 41);
        IFaceNameToId.put("wlan", 21);
        IFaceNameToId.put("wls", 21);
    }

    private static String currentBearerName = null;
    public ConnectivityMonitor() {
        startUpdating();
    }

    @Override
    public ReadResponse read(int resourceid) {
        switch (resourceid) {
            case 0: // current network bearer
                return ReadResponse.success(resourceid, currentBearer); // Ethernet
            case 1: // network bearers
                //Map<Integer, Long> map = new HashMap<>();
                //map.put(0, (long) 41);
                return ReadResponse.success(resourceid, availableBearers, ResourceModel.Type.INTEGER);
            case 2: // signal strength
                return ReadResponse.success(resourceid, signalStrength);
            case 3: // link quality
                return ReadResponse.success(resourceid, linkQuality);
            case 4: // ip addresses
                return ReadResponse.success(resourceid, ips, ResourceModel.Type.STRING);
            case 5: // router ip
                return ReadResponse.success(resourceid, routerIps, ResourceModel.Type.STRING);
            case 6: // link utilization
                // TODO implementation
            case 7: // APN
                // TODO implementation
            case 8: // Cell ID
                // TODO implementation
            case 9: // SMNC
                // TODO implementation
            case 10: // SMCC
                // TODO implementation
            default:
                return super.read(resourceid);
        }
    }

    private void startUpdating() {
        LOG.debug("Start updating resources");

        iwconfigThread.start();

        // update ip list
        ipThread.start();

        // update gateway IPs
        gatewayThread.start();
    }

    private void stopUpdating() {
        LOG.debug("Stop updating resources");

        iwconfigThread.interrupt();
        ipThread.interrupt();
        gatewayThread.interrupt();
    }


    /**
     * Creates a map of available host addresses for this machine.
     * They key in the map is an index starting from 0
     *
     * @return A map in the form of index:IP
     */
    private Map<Integer, String> getIPs() {
        Map<Integer, String> ips = new HashMap<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            int i = 0;
            List<Integer> bearers = new LinkedList<>();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                if (iface.isUp()) {
                    //LOG.debug("getIPs: checking iface {} ", gson.toJson(iface));

                    // only consider interface that are up
                    // try to get bearer kind
                    for (String ifaceName : IFaceNameToId.keySet()) {
                        if (iface.getDisplayName().startsWith(ifaceName)) {
                            int bearer = IFaceNameToId.get(ifaceName);
                            currentBearerName = iface.getDisplayName();
                            bearers.add(bearer);
                        }
                    }

                    // get IPs
                    Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        ips.put(i++, inetAddresses.nextElement().getHostAddress());
                    }
                }
            }

            //LOG.debug("bearers: {}", gson.toJson(bearers));

            // check if current bearer is 1st element in bearers
            if (bearers.size() == 0) {
                if (currentBearer != null) {
                    currentBearer = null;
                    LOG.debug("no current bearer, set to null");
                    try {
                        fireResourcesChange(0);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
            } else if (!bearers.get(0).equals(currentBearer)) {
                currentBearer = bearers.get(0);

                LOG.debug("current bearer has changed, set to {}", currentBearer);
                try {
                    fireResourcesChange(0);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }

            // make bearers to availableBearers
            Map<Integer, Long> map = new HashMap<>();
            int j = 0;
            for (Integer bearer : bearers) {
                map.put(j++, (long) bearer);
            }

            if (!map.equals(availableBearers)) {
                availableBearers = map;
                LOG.debug("available bearers have changed, set to {}", availableBearers);
                try {
                    fireResourcesChange(1);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            //e.printStackTrace();
        }
        return ips;
    }

    /**
     * Creates a map of available router addresses for this machine.
     * They key in the map is an index starting from 0
     *
     * @return A map in the form of index:IP
     */
    private Map<Integer, String> getGatewayIPs() {
        Map<Integer, String> ips = new HashMap<>();
        try {
            Process p = Runtime.getRuntime().exec(routeCmd);
            Scanner sc = new Scanner(p.getInputStream(), "IBM850");

            // "Kernel IP routing table"
            sc.nextLine();

            // Destination  Gateway     Genmask     Flags       MSS         Window      irtt        Iface
            sc.nextLine();

            // 0.0.0.0      10.147.65.1 0.0.0.0     UG          0           0           0           eth0
            // ...
            int i = 0;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                do {
                    line = line.replace("  ", " ");
                } while (line.contains("  "));
                String[] splitLine = line.split(" ");
                if (splitLine[3].equals("UG")) {
                    ips.put(i++, splitLine[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ips;
    }

    private Thread linkQualityThreadOld = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                linkQuality = r.nextInt(256);
                try {
                    fireResourcesChange(3);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    });

    private Thread ipThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                Map<Integer, String> newIps = getIPs();
                if (!newIps.equals(ips)) {
                    ips = newIps;
                    try {
                        fireResourcesChange(4);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    });

    private Thread gatewayThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                Map<Integer, String> newGatewayIPs = getGatewayIPs();
                if (!newGatewayIPs.equals(routerIps)) {
                    routerIps = newGatewayIPs;
                    try {
                        fireResourcesChange(5);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    });

    private Thread iwconfigThread = new Thread(new Runnable() {
        @Override
        public void run() {
            String line;
            int i, j, k;
            String linkQualityLabel = "Link Quality=";
            String signalLevelLabel = "Signal level=";
            while (!Thread.interrupted()) {
                if (currentBearerName != null) {
                    try {
                        Process p = Runtime.getRuntime().exec("iwconfig " + currentBearerName);
                        BufferedReader result = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        while ((line = result.readLine()) != null) {
                            i = line.indexOf(linkQualityLabel);
                            j = line.indexOf(signalLevelLabel); // entry after link quality

                            if (i != -1) {
                                // we are in the correct line

                                // get link quality
                                String[] quality = line.substring(i + linkQualityLabel.length(), j - 1).trim().split("/");
                                linkQuality = Math.round((Float.parseFloat(quality[0]) / Float.parseFloat(quality[1])) * 100);

                                // get signal strength
                                k = line.indexOf("dBm");
                                signalStrength = Integer.parseInt(line.substring(j + signalLevelLabel.length(), k-1).trim());
                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    });
}
