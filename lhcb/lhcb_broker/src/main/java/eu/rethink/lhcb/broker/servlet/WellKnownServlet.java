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

import eu.rethink.lhcb.broker.LHCBBroker;
import eu.rethink.lhcb.broker.RequestHandler;
import eu.rethink.lhcb.utils.RequestCallback;
import eu.rethink.lhcb.utils.message.Message;
import eu.rethink.lhcb.utils.message.exception.InvalidMessageException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Http Servlet that is supposed to handle any request to /.well-known/*
 */
public class WellKnownServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(WellKnownServlet.class);

    private RequestHandler requestHandler;

    private static final String WELLKNOWN_PREFIX = "/.well-known";

    public WellKnownServlet(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.debug("got POST request on {}", req.getPathInfo());
        handleRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.debug("got GET request on {}", req.getPathInfo());
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        resp.addHeader("Access-Control-Allow-Origin", "*");
        String host = req.getHeader("X-Forwarded-Host");
        if (host == null)
            host = req.getHeader("Host");

        LHCBBroker.externalHost = host;
        final AsyncContext asyncContext = req.startAsync();
        asyncContext.start(() -> {
            ServletRequest aReq = asyncContext.getRequest();
            String payload = null;
            try {
                payload = IOUtils.toString(aReq.getReader());
            } catch (IOException e) {
                e.printStackTrace();
            }

            String finalPayload = payload;

            Map<String, String[]> params = aReq.getParameterMap();
            LOG.debug("payload: {}\r\nparams: {}", payload, params);

            RequestCallback cb = new RequestCallback() {

                @Override
                public void response(Message msg) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    try {
                        asyncContext.getResponse().getWriter().write(msg.toString());
                        asyncContext.getResponse().getWriter().flush();
                        asyncContext.complete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void error(Exception e) {
                    String error = "Request failed.\r\npayload: " + finalPayload + "\r\nparams: " + params;
                    LOG.error(error + "\r\nreason: " + e.getLocalizedMessage(), e instanceof InvalidMessageException ? null : e);
                    if (e instanceof InvalidMessageException) {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                    }
                    completeAsyncContext(asyncContext, error + "\r\nreason: " + e.getLocalizedMessage());
                }
            };

            try {
                Message msg = null;

                if (payload.length() > 0) {
                    msg = Message.fromString(payload);
                } else {
                    msg = Message.fromParams(params);
                }

                requestHandler.handleRequest(msg, cb);
            } catch (InvalidMessageException e) {
                cb.error(e);
            }
        });
    }

    private void completeAsyncContext(AsyncContext aContext, String message) {
        try {
            aContext.getResponse().getWriter().write(message);
            aContext.getResponse().getWriter().flush();
        } catch (IOException e1) {
            LOG.error("Unable to send message '" + message + "'to requester", e1);
        }
        aContext.complete();
    }
}
