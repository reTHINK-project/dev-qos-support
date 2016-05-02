/**
 * Main file for the QoS Agent
 *
 */

// Imports
var requestify = require('requestify');
var express = require('express');
var microtime = require('microtime');
var http = require("http");
var myProcess = require('process');
//var colors = require('colors');
var level = require('../log/level.json');
var colors = require('colors/safe');

export default class Agent {

    /**
     * The constructor
     */
    constructor() {

            this.applicationLogLevel = level.Debug;

            this.turnUser = this._generateUUID();
            this.turnPass = this._generateUUID();
            this._ownId = this._generateUUID();

            this._log(level.Information, "Running Agent: " + this._ownId);
            this._log(level.Information, "The Broker URL: " + this.brokerUrl);

            // Global vars
            this.servingAreaName = "XY";
            this.isAccessAgent = true;
            // TODO: Extract such information from a config file
            this.brokerUrl = "";
            this.restApp = express();
            this.restPort = 10000;
            this.timers = [];
            this.ownIp = ""; //To be edited and based on the TURN config
            this.TURNPort = 3478; //To be edited and based on the TURN config
            this.servingAreaServers = {};
            this.nodeTimeout = 100; //seconds
            this.isRegisteredAtBroker = false; // for development, we set this value to true
            this.registerTimeout = 1000 * 5000; // When do ne need to update the registration for this agent at the broker
            this.registerAttempts = 0;
            this.unsuccessfulRegisterTimeout = 5 * 1000; // the time, when the registering is being tried again
            this.probingTimeout = 5 * 1000;
            this.maxProbes = 10;
            this.showStateTimeout = 10 * 1000;

            /*
             * The list of TURN Agents we try to probe.
             * This list is pre-defined on development state. However,
             * that list needs to be retrieved from the broker in the final development.
             *
             */
            this.turnAgentList = [];

            /*
             * the overall object which stores that lastest performance values
             * of the probed Turn Agents
             *
             * The structure is the following
             * "agentid": {
             *    agentAddress = "xx.xx.xx.xx:ppppp",
             *   lastProbes = [<integer oldest>, ..., <integer newest>],
             *   meanProbes = <integer>
             *   lastSeen = <UnixTimestamp>
             * }
             */
            this.turnProbes = {};

        } // end constructor


    /**
     * Starting the stuff
     */
    start() {
        this._runWatchDogs()
    }

    /**
     * run the watchdogs and timers
     */
    _runWatchDogs() {
            console.log("_runWatchDogs called");

            // Check if the registeing is set to true
            if (this.isRegisteredAtBroker) {

                console.log("Agent is registered at broker, setting timeout for re-registration");

                // Re-Register at Broker with the given timer
                setTimeout(() => {
                    this.isRegisteredAtBroker = false;
                    this._registerAtBroker()
                }, this.registerTimeout);

                // if we are an access agent, we start the probing
                if (this.isAccessAgent) {
                    setInterval(() => {
                        this._runProbing();
                    }, this.probingTimeout);

                    // Status Report to console
                    setInterval(() => {
                        this._statusReport();
                    }, this.showStateTimeout);
                }

            } else {
                // Try to register at Broker
                this._registerAtBroker();
                // Try again if the registering was not successful
                setTimeout(() => {
                    this._runWatchDogs()
                }, this.unsuccessfulRegisterTimeout);
            }
        } // end runWatchDogs

    /**
     * Show an Status Report the current Status of all reached TURN Agents
     */
    _statusReport() {
        this._log(level.Information, "Current stored TURN Agents");
        let agentAddress;
        let meanProbes;
        let lastSeen;
        for (var entry in this.turnProbes) {
            agentAddress = this.turnProbes[entry].agentAddress;
            meanProbes = this.turnProbes[entry].meanProbes;
            lastSeen = parseInt((new Date().getTime() - this.turnProbes[entry].lastSeen) / 1000);
            this._log(level.Information, entry + "\t" + agentAddress + "\t" + "~" + meanProbes + "ms\t" + "last seen before " + lastSeen + "s");
        }
        this._log(level.Information, "--------------------------");
    }

    /**
     * Register at the broker
     */
    _registerAtBroker() {
            let requestRes = this.brokerUrl + "/registerServiceLocation/";
            console.log("Registering at Broker with URL: " + requestRes);

            // Run a POST Request
            requestify.request(
                requestRes, {
                    method: 'POST',
                    body: {
                        agentAddress: this.ownIp + ":" + this.restPort,
                        servingArea: this.servingArea,
                        action: "register",
                        isAccessAgent: this.isAccessAgent
                    },
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    dataType: 'json'
                }
            ).then(
              (response) => {
                console.log("Incoming response from broker " + response.body);
                console.log("ResponseCode:" + response.getCode());
                this.isRegisteredAtBroker = this._validate("registerResponse", response);
                console.log("Validation and isRegisteredAtBroker: " + this.isRegisteredAtBroker);
            },
              (err) => {
                this._log(level.Error, "Error registering at the Broker " + JSON.stringify(err));
            }
          );
        } // end registerAtBroker

    /**
     * Run the actial probing between the agents.
     * We will go though the list this.turnAgentList
     */
    _runProbing() {
            for (var i = 0; i < this.turnAgentList.length; i++) {
                this._callRemoteAgent(this.turnAgentList[i]);
            }
        } // end runProbing

    /**
     * Executes the actual call towards the other Agents
     * @param host The host we want to address, the requested format: <name/ipAddress>:port
     */
    _callRemoteAgent(host) {
            try {
                console.log(" _callRemoteAgent: Calling the remote agent http://" + host + '/performance/ping/' + this.servingAreaName + '/' + microtime.now())
                requestify.get("http://" + host + '/performance/ping/' + this._ownId + '/' + microtime.now()).then((response) => {
                    var body = response.getBody();
                    var now = microtime.now();
                    var calc = Math.round((now - parseInt(body.reqTimestamp)) / 1000);
                    console.log(" _callRemoteAgent [" + host + ", " + body.agentId + "] Response received,  RTT: " + calc + "ms");
                    this._storeProbe(body.agentId, host, calc);
                }, (err) => {
                    this._log(level.Error, "Error trying to ping the given agent address (" + host + " ): " + err.message);
                });
            } catch (err) {
                this._log(level.Error, "Catched error: " + err.message);
            }
        } // end callRemoteAgent

    /**
     * Store the received probe into the interal array
     * we push that in another interval towards the broker
     * @param agentId The Id of the answering agent, to see the relationship between the agents
     * @param host The Host Name/IP Address + Port
     * @param calc The actual calculated Round-Trip Time
     */
    _storeProbe(agentId, host, calc) {

        console.log(" _storeProbe: storing the current probe " + agentId + ", " + host + ", " + calc);

        let turnAgent = this.turnProbes[agentId];
        if (turnAgent == undefined) {
            this.turnProbes[agentId] = {
                agentAddress: host,
                lastProbes: [calc],
                meanProbes: [calc],
                lastSeen: new Date().getTime()
            };
        } else {

            let lastProbes = this.turnProbes[agentId].lastProbes;

            // Check if the probes are already stored, if not create new element for lastProbes
            // and store the first value
            if (lastProbes == undefined) {
                lastProbes = [calc];
            } else {
                // check if the maximum number of probe samples are reached
                // if yes, remove the oldest
                if (lastProbes.length > this.maxProbes) {
                    lastProbes.shift(); // remove the first element (oldest)
                }
                // push the probe to the array
                lastProbes.push(calc);
            }

            // Calculate the mean of the probes
            let meanProbes = 0;
            for (var i = 0; i < lastProbes.length; i++) {
                meanProbes += lastProbes[i];
            }
            meanProbes = meanProbes / lastProbes.length;

            // push everything into the overall objet turnProbes
            this.turnProbes[agentId] = {
                agentAddress: host,
                lastProbes: lastProbes,
                meanProbes: parseInt(meanProbes),
                lastSeen: new Date().getTime()
            }

        }
    }

    /**
     * Validate the given content and response with true or false
     */
    _validate(what, content) {
            let ret = true;

            switch (what) {
                case "registerResponse":
                    ret = this._validate("isJSON", content.body);
                    console.log("Validating JSON returned: " + ret);
                    ret = ret && (content.getCode() == 200);
                    break;

                case "isJSON":
                    try {
                        JSON.parse(content);
                        ret = true;
                    } catch (e) {
                        ret = false;
                    }
                    break;

                case "isUndefined":
                    ret = (content == undefined);
                    break;

            }
            // return the validation result
            return ret;

        } // end validate


    /**
     * The initializing Function
     */
    initialize() {
            console.log("Using Broker URL: " + myProcess.argv[2]);
            if (myProcess.argv[2]) {
                this.brokerUrl = myProcess.argv[2];
            }

            //this.startREST();

        } // end initialize

    /**
     * Register the REST Resources for the agent performance measurements
     */
    startREST() {
            // Check if the system is an Access Agent,
            // If not, we have a TURN Agent and hence,
            // we activate the REST Interface for ping/pong the agents
            if (this.isAccessAgent) {
                console.log("Running as Access Agent Service, no REST Interface will be started");
            } else { //ok, lets activate the REST for TURN Agents
                console.log("Starting the REST Interface for TURN Agents");

                this.restApp.get('/performance/ping/:requestingAgent/:reqTimestamp', (req, res) => {

                    var responeJSON = {};
                    var statusCode = 200;

                    // Check for valid timestamp
                    if (!isNaN(req.params.reqTimestamp)) {
                        responeJSON = {
                            "reqTimestamp": req.params.reqTimestamp,
                            "agentId": this._ownId,
                            "pingpong": "pong"
                        };
                        statusCode = 200;

                        console.log("Received successful ping request from " + req.params.requestingAgent + ", responding accordingly");

                    } else {
                        responeJSON = {
                            "error": "Timestamp is not set."
                        };
                        statusCode = 400;
                    }
                    res.status(statusCode).send(responeJSON);
                });

                this.restApp.listen(this.restPort);

                console.log("Running Agent at HTTP/REST Port: " + this.restPort);
            }

        } // end startRest

    /**
     * Generate a UUID
     * (credits go to http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript)
     * @return uuid {String} ... the generated unique Identifier
     **/
    _generateUUID() {
            var d = new Date().getTime();
            var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = (d + Math.random() * 16) % 16 | 0;
                d = Math.floor(d / 16);
                return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
            });
            return uuid;
        } // end GenerateUUID

    _randomInt(low, high) {
        return Math.floor(Math.random() * (high - low) + low);
    }
}
