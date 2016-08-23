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

package eu.rethink.lhcb.client.util;

import java.util.List;
import java.util.Map;

public class Bearers {
    public final List<Tuple<String, Integer>> bearers;
    public final Map<Integer, String> ips;

    public Bearers(List<Tuple<String, Integer>> bearers, Map<Integer, String> ips) {
        this.bearers = bearers;
        this.ips = ips;
    }

    public Tuple<String, Integer> getCurrentBearer() {
        if (bearers.size() > 0)
            return bearers.get(0);
        else
            return null;
    }

    public String getCurrentIp() {
        if (ips.size() > 0 && bearers.size() > 0)
            return ips.get(0);
        else
            return null;
    }
}
