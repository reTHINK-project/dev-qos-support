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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Implementation of the Connectivity Monitoring LwM2M Object with Dummy values
 */
public class ConnectivityMonitorDummy extends BaseInstanceEnabler {
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
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitorDummy.class);


    private static final Random r = new Random();
    private static final String routeCmd = "route -n";

    private int linkQuality = r.nextInt(8);
    private int signalStrength = r.nextInt(64);
    private int linkUtilization = r.nextInt(100);
    private static Map<Integer, String> ips = new HashMap<>();
    private static Map<Integer, String> routerIps = new HashMap<>();
    private static Map<Integer, Long> bearers = new HashMap<>();
    private static Map<Integer, String> apns = new HashMap<>();

    private int sleepTime = 2000;

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Integer> IFaceNameToId = new HashMap<>();

    static {
        IFaceNameToId.put("eth", 41);
        IFaceNameToId.put("ens", 41);
        IFaceNameToId.put("wlan", 21);
        IFaceNameToId.put("wls", 21);

        ips.put(0, "192.168.133.37");
        ips.put(1, "192.168.133.38");
        ips.put(2, "192.168.133.39");

        routerIps.put(0, "192.168.133.1");

        apns.put(0, "internet");
        apns.put(1, "mywap");
        apns.put(2, "bigbank-intranet");
        apns.put(3, "mycompany.mnc02.mcc283.gprs");

        bearers.put(0, (long) 1);
        bearers.put(1, (long) 2);
        bearers.put(3, (long) 5);
        bearers.put(4, (long) 21);
        bearers.put(5, (long) 41);
    }

    private static String currentBearerName = null;
    public ConnectivityMonitorDummy() {
        startUpdating();
    }

    @Override
    public ReadResponse read(int resourceid) {
        switch (resourceid) {
            case 0: // current network bearer
                return ReadResponse.success(resourceid, 2);
            case 1: // network bearers
                return ReadResponse.success(resourceid, bearers, ResourceModel.Type.INTEGER);
            case 2: // signal strength
                return ReadResponse.success(resourceid, signalStrength);
            case 3: // link quality
                return ReadResponse.success(resourceid, linkQuality);
            case 4: // ip addresses
                return ReadResponse.success(resourceid, ips, ResourceModel.Type.STRING);
            case 5: // router ip
                return ReadResponse.success(resourceid, routerIps, ResourceModel.Type.STRING);
            case 6: // link utilization
                return ReadResponse.success(resourceid, linkUtilization);
            case 7: // APN
                return ReadResponse.success(resourceid, apns, ResourceModel.Type.STRING);
            case 8: // Cell ID
                return ReadResponse.success(resourceid, 24908);
            case 9: // SMNC
                return ReadResponse.success(resourceid, 243);
            case 10: // SMCC
                return ReadResponse.success(resourceid, 6);
            default:
                return super.read(resourceid);
        }
    }

    private void startUpdating() {
        LOG.debug("Start updating resources");
        randomizerThread.start();
    }

    private void stopUpdating() {
        LOG.debug("Stop updating resources");
        randomizerThread.interrupt();
    }

    private Thread randomizerThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                linkQuality = r.nextInt(8);
                signalStrength = r.nextInt(64);
                linkUtilization = r.nextInt(100);
                try {
                    fireResourcesChange(2, 3, 6);
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
}
