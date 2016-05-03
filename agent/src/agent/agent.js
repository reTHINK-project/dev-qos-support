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

var turnConnector = require('../turnconnector/turnconnector.js');

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

			this.turnServerProbeTimeout = 1000 * 3;
			this.maxTurnSessions = 100;

			/*
			 * The list of TURN Agents we try to probe.
			 * This list is pre-defined on development state. However,
			 * that list needs to be retrieved from the broker in the final development.
			 *
			 */
			this.turnServerList = [];

			/**
			 * The Connector for the TURN Service via Telnet
			 */
			this.connector;

		} // end constructor

	/**
	 * Starting the stuff
	 */
	start() {
		this._registerAtBroker()
			.then(
				(res) => {
					this._runWatchDogs();
					this._startTURNConnector();
				},
				(err) => {
					console.log("Error occured: " + err);
				}
			);
	}

	/**
	 * Start the TURN Connector to connect to the TURN servers in our
	 * Data Center Domain
	 */
	_startTURNConnector() {

    // Create the TURN Connector object
    this.connector = new turnConnector();

	}

	/**
	 * run the watchdogs and timers
	 */
	_runWatchDogs() {

			// Re-Register at Broker with the given timer
			setTimeout(() => {
				this.isRegisteredAtBroker = false;
				this._registerAtBroker()
			}, this.registerTimeout);

			// Run the TURN Server probing
			setInterval(() => {
				console.log("Probing TURN service");
				this._probeTurnServer();
			}, 1500);
		} // end runWatchDogs

	/**
	 * Show an Status Report the current Status of all reached TURN Agents
	 */
	_statusReport() {
		console.log("Current stored TURN Agents");
		// ...
		console.log("--------------------------");
	}

	_probeTurnServer() {
		this.connector.runProbe()
			.then((totalSessions) => {
				console.log("The connected TURN Service has currently " + totalSessions + " total sessions.");
				return totalSessions;
			})
			.then((totalSessions) => {
				// Push information to broker
				return this._updateStatistics(totalSessions);
			})
			.then((res) => {
				console.log("Successful update of current active Sessions of TURN service.");
			})
			.catch(
				(err) => {
					console.log("Error occured: ", err);
				});
	}

	_updateStatistics(totalSessions) {
		return new Promise((resolve, reject) => {

			let requestRes = this.brokerUrl + "/updateServiceLocation/";
			//console.log("Updating Broker-Entry with URL: " + requestRes);

			// Run a POST Request
			requestify.put(requestRes, {
						agentId: this._ownId,
						servingArea: this.servingAreaName,
						action: "update",
						activeSessions: totalSessions,
						maxSessions: this.maxTurnSessions
					}
        ).then(
				(response) => {
					console.log("Received Update Respose from Broker: " + response.body);
					console.log("ResponseCode:" + response.getCode());
					if (response.getCode() == 200) {
						resolve(response.body);
					} else {
						reject("No 200 while trying to update the session statistics. Response Code:" + response.getCode());
					}
				},
				(err) => {
					console.log("Error updating the turn sessios at the Broker " + JSON.stringify(err));
					reject(err);
				}
			);
		});
	}

	/**
	 * Register at the broker
	 */
	_registerAtBroker() {
			return new Promise((resolve, reject) => {
				let requestRes = this.brokerUrl + "/registerServiceLocation/";
				//console.log("Registering at Broker with URL: " + requestRes);

				// Run a POST Request
				requestify.post(requestRes, {
							agentId: this._ownId,
							servingArea: this.servingAreaName,
							action: "register",
							isAccessAgent: this.isAccessAgent
						}
        ).then(
					(response) => {
						//console.log("Incoming response from broker " + response.body);
						//console.log("ResponseCode:" + response.getCode());
						this.isRegisteredAtBroker = this._validate("registerResponse", response);
						//console.log("Validation and isRegisteredAtBroker: " + this.isRegisteredAtBroker);
						if (this.isRegisteredAtBroker) {
							resolve(this.isRegisteredAtBroker);
						} else {
							reject(false);
						}
					},
					(err) => {
						console.log("Error registering at the Broker " + JSON.stringify(err));
						reject(false);
					}
				);
			});
		} // end registerAtBroker

	/**
	 * Validate the given content and response with true or false
	 */
	_validate(what, content) {
			let ret = true;

			switch (what) {
			case "registerResponse":
				ret = this._validate("isJSON", content.body);
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
			var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
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
