"use strict";

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

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
var mid = 0;

var LHCB = function () {
    function LHCB() {
        _classCallCheck(this, LHCB);

        this.l = new Logger(this);
        this.l.d("Creating new LHCB Lib from:", arguments);
    }

    _createClass(LHCB, [{
        key: "getBroker",
        value: function getBroker(host, port) {
            return new LHCBBroker(host, port);
        }
    }, {
        key: "getLocalClient",
        value: function getLocalClient() {
            return new LHCBClient();
        }
    }]);

    return LHCB;
}();

var LHCBBroker = function () {
    function LHCBBroker(host, port) {
        _classCallCheck(this, LHCBBroker);

        this.l = new Logger(this);
        this.l.d("Creating new LHCB Broker from:", arguments);
        this.ws = new WebSocket("wss://" + host + ":" + port + "/ws");
        this.l.d("Created WebSocket:", this.ws);
    }

    _createClass(LHCBBroker, [{
        key: "read",
        value: function read(name) {
            var _this = this;

            return new Promise(function (resolve) {
                _this.l.d("Running 'read' Promise");

                var request = { "type": "read", "client": name };
                var jsonRequest = JSON.stringify(request);
                _this.ws.onmessage = function (msg) {
                    var response = JSON.parse(msg.data);
                    _this.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                    resolve(response);
                };
                _this.l.d("Sending request:", jsonRequest);
                _this.ws.send(jsonRequest);
            });
        }
    }, {
        key: "execute",
        value: function execute(client, funcName, args) {
            var _this2 = this;

            return new Promise(function (resolve) {
                _this2.l.d("Running 'execute' Promise");

                var request = { "type": "execute", "client": client, "value": { "name": funcName, "args": args } };
                var jsonRequest = JSON.stringify(request);
                _this2.ws.onmessage = function (msg) {
                    var response = JSON.parse(msg.data);
                    _this2.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                    resolve(response);
                };
                _this2.l.d("Sending request:", jsonRequest);
                _this2.ws.send(jsonRequest);
            });
        }
    }, {
        key: "getRemoteClient",
        value: function getRemoteClient(name) {
            return new LHCBClient(this, name);
        }
    }]);

    return LHCBBroker;
}();

var LHCBClient = function () {
    function LHCBClient(broker, name) {
        var _this3 = this;

        _classCallCheck(this, LHCBClient);

        this.l = new Logger(this);
        this.l.d("Creating new LHCB Client from:", arguments);

        this.ready = false;
        if (!broker || !name) {
            (function () {
                _this3.l.d("Unspecified Broker or Name, trying to connect locally AND get BrokerInfo");
                var ws = new WebSocket("wss://localhost:9443/ws");

                var request = {
                    "type": "execute", "mid": mid++, "value": {
                        "name": "getBrokerInfo"
                    }
                };
                var jsonRequest = JSON.stringify(request);
                ws.onmessage = function (msg) {
                    var response = JSON.parse(msg.data);
                    _this3.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                    if (response.type == "response") {
                        _this3.name = response.value.name;
                        _this3.broker = new LHCBBroker(response.value.host, response.value.port);
                        _this3.ready = true;
                    }
                };
                ws.onopen = function () {
                    _this3.l.d("WebSocket open");
                    _this3.l.d("Sending request:", jsonRequest);
                    ws.send(jsonRequest);
                };
                _this3.localWs = ws;
            })();
        } else {
            this.l.d("Broker and Name specified, communicating exclusively through Broker");
            this.broker = broker;
            this.name = name;
            this.ready = true;
        }
    }

    _createClass(LHCBClient, [{
        key: "isReady",
        value: function isReady() {
            return this.ready;
        }
    }, {
        key: "read",
        value: function read() {
            var _this4 = this;

            if (this.localWs && this.localWs.readyState == WebSocket.OPEN) {
                this.l.d("Local WebSocket is open, reading locally...");
                return new Promise(function (resolve) {
                    _this4.l.d("Running 'read' Promise");

                    var request = { "type": "read" };
                    var jsonRequest = JSON.stringify(request);
                    _this4.localWs.onmessage = function (msg) {
                        var response = JSON.parse(msg.data);
                        _this4.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                        resolve(response);
                    };
                    _this4.l.d("Sending request:", jsonRequest);
                    _this4.localWs.send(jsonRequest);
                });
            } else {
                this.l.d("No local WebSocket connection to LHCB Client -> Reading via Broker...");
                return this.broker.read(this.name);
            }
        }
    }, {
        key: "execute",
        value: function execute(funcName, args) {
            var _this5 = this;

            if (this.localWs && this.localWs.readyState == WebSocket.OPEN) {
                this.l.d("Local WebSocket is open, executing locally...");
                return new Promise(function (resolve) {
                    _this5.l.d("Running 'execute' Promise");

                    var request = { "type": "execute", "value": { "name": funcName, "args": args } };
                    var jsonRequest = JSON.stringify(request);
                    _this5.localWs.onmessage = function (msg) {
                        var response = JSON.parse(msg.data);
                        _this5.l.d("Got response for " + jsonRequest + ":", JSON.stringify(response, null, 2));
                        resolve(response);
                    };
                    _this5.l.d("Sending request:", jsonRequest);
                    _this5.localWs.send(jsonRequest);
                });
            } else {
                this.l.d("No local WebSocket connection to LHCB Client -> Executing via Broker...");
                return this.broker.execute(this.name, funcName, args);
            }
        }
    }, {
        key: "getBroker",
        value: function getBroker() {
            return this.broker;
        }
    }, {
        key: "getName",
        value: function getName() {
            return this.name;
        }
    }]);

    return LHCBClient;
}();

/**
 * Simple class for easier logging
 */


var Logger = function () {
    function Logger(obj) {
        var _this6 = this;

        _classCallCheck(this, Logger);

        this.name = obj.constructor.name;
        // console.debug("Setting up Logger for", this.name);
        var _iteratorNormalCompletion = true;
        var _didIteratorError = false;
        var _iteratorError = undefined;

        try {
            var _loop = function _loop() {
                var name = _step.value;

                var method = obj[name];
                // Supposedly you'd like to skip constructor
                if (!(method instanceof Function) || method === obj) return "continue";
                // console.log("injecting logger into", name);
                var self = _this6;
                obj[name] = function () {
                    console.debug(self.name + "." + name + "():", arguments);
                    return method.apply(obj, arguments);
                };
            };

            for (var _iterator = Object.getOwnPropertyNames(Object.getPrototypeOf(obj))[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                var _ret2 = _loop();

                if (_ret2 === "continue") continue;
            }
        } catch (err) {
            _didIteratorError = true;
            _iteratorError = err;
        } finally {
            try {
                if (!_iteratorNormalCompletion && _iterator.return) {
                    _iterator.return();
                }
            } finally {
                if (_didIteratorError) {
                    throw _iteratorError;
                }
            }
        }

        this.c = console;
    }

    _createClass(Logger, [{
        key: "d",
        value: function d(msg) {
            for (var _len = arguments.length, args = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
                args[_key - 1] = arguments[_key];
            }

            arguments[0] = this.name + ": " + arguments[0];
            this.c.debug.apply(this.c, arguments);
        }
    }, {
        key: "l",
        value: function l(msg) {
            for (var _len2 = arguments.length, args = Array(_len2 > 1 ? _len2 - 1 : 0), _key2 = 1; _key2 < _len2; _key2++) {
                args[_key2 - 1] = arguments[_key2];
            }

            arguments[0] = this.prefix + arguments[0];
            this.c.log.apply(this.c, arguments);
        }
    }, {
        key: "i",
        value: function i(msg) {
            for (var _len3 = arguments.length, args = Array(_len3 > 1 ? _len3 - 1 : 0), _key3 = 1; _key3 < _len3; _key3++) {
                args[_key3 - 1] = arguments[_key3];
            }

            arguments[0] = this.prefix + arguments[0];
            this.c.info.apply(this.c, arguments);
        }
    }, {
        key: "w",
        value: function w(msg) {
            for (var _len4 = arguments.length, args = Array(_len4 > 1 ? _len4 - 1 : 0), _key4 = 1; _key4 < _len4; _key4++) {
                args[_key4 - 1] = arguments[_key4];
            }

            arguments[0] = this.prefix + arguments[0];
            this.c.warn.apply(this.c, arguments);
        }
    }, {
        key: "e",
        value: function e(msg) {
            for (var _len5 = arguments.length, args = Array(_len5 > 1 ? _len5 - 1 : 0), _key5 = 1; _key5 < _len5; _key5++) {
                args[_key5 - 1] = arguments[_key5];
            }

            arguments[0] = this.prefix + arguments[0];
            this.c.error.apply(this.c, arguments);
        }
    }]);

    return Logger;
}();

window.LHCB = new LHCB();

// export default LHCB;
//# sourceMappingURL=lhcb.js.map