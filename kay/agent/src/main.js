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
var brokerUrl = "http://localhost:8667";
var restApp = express();
var ownId = generateUUID();
var restPort = 10000;
var timers = [];
var ownIp = ""; //To be edited and based on the TURN config
var TURNPort = 3478; //To be edited and based on the TURN config
var turnUser = generateUUID();
var turnPass = generateUUID();
var servingAreaServers = {};
var nodeTimeout = 100; //seconds

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
console.log(myProcess.argv);
  console.log("Using the following IP Address: " + myProcess.argv[2]);
  if (myProcess.argv[2]) {
      ownIp = myProcess.argv[2];
  }

  console.log("Using the following port: " + myProcess.argv[3]);
  if (myProcess.argv[3]) {
    if (!isNaN(myProcess.argv[3])) {
      restPort = myProcess.argv[3];
    }
  }

  startREST();

  // Create first entry (self ref.)
  // servingAreaServers["http://" + ownIp + ":" + restPort] = nodeTimeout;
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
  setInterval(getRemoteAgents, 5000);
  setInterval(callRemoteAgents, 2000);

  // Push own entry towards the broker
  pushDataToBroker(servingAreaName, ownId, 0);

}

/**
 * calls the broker for agents inside the given servingArea
 */
function getRemoteAgents() {
  requestify.get(brokerUrl + '/turn-servers-management/listAgents/' + servingAreaName)
  .then(function(response) {
    console.log("getRemoteAgents(): Done requesting agents");
    var body = response.getBody();
    console.log("getRemoteAgents(): The response body (length=" + body.length + "): " + body);
    if(body.length <= 0) {
      console.log("getRemoteAgents(): Response is empty");
    } else {
      for(var i = 0, len = body.length; i < len; i++) {
        var element = body[i];
        servingAreaServers[element] = nodeTimeout;
      }
    }
    console.log("getRemoteAgents(): We have the following Serving Area Servers: " + JSON.stringify(servingAreaServers));
  }, function(err) {
    console.log("getRemoteAgents(): Error requesting list of agents: " + err);
  });
}

function callRemoteAgents() {
  console.log("> Call remote agents called");
  for(var node in servingAreaServers) {
    console.log(">> Found " + node);
    callRemoteAgent(node);
  }
}

function callRemoteAgent(host) {
  requestify.get("http://" + host + '/performance/ping/' + servingAreaName + '/' + microtime.now()).then(function(response) {
    var body = response.getBody();
    var now = microtime.now();
    var calc = Math.round((now - parseInt(body.reqTimestamp)) / 1000);
    log("[" + host + ", Node " + body.nodeId + "] Area: " + body.servingArea + ", Response RTT: " + calc + "ms");
    pushDataToBroker(body.servingArea, body.nodeId, calc);
  }, function(err) {
    console.log("Error trying to ping the given agent address (" + host + " ): " + err);
  });
}

function pushDataToBroker(servingArea, toId, rtt) {
  // Push data to Broker
  // /turn-servers/update/:servingArea/:from/:to/:rtt
  requestify.post(brokerUrl + "/turn-servers-management/update/", {
    agentAddress: ownIp + ":" + restPort,
    servingArea: servingArea,
    from: ownId,
    to: toId,
    rtt: rtt,
    ipAddress: ownIp,
    turnPort: TURNPort,
    turnUser: turnUser,
    turnPass: turnPass
  }).then(function(response) {
    log("Done pushing");
  }, function(err) {
    console.log("Error updating the broker " + JSON.stringify(err));
  });
}

// Start
runMain();