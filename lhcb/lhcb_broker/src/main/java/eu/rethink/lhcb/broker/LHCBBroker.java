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

package eu.rethink.lhcb.broker;

import eu.rethink.lhcb.broker.provider.CustomModelProvider;
import eu.rethink.lhcb.broker.servlet.WellKnownServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Last Hop Connectivity Broker - Broker module implementation
 */
public class LHCBBroker {
    private static final Logger LOG = LoggerFactory.getLogger(LHCBBroker.class);

    private LeshanServer leshanServer;
    private Server server;

    private int httpPort = 8080;
    private int coapPort = 5683;

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public void setCoapPort(int coapPort) {
        this.coapPort = coapPort;
    }

    public void start() {
        // create Leshan server
        LeshanServerBuilder lsb = new LeshanServerBuilder();
        lsb.setLocalAddress("0", coapPort);
        lsb.setObjectModelProvider(new CustomModelProvider());
        leshanServer = lsb.build();
        leshanServer.start();

        // create HTTP server
        server = new Server(httpPort);
        ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", true, false);
        ServletHolder servletHolder = new ServletHolder(new WellKnownServlet(leshanServer));
        servletContextHandler.addServlet(servletHolder, "/.well-known/*");
        try {
            LOG.info("Server should be available at: " + server.getURI());
            LOG.info("Starting server...");
            server.start();
        } catch (Exception e) {
            LOG.error("HTTP server error", e);
        }
    }

    public static void main(String[] args) {
        LHCBBroker broker = new LHCBBroker();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            arg = arg.toLowerCase();

            switch (arg) {
                case "-httpport":
                case "-http":
                case "-h":
                    String rawHttpPort = args[++i];
                    int httpPort = 0;
                    // if http address was given (like for coap), extract only port part
                    try {
                        httpPort = Integer.parseInt(rawHttpPort);
                    } catch (IndexOutOfBoundsException e) {
                        //e.printStackTrace();
                    }
                    broker.setHttpPort(httpPort);
                    break;
                case "-sslport":
                case "-ssl":
                case "-s":
                    // TODO implementation
                    break;
                case "-coapport":
                case "-coap":
                case "-c":
                    String rawCoapPort = args[++i];
                    int coapPort = 0;
                    // if http address was given (like for coap), extract only port part
                    try {
                        coapPort = Integer.parseInt(rawCoapPort);
                    } catch (IndexOutOfBoundsException e) {
                        //e.printStackTrace();
                    }
                    broker.setCoapPort(coapPort);
                    break;
                case "-coapsport":
                case "-coaps":
                case "-cs":
                    // TODO implementation
                    break;
                case "-keystorePath":
                case "-kp":
                    // TODO implementation
                    break;
                case "-truststorePath":
                case "-tp":
                    // TODO implementation
                    break;
                case "-keystorePassword":
                case "-kpw":
                    // TODO implementation
                    break;
                case "-keyManagerPassword":
                case "-kmpw":
                    // TODO implementation
                    break;
                case "-truststorePassword":
                case "-tpw":
                    // TODO implementation
                    break;
                default:
                    // unknown arg
                    i++;
            }
        }

        broker.start();
    }
}
