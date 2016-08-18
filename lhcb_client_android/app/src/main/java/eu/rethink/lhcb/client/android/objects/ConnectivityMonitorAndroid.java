package eu.rethink.lhcb.client.android.objects;

import android.app.Activity;
import eu.rethink.lhcb.client.objects.ConnectivityMonitor;
import eu.rethink.lhcb.client.util.Bearers;
import eu.rethink.lhcb.client.util.Tuple;
import eu.rethink.lhcb.client.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Robert Ende on 17.08.16.
 */
public class ConnectivityMonitorAndroid extends ConnectivityMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMonitorAndroid.class);

    private Activity activity = null;

    public ConnectivityMonitorAndroid(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void init() {
        ipThread.start();
    }

    private Thread ipThread = new Thread(new Runnable() {
        @Override
        public void run() {
            ArrayList<Integer> changedResources = new ArrayList<>();
            while (!Thread.interrupted()) {
                Bearers bearers = Utils.getBearers();
                currentBearers = bearers;
                if (!bearers.ips.equals(currentIPs)) {
                    currentIPs = bearers.ips;
                    LOG.debug("current IPs have changed, set to {}", currentIPs);
                    changedResources.add(4);
                }

                // check if current bearer is 1st element in bearers
                if (bearers.bearers.size() == 0) {
                    if (currentBearer != -1) {
                        currentBearer = -1;
                        LOG.debug("no current bearer, set to -1");
                        changedResources.add(0);
                    }
                } else if (!bearers.bearers.get(0).y.equals(currentBearer)) {
                    currentBearer = bearers.bearers.get(0).y;
                    LOG.debug("current bearer has changed, set to {}", currentBearer);
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
                    LOG.debug("available bearers have changed, set to {}", currentAvailableBearers);
                    changedResources.add(1);
                }

                fireResourcesChange(changedResources);
                changedResources.clear();

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    });
}
