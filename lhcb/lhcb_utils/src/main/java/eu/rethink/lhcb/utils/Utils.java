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

package eu.rethink.lhcb.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mNodeVisitor;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Robert Ende on 18.08.16.
 */
public class Utils {
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    //public static final Gson gson = new GsonBuilder().create();

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final String routeCmd = "route -n";
    private static final Map<String, Integer> IFaceNameToId = new LinkedHashMap<>();

    static {
        IFaceNameToId.put("eth", 41);
        IFaceNameToId.put("ens", 41);
        IFaceNameToId.put("wlan", 21);
        IFaceNameToId.put("wls", 21);
        IFaceNameToId.put("rmnet0", 0);
        IFaceNameToId.put("rmnet1", 1);
        IFaceNameToId.put("rmnet2", 2);
        IFaceNameToId.put("rmnet3", 3);
        IFaceNameToId.put("rmnet3", 4);
        IFaceNameToId.put("rmnet3", 5);
        IFaceNameToId.put("rmnet", 6);
    }

    /**
     * Creates a map of available host addresses for this machine.
     * They key in the map is an index starting from 0
     *
     * @return A map in the form of index:IP
     */
    public static Bearers getBearers() {
        Map<Integer, String> ips = new LinkedHashMap<>();
        List<Tuple<String, Integer>> bearers = new ArrayList<>();

        try {
            int i = 0;
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                if (!iface.isLoopback()) {
                    LOG.trace("getIPs: checking iface {} ", iface.getDisplayName());

                    // only consider interface that are up
                    // try to get bearer kind
                    for (String ifaceName : IFaceNameToId.keySet()) {
                        LOG.trace("check: {} == {}?", iface.getDisplayName(), ifaceName);
                        if (iface.getDisplayName().startsWith(ifaceName)) {
                            int bearer = IFaceNameToId.get(ifaceName);
                            bearers.add(new Tuple<>(iface.getDisplayName(), bearer));
                            break;
                        }
                    }

                    // get IPs
                    Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        ips.put(i++, inetAddresses.nextElement().getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            //e.printStackTrace();
            LOG.warn("Unable parse Network Interfaces", e);
        }

        return new Bearers(bearers, ips);
    }

    /**
     * Creates a map of available router addresses for this machine.
     * They key in the map is an index starting from 0
     *
     * @return A map in the form of index:IP
     */
    public static Map<Integer, String> getGatewayIPs() {
        Map<Integer, String> ips = new LinkedHashMap<>();
        String line = null;
        try {
            Process p = Runtime.getRuntime().exec(routeCmd);
            Scanner sc = new Scanner(p.getInputStream(), "IBM850");

            // "Kernel IP routing table"
            line = sc.nextLine();

            // Destination  Gateway     Genmask     Flags       MSS         Window      irtt        Iface
            line = sc.nextLine();

            // 0.0.0.0      10.147.65.1 0.0.0.0     UG          0           0           0           eth0
            // ...
            int i = 0;
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                do {
                    line = line.replace("  ", " ");
                } while (line.contains("  "));
                String[] splitLine = line.split(" ");
                if (splitLine[3].equals("UG")) {
                    ips.put(i++, splitLine[1]);
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            LOG.warn("Unable to get Gateway IPs", e);
            LOG.trace("Last scanned line before exception: {}", line);
        }
        return ips;
    }

    public static Tuple<Integer, Integer> getLinkQualityAndSignalStrength(String currentBearerName) {
        String line;
        int i, j, k;
        String linkQualityLabel = "Link Quality=";
        String signalLevelLabel = "Signal level=";
        int linkQuality = -1, signalStrength = -1;
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
                    signalStrength = Integer.parseInt(line.substring(j + signalLevelLabel.length(), k - 1).trim());
                }

            }
        } catch (IOException e) {
            //e.printStackTrace();
            LOG.trace("Unable to get WiFi info using iwconfig, probably because it is not installed", e);
        }
        return new Tuple<>(linkQuality, signalStrength);
    }

    public static CompletableFuture<JsonObject> parseCMReadResponse(ObjectModel model, ReadResponse response) {
        LOG.debug("parsing response: {}", response);
        CompletableFuture<JsonObject> promise = new CompletableFuture<>();

        response.getContent().accept(new LwM2mNodeVisitor() {
            @Override
            public void visit(LwM2mObject object) {
                // should never happen
            }

            @Override
            public void visit(LwM2mObjectInstance instance) {
                Map<Integer, LwM2mResource> resources = instance.getResources();
                Map<Integer, ResourceModel> resourceModelMap = model.resources;

                JsonObject json = new JsonObject();

                for (Map.Entry<Integer, LwM2mResource> entry : resources.entrySet()) {
                    json.add(resourceModelMap.get(entry.getKey()).name, gson.toJsonTree(entry.getValue().isMultiInstances() ? entry.getValue().getValues().values().toArray() : entry.getValue().getValue()));
                }

                LOG.debug("final json: {}", json);
                promise.complete(json);
            }

            @Override
            public void visit(LwM2mResource resource) {
                // should never happen

            }
        });

        return promise;
    }

    public static String changeIface(String name, String password) {
        LOG.debug("Interface change request name: " + name + ", pw: " + password);
        Process p;
        try {
            p = Runtime.getRuntime().exec("nmcli d wifi connect " + name + " password " + password);
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOG.debug("Process exitValue: {}", p.exitValue());
            BufferedReader resultBuffer = new BufferedReader(new InputStreamReader(p.exitValue() == 0 ? p.getInputStream() : p.getErrorStream()));
            String result = "";
            String line;
            while ((line = resultBuffer.readLine()) != null) {
                result += line;
            }
            LOG.debug("changeIface result: {}", result);
            return result;

        } catch (IOException e) {
            LOG.error("changeIface failed", e);
            //e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    /**
     * Load and return custom Object Models from models.json
     *
     * @return List of ObjectModels
     */
    public static List<ObjectModel> getCustomObjectModels() {
        InputStream modelStream = Utils.class.getResourceAsStream("/model.json");
        return ObjectLoader.loadJsonStream(modelStream);
    }
}

