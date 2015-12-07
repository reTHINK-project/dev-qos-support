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

// Global vars
var servingAreaName = "XY";
// TODO: Extract such information from a config file
var servingAreaServers = ["http://localhost:10000", "http://localhost:10001", "http://localhost:10002"];
var brokerUrl = "http://localhost:8667";
var restApp = express();
var ownId = generateUUID();
var restPort = 10000;
var timers = [];

// The Functions

/*
 * Run the main agent routine
 */
function runMain() {
  initialize();

  runWatchDogs();
}

/**
 * The initializing Function
 */
function initialize() {
  console.log("Using the following port: " + myProcess.argv[2]);
  if (myProcess.argv[2]) {
    if (!isNaN(myProcess.argv[2])) {
      restPort = myProcess.argv[2];
    }
  }
  startREST();
}

/**
 * Register the REST Resources for the agent performance measurements
 */
function startREST() {
  restApp.get('/performance/ping/:servingArea/:reqTimestamp', function(req, res) {

    var responeJSON = {};
    var statusCode = 200;

    // Check for valid servingArea && valid timestamp
    if (
      req.params.servingArea == servingAreaName &&
      !isNaN(req.params.reqTimestamp)
    ) {
      responeJSON = {
        "servingArea": req.params.servingArea,
        "reqTimestamp": req.params.reqTimestamp,
        "nodeId": ownId,
        "pingpong": "pong"
      };
      statusCode = 200;
    } else {
      responeJSON = {
        "error": "Serving Area not correct or the timestamp is not set."
      };
      statusCode = 400;
    }
    res.status(statusCode).send(responeJSON);
  });
  restApp.listen(restPort);
  log("Running Agent at HTTP/REST Port: " + restPort);
}

/**
 * Generate a UUID
 * (credits go to http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript)
 * @return uuid {String} ... the generated unique Identifier
 **/
function generateUUID() {
  var d = new Date().getTime();
  var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = (d + Math.random() * 16) % 16 | 0;
    d = Math.floor(d / 16);
    return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
  return uuid;
}

function randomInt(low, high) {
  return Math.floor(Math.random() * (high - low) + low);
}

function log(str) {
  console.log(str);
}

function runWatchDogs() {
  setInterval(callRemoteAgents, 5000);
}

function callRemoteAgents() {
  servingAreaServers.forEach((item) => {
    callRemoteAgent(item);
  });
}

function callRemoteAgent(host) {
  requestify.get(host + '/performance/ping/' + servingAreaName + '/' + microtime.now()).then(function(response) {
    var body = response.getBody();
    var now = microtime.now();
    var calc = Math.round((now - parseInt(body.reqTimestamp))/1000);
    log("[" + host + ", Node " + body.nodeId + "] Area: " + body.servingArea + ", Response RTT: "  +  calc  + "ms");

    // Push data to Broker
    // /turn-servers/update/:servingArea/:from/:to/:rtt
    requestify.get(brokerUrl + "/turn-servers/update/" + body.servingArea + "/" + ownId + "/" + body.nodeId + "/" + calc).then(function(response) {
      log("Done pushing");
    }, function(err) {
      console.log("Error " + err);
    });
  }, function(err) {
    console.log("Error " + err);
  });
}

// Start
runMain();
