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
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.*;
import android.text.TextUtils;
import eu.rethink.lhcb.client.objects.ConnectivityMonitor;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.WIFI_SERVICE;

;

/**
 * Extension of ConnectivityMonitor with Android specific Runnables.
 */
public class ConnectivityMonitorAndroid extends ConnectivityMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitorAndroid.class);

    private static final String[] APN_PROJECTION = {
            Telephony.Carriers.TYPE,            // 0
            Telephony.Carriers.MMSC,            // 1
            Telephony.Carriers.MMSPROXY,        // 2
            Telephony.Carriers.MMSPORT          // 3
    };

    // Context of App that runs the LHCB Client (needed for wifi Runner)
    private Context context = null;

    /**
     * Retrieves signal strength & link quality from Wifi Manager.
     */
    private Runnable wifiRunner = new Runnable() {
        @Override
        public void run() {
            Set<Integer> changedResources = new LinkedHashSet<>(2);
            WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);

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

            //LOG.debug("Debugging scanResults: {}", wifiManager.getScanResults());
            int i = 1;
            for (ScanResult scanResult : wifiManager.getScanResults()) {
                //try {
                //    LOG.debug("in gson: {}", Utils.gson.toJson(scanResult));
                //} catch (Exception e) {
                //    e.printStackTrace();
                //}
                //currentBearers.bearers.add(new Tuple<>(scanResult.SSID, -1));
                LOG.trace("SSID: {}, BSSID: {}", scanResult.SSID, scanResult.BSSID);
                apn.put(i++, scanResult.SSID);
            }

            while (apn.size() > i) {
                apn.remove(apn.size() - 1);
            }

            if (changedResources.size() > 0)
                fireResourcesChange(changedResources);
        }
    };

    private Runnable connectivityRunner = new Runnable() {
        @Override
        public void run() {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    LOG.trace("NetworkInfo for network {}: {} (" + networkInfo.getClass() + ")", network, networkInfo);

                }
            } else {
                for (NetworkInfo networkInfo : connectivityManager.getAllNetworkInfo()) {
                    LOG.trace("NetworkInfo: {}", networkInfo);
                }
            }
        }
    };

    private Runnable cellRunner = new Runnable() {
        @Override
        public void run() {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            apn.put(0, telephonyManager.getNetworkOperatorName());

            List<CellInfo> allCellInfo = telephonyManager.getAllCellInfo();
            LOG.trace("got cellInfos: {}, size: {}", allCellInfo, allCellInfo.size());
            if (allCellInfo.size() > 0) {
                CellInfo cellInfo = allCellInfo.get(0);
                LOG.debug("cellInfo class: {}", cellInfo.getClass());
                int dbm = 0;
                if (cellInfo instanceof CellInfoCdma) {
                    dbm = ((CellInfoCdma) cellInfo).getCellSignalStrength().getDbm();
                } else if (cellInfo instanceof CellInfoGsm) {
                    dbm = ((CellInfoGsm) cellInfo).getCellSignalStrength().getDbm();
                } else if (cellInfo instanceof CellInfoLte) {
                    dbm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                } else if (cellInfo instanceof CellInfoWcdma) {
                    dbm = ((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm();
                }
                signalStrength = convertDbm(dbm);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                LOG.trace("getCellLocation: {}", telephonyManager.getCellLocation());
                //LOG.trace("getDeviceId: {}", telephonyManager.getDeviceId());
                LOG.trace("getNetworkCountryIso: {}", telephonyManager.getNetworkCountryIso());
                LOG.trace("getDeviceSoftwareVersion: {}", telephonyManager.getDeviceSoftwareVersion());
                LOG.trace("getLine1Number: {}", telephonyManager.getLine1Number());
                LOG.trace("getNetworkCountryIso: {}", telephonyManager.getNetworkCountryIso());
                LOG.trace("getNetworkOperator: {}", telephonyManager.getNetworkOperator());
                LOG.trace("getNetworkOperatorName: {}", telephonyManager.getNetworkOperatorName());
                String networkOperator = telephonyManager.getNetworkOperator();
                if (!TextUtils.isEmpty(networkOperator)) {
                    smcc = Integer.parseInt(networkOperator.substring(0, 3));
                    smnc = Integer.parseInt(networkOperator.substring(3));
                }
            }
        }
    };

    private Runnable apnRunner = new Runnable() {
        @Override
        public void run() {
            // from http://stackoverflow.com/questions/7257567/create-network-access-point-name-with-code

            //path to preffered APNs
            final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

            //receiving cursor to preffered APN table
            Cursor c = context.getContentResolver().query(PREFERRED_APN_URI, null, null, null, null);

            LOG.debug("columns: {}", c.getColumnNames());
            //moving the cursor to beggining of the table
            c.moveToFirst();

            //now the cursor points to the first preffered APN and we can get some
            //information about it
            //for example first preffered APN id
            int index = c.getColumnIndex("_id");    //getting index of required column
            Short id = c.getShort(index);           //getting APN's id from

            //we can get APN name by the same way
            index = c.getColumnIndex("name");
            String name = c.getString(index);

            LOG.debug("APN Runner got id '{}' and name '{}'", id, name);

            apn.put(0, name);
            c.close();
        }
    };

    public ConnectivityMonitorAndroid(Context context) {
        this.context = context;

        addToRunner(ipRunner, gatewayRunner, cellRunner, wifiRunner);
        //addToRunner(ipRunner, gatewayRunner, wifiRunner);
        //addToRunner(connectivityRunner, cellRunner);
    }

    @Override
    public ExecuteResponse execute(int resourceid, String params) {
        LOG.info("execute called: [" + resourceid + ", " + params + "]");
        return super.execute(resourceid, params);
    }
}
