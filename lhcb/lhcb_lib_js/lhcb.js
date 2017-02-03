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
let mid = 0;

class LHCB {
    constructor() {
        this.l = new Logger(this);
        this.l.d("Creating new LHCB Lib from:", arguments);
    }

    getBroker(host, port) {
        return new LHCBBroker(host, port);
    }

    getLocalClient() {
        return new LHCBClient();
    }
}

class LHCBBroker {
    constructor(host, port) {
        this.l = new Logger(this);
        this.l.d("Creating new LHCB Broker from:", arguments);
        this.ws = new WebSocket("wss://" + host + ":" + port + "/ws");
        this.l.d("Created WebSocket:", this.ws);
    }

    read(name) {
        return new Promise((resolve) => {
            this.l.d("Running 'read' Promise");

            let request = {"type": "read", "client": name};
            let jsonRequest = JSON.stringify(request);
            this.ws.onmessage = (msg) => {
                let response = JSON.parse(msg.data);
                this.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                resolve(response);
            };
            this.l.d("Sending request:", jsonRequest);
            this.ws.send(jsonRequest);
        });
    }

    execute(client, funcName, args) {

        return new Promise((resolve) => {
            this.l.d("Running 'execute' Promise");

            let request = {"type": "execute", "client": client, "value": {"name": funcName, "args": args}};
            let jsonRequest = JSON.stringify(request);
            this.ws.onmessage = (msg) => {
                let response = JSON.parse(msg.data);
                this.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                resolve(response);
            };
            this.l.d("Sending request:", jsonRequest);
            this.ws.send(jsonRequest);
        })
    }

    getRemoteClient(name) {
        return new LHCBClient(this, name);
    }
}

class LHCBClient {
    constructor(broker, name) {
        this.l = new Logger(this);
        this.l.d("Creating new LHCB Client from:", arguments);

        this.ready = false;
        if (!broker || !name) {
            this.l.d("Unspecified Broker or Name, trying to connect locally AND get BrokerInfo");
            let ws = new WebSocket("wss://localhost:9443/ws");

            let request = {
                "type": "execute", "mid": mid++, "value": {
                    "name": "getBrokerInfo"
                }
            };
            let jsonRequest = JSON.stringify(request);
            ws.onmessage = (msg) => {
                let response = JSON.parse(msg.data);
                this.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                if (response.type == "response") {
                    this.name = response.value.name;
                    this.broker = new LHCBBroker(response.value.host, response.value.port);
                    this.ready = true;
                }
            };
            ws.onopen = () => {
                this.l.d("WebSocket open");
                this.l.d("Sending request:", jsonRequest);
                ws.send(jsonRequest);
            };
            this.localWs = ws;
        } else {
            this.l.d("Broker and Name specified, communicating exclusively through Broker");
            this.broker = broker;
            this.name = name;
            this.ready = true;
        }
    }

    isReady() {
        return this.ready;
    }

    read() {
        if (this.localWs && this.localWs.readyState == WebSocket.OPEN) {
            this.l.d("Local WebSocket is open, reading locally...");
            return new Promise((resolve) => {
                this.l.d("Running 'read' Promise");

                let request = {"type": "read"};
                let jsonRequest = JSON.stringify(request);
                this.localWs.onmessage = (msg) => {
                    let response = JSON.parse(msg.data);
                    this.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                    resolve(response);
                };
                this.l.d("Sending request:", jsonRequest);
                this.localWs.send(jsonRequest);
            })
        } else {
            this.l.d("No local WebSocket connection to LHCB Client -> Reading via Broker...");
            return this.broker.read(this.name);
        }
    }

    execute(funcName, args) {
        if (this.localWs && this.localWs.readyState == WebSocket.OPEN) {
            this.l.d("Local WebSocket is open, executing locally...");
            return new Promise((resolve) => {
                this.l.d("Running 'execute' Promise");

                let request = {"type": "execute", "value": {"name": funcName, "args": args}};
                let jsonRequest = JSON.stringify(request);
                this.localWs.onmessage = (msg) => {
                    let response = JSON.parse(msg.data);
                    this.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                    resolve(response);
                };
                this.l.d("Sending request:", jsonRequest);
                this.localWs.send(jsonRequest);
            });
        } else {
            this.l.d("No local WebSocket connection to LHCB Client -> Executing via Broker...");
            return this.broker.execute(this.name, funcName, args);
        }
    }

    getBroker() {
        return this.broker;
    }

    getName() {
        return this.name;
    }
}

/**
 * Simple class for easier logging
 */
class Logger {
    constructor(obj) {
        this.name = obj.constructor.name;
        // console.debug("Setting up Logger for", this.name);
        for (let name of Object.getOwnPropertyNames(Object.getPrototypeOf(obj))) {
            let method = obj[name];
            // Supposedly you'd like to skip constructor
            if (!(method instanceof Function) || method === obj) continue;
            // console.log("injecting logger into", name);
            let self = this;
            obj[name] = function () {
                console.debug(self.name + "." + name + "():", arguments);
                return method.apply(obj, arguments);
            }
        }
        this.c = console;
    }

    d(msg, ...args) {
        arguments[0] = this.name + ": " + arguments[0];
        this.c.debug.apply(this.c, arguments);
    }

    l(msg, ...args) {
        arguments[0] = this.prefix + arguments[0];
        this.c.log.apply(this.c, arguments);
    }

    i(msg, ...args) {
        arguments[0] = this.prefix + arguments[0];
        this.c.info.apply(this.c, arguments);
    }

    w(msg, ...args) {
        arguments[0] = this.prefix + arguments[0];
        this.c.warn.apply(this.c, arguments);
    }

    e(msg, ...args) {
        arguments[0] = this.prefix + arguments[0];
        this.c.error.apply(this.c, arguments);
    }
}

window.LHCB = new LHCB();

// export default LHCB;