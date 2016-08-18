package eu.rethink.lhcb.client.android.objects;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Robert Ende on 17.08.16.
 */
public class ConnectivityMonitorAndroid extends BaseInstanceEnabler {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitorAndroid.class);

    private static Map<Integer, String> ips = new HashMap<>();

    static {
        ips.put(0, "192.168.133.37");
        ips.put(1, "192.168.133.38");
        ips.put(2, "192.168.133.39");
    }

    @Override
    public ReadResponse read(int resourceid) {
        LOG.debug("Read on Device Resource " + resourceid);

        switch (resourceid) {
            case 0: // current network bearer
                return ReadResponse.success(resourceid, 1337); // Ethernet
            //case 1: // network bearers
            //    //Map<Integer, Long> map = new HashMap<>();
            //    //map.put(0, (long) 41);
            //    return ReadResponse.success(resourceid, availableBearers, ResourceModel.Type.INTEGER);
            //case 2: // signal strength
            //    return ReadResponse.success(resourceid, signalStrength);
            //case 3: // link quality
            //    return ReadResponse.success(resourceid, linkQuality);
            case 4: // ip addresses
                //return ReadResponse.success(resourceid, ips, ResourceModel.Type.STRING);
            //case 5: // router ip
            //    return ReadResponse.success(resourceid, routerIps, ResourceModel.Type.STRING);
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
}
