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
import org.eclipse.leshan.core.response.ValueResponse;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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

    private static Random r = new Random();

    private int linkQuality = 255;

    public ConnectivityMonitor() {
        // keep changing linkQuality to test observing values
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    linkQuality = r.nextInt(256);
                    try {
                        fireResourceChange(3);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private String[] getIPs() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            List<String> ips = new LinkedList<>();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    ips.add(inetAddresses.nextElement().getHostAddress());
                }
            }

            return ips.toArray(new String[ips.size()]);
        } catch (SocketException e) {
            //e.printStackTrace();
        }
        return new String[]{};
    }

    @Override
    public ValueResponse read(int resourceid) {
        switch (resourceid) {
            case 0: // current network bearer
                return createResponse(resourceid, 41); // Ethernet
            case 1: // network bearers
                return createResponse(resourceid, new int[]{41});
            case 2: // signal strength
                return createResponse(resourceid, 110);
            case 3: // link quality
                return createResponse(resourceid, linkQuality);
            case 4: // ip addresses
                return createResponse(resourceid, getIPs());
            case 5: // router ip
                return createResponse(resourceid, new String[]{"192.168.0.1"});
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

    /**
     * Returns ValueResponse containing a single Integer.
     *
     * @param resourceid - resource ID that is being read
     * @param value      - the Integer value to be put into the ValueResponse
     * @return ValueResponse containing the specified Integer
     */
    private ValueResponse createResponse(int resourceid, int value) {
        return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid,
                Value.newIntegerValue(value)));
    }

    /**
     * Returns ValueResponse containing an array of Strings.
     *
     * @param resourceid - resource ID that is being read
     * @param values     - the String values to be put into the ValueResponse
     * @return ValueResponse containing the specified Strings
     */
    private ValueResponse createResponse(int resourceid, int[] values) {
        Value[] vs = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            int v = values[i];
            vs[i] = Value.newIntegerValue(v);
        }

        return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid, vs));
    }

    /**
     * Returns ValueResponse containing an array of Integers.
     *
     * @param resourceid - resource ID that is being read
     * @param values     - the Integer values to be put into the ValueResponse
     * @return ValueResponse containing the specified Integers
     */
    private ValueResponse createResponse(int resourceid, String[] values) {
        Value[] vs = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            vs[i] = Value.newStringValue(v);
        }

        return new ValueResponse(ResponseCode.CONTENT, new LwM2mResource(resourceid, vs));
    }
}
