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

            console.log("Running Agent: " + this._ownId);
            console.log("The Broker URL: " + this.brokerUrl);

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
            this.turnServerList = [];

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
        console.log("Current stored TURN Agents");
        // ...
        console.log("--------------------------");
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
                        agentId: this._ownId,
                        servingArea: this.servingAreaName,
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
                console.log("Error registering at the Broker " + JSON.stringify(err));
            }
          );
        } // end registerAtBroker

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
