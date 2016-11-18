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

import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static eu.rethink.lhcb.utils.Utils.gson;

/**
 * Event Servlet that sends notifications to subscribed browsers about client changes
 * and handles observations on clients.
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

    private Set<Event> events = new ConcurrentHashSet<>();

    // observation listener handles the notifications from the clients and sends them to the browser
    private final ObservationRegistryListener observationRegistryListener = new ObservationRegistryListener() {

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void newValue(Observation observation, LwM2mNode lwM2mNode, List<TimestampedLwM2mNode> list) {
            LOG.debug("Received notification from [{}] containing value [{}]", observation.getPath(),
                    lwM2mNode.toString());

            // get client object that sent the notification
            Client client = server.getClientRegistry().findByRegistrationId(observation.getRegistrationId());

            if (client != null) {
                // build the json notification and send it to the browser
                String data = new StringBuffer("{\"ep\":\"").append(client.getEndpoint()).append("\",\"res\":\"")
                        .append(observation.getPath().toString()).append("\",\"val\":").append(gson.toJson(lwM2mNode))
                        .append("}").toString();

                sendEvent(EVENT_NOTIFICATION, data, client.getEndpoint(), observation.getPath().getResourceId());
            }
        }

        @Override
        public void newObservation(Observation observation) {
            // we don't care for this
            LOG.debug("new observation:{}", observation);
        }
    };

    // client registry listener notifies browser with new client list if new LHCB client connected
    private final ClientRegistryListener clientRegistryListener = new ClientRegistryListener() {
        private void sendNotify() {
            sendEvent(EVENT_CLIENT_LIST, gson.toJson(server.getClientRegistry().allClients()), null, null);
        }

        @Override
        public void registered(Client client) {
            sendNotify();
        }

        @Override
        public void updated(ClientUpdate clientUpdate, Client client) {
            // no need to handle update
        }

        @Override
        public void unregistered(Client client) {
            sendNotify();
        }
    };

    /**
     * Create the EventServlet based on the given LeshanServer
     *
     * @param server
     */
    public EventServlet(LeshanServer server) {
        this.server = server;
        server.getObservationRegistry().addListener(this.observationRegistryListener);
        server.getClientRegistry().addListener(this.clientRegistryListener);

    }

    /**
     * Send an event to each subscribed browser
     *
     * @param event
     * @param data
     * @param endpoint
     * @param resourceId
     */
    private synchronized void sendEvent(String event, String data, String endpoint, Integer resourceId) {
        LOG.debug("Dispatching {} event from endpoint {}", event, endpoint);

        // Event class is event a browser is subscribed to
        for (Event e : events) {
            // only send event if endpoint and (optionally) resourceId matches
            if ((endpoint == null && e.getEndpoint() == null) || (e.getEndpoint() != null && e.getEndpoint().equals(endpoint))) {
                LOG.debug("to event endpoint: {}", e.getEndpoint());
                try {
                    e.sendEvent(event, data);
                } catch (IllegalStateException e1) {
                    e1.printStackTrace();
                    e.onClose();
                }
            }
        }
    }

    @Override
    protected EventSource newEventSource(HttpServletRequest req) {
        LOG.debug("newEventSource: {}", req);
        // someone wants to subscribe to a certain event
        String endpointParam = req.getParameter(QUERY_PARAM_ENDPOINT);

        LOG.debug("extracted endpoint parameter: {}", endpointParam);

        // no param provided = just general event (for e.g. client list)
        if (endpointParam == null)
            return new Event(null);

        // browser wants to only be notified for certain endpoint and (optionally) resource
        String[] splitParam = endpointParam.split("/");
        return new Event(splitParam[0]);
    }

    /**
     * Holds information about an event a browser wants to be notified of when it happens
     */
    private class Event implements EventSource {
        private String endpoint;
        private Emitter emitter;

        public Event(String endpoint) {
            LOG.debug("New EventSource! for {}", endpoint);
            this.endpoint = endpoint;
        }

        @Override
        public void onOpen(Emitter emitter) throws IOException {
            // When the emitter opens, i.e. a browser connection is up, start the observation of the requested client
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
                        ObserveResponse response;
                        response = server.send(client, new ObserveRequest(4, 0));
                        LOG.debug("got response: {}", response);
                    } else {
                        LOG.warn("tried to observe endpoint {} but it doesn't exist!", endpoint);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                sendEvent(EVENT_CLIENT_LIST, gson.toJson(server.getClientRegistry().allClients()));
            }
        }

        @Override
        public void onClose() {
            // remove event from the list and cancel the observation
            events.remove(this);
            LOG.debug("onClose. endpoint: " + endpoint);
            // TODO this might need a check if we have multiple events that are observing the same resource
            server.getObservationRegistry().cancelObservations(server.getClientRegistry().get(endpoint), "/4/0");
        }

        /**
         * Forward event to the browser using the emitter
         *
         * @param event - Event type
         * @param data  - JSON data (stringified)
         */
        public void sendEvent(String event, String data) {
            try {
                emitter.event(event, data);
            } catch (IOException e) {
                //e.printStackTrace();
                onClose();
            }
        }

        /**
         * Get the endpoint name for this event
         *
         * @return name of the endpoint this event is limited to.
         */
        public String getEndpoint() {
            return endpoint;
        }
    }
}
