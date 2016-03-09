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

package eu.rethink.lhcb.broker.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Robert Ende on 07.03.16.
 */
public class EventServlet extends EventSourceServlet {
    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String EVENT_NOTIFICATION = "NOTIFICATION";

    private static final String EVENT_CLIENT_LIST = "CLIENTS";

    private static final String EVENT_COAP_LOG = "COAPLOG";

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private final LeshanServer server;

    private final Gson gson = new GsonBuilder().create();

    private Set<Event> events = new ConcurrentHashSet<>();


    private final ObservationRegistryListener observationRegistryListener = new ObservationRegistryListener() {

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void newValue(Observation observation, LwM2mNode value) {
            LOG.debug("Received notification from [{}] containing value [{}]", observation.getPath(),
                    value.toString());
            Client client = server.getClientRegistry().findByRegistrationId(observation.getRegistrationId());

            if (client != null) {
                String data = new StringBuffer("{\"ep\":\"").append(client.getEndpoint()).append("\",\"res\":\"")
                        .append(observation.getPath().toString()).append("\",\"val\":").append(gson.toJson(value))
                        .append("}").toString();

                sendEvent(EVENT_NOTIFICATION, data, client.getEndpoint());
            }
        }

        @Override
        public void newObservation(Observation observation) {
            LOG.debug("new observation:{}", observation);
        }
    };

    private final ClientRegistryListener clientRegistryListener = new ClientRegistryListener() {
        private void sendNotify() {
            sendEvent(EVENT_CLIENT_LIST, gson.toJson(getEndpoints()), null);
        }

        @Override
        public void registered(Client client) {
            sendNotify();
        }

        @Override
        public void updated(Client clientUpdated) {
            sendNotify();
        }

        @Override
        public void unregistered(Client client) {
            sendNotify();
        }
    };


    public EventServlet(LeshanServer server) {
        this.server = server;
        server.getObservationRegistry().addListener(this.observationRegistryListener);
        server.getClientRegistry().addListener(this.clientRegistryListener);

    }

    private synchronized void sendEvent(String event, String data, String endpoint) {
        LOG.debug("Dispatching {} event from endpoint {}", event, endpoint);


        for (Event e : events) {
            if ((endpoint == null && e.getEndpoint() == null) || (e.getEndpoint() != null && e.getEndpoint().equals(endpoint))) {
                LOG.debug("to event endpoint: {}", e.getEndpoint());
                e.sendEvent(event, data);
            }
        }
    }


    @Override
    protected EventSource newEventSource(HttpServletRequest req) {
        LOG.debug("newEventSource: {}", req);
        String endpoint = req.getParameter(QUERY_PARAM_ENDPOINT);
        LOG.debug("extracted endpoint: {}", endpoint);
        return new Event(endpoint);
    }

    private String[] getEndpoints() {
        Collection<Client> clients = server.getClientRegistry().allClients();
        Iterator<Client> iterator = clients.iterator();
        String[] endpoints = new String[clients.size()];
        int i = 0;
        while (iterator.hasNext()) {
            Client next = iterator.next();
            endpoints[i++] = next.getEndpoint();
        }
        LOG.debug("generated endpoint list: {}", gson.toJson(endpoints));
        return endpoints;
    }

    private class Event implements EventSource {
        private String endpoint;
        private Emitter emitter;

        public Event(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onOpen(Emitter emitter) throws IOException {
            this.emitter = emitter;
            events.add(this);
            //if (endpoint != null) {
            //    coapMessageTracer.addListener(endpoint, new ClientCoapListener(endpoint));
            //}
            LOG.debug("onOpen. endpoint: " + endpoint);
            LOG.debug("endpoint null? -> {}", endpoint == null);
            if (endpoint != null) {
                try {
                    Client client = server.getClientRegistry().get(endpoint);
                    if (client != null) {
                        LOG.debug("got client for endpoint {}: {}", endpoint, client);
                        ObserveResponse response = server.send(client, new ObserveRequest(4, 0));
                        LOG.debug("got response: {}", response);
                    } else {
                        LOG.warn("tried to observe endpoint {} but it doesn't exist!", endpoint);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                //sendEvent(EVENT_CLIENT_LIST, gson.toJson(getEndpoints()));
            }
        }

        @Override
        public void onClose() {
            //cleanCoapListener(endpoint);
            events.remove(this);
        }

        public void sendEvent(String event, String data) {
            try {
                emitter.event(event, data);
            } catch (IOException e) {
                onClose();
            }
        }

        public String getEndpoint() {
            return endpoint;
        }
    }
}
