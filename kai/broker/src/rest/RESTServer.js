var Express = require('express');
var app = Express();
var bodyParser = require('body-parser');
app.use(bodyParser.json()); // for parsing application/json
var beautify = require("json-beautify");

import QoSRequestHandler from '../qosrequesthandler';

var nodeStates = {};

/**
 * The lifetime of an entry
 */
var lifetime = 10;

/**
 * The time when the housekeeping function shall be fired
 */
var housekeepingTimer = 1000;


/**
 * The REST Server
 */
export default class RESTServer {


  /**
   * construction of the REST Server
   * @param  {Object} config      configuration object
   */
  constructor(config) {
    console.log("Instantiated the REST Server");
    this.config = config;
    this.initialize();
  }

  start() {
    app.listen(this.config.REST_PORT);
    console.log('Listening on port ' + this.config.REST_PORT + '...');
  }

  initialize() {
    console.log("Initializing the resources");

    app.get('/turn-servers/:sessionId/:servingArea', (req, res) => {
      this._processTURNServerRequest(req, res);
    });

    app.post('/turn-servers-management/update/', (req, res) => {
      // Extract servingArea, from, to, rtt out of the JSON Body
      this._processUpdateMessage(req, res);
      res.send("OK for the update");
    });

    app.get('/turn-servers-management/listAgents/:servingArea', (req, res) => {
      this._processTURNServerAgentListReq(req, res);
    });

    this.qosHandler = new QoSRequestHandler();
    console.log("Done Initializing");

    // Housekeeping
    setInterval(this._houseKeeping, housekeepingTimer);
  }

  /**
   * Housekeeping
   */
  _houseKeeping() {
    for (var area in nodeStates) {
      // Go through all areas
      for (var key in nodeStates[area]) {
        var servingArea = nodeStates[area];
        var entry = servingArea[key];
        entry.remoteList[key].ttl = entry.remoteList[key].ttl - housekeepingTimer/1000;
        if (entry.remoteList[key].ttl <= 0) {
          console.log("Remove " + key + " because ttl is lteq 0");
          delete servingArea[key];
        }
      }
    }
  }

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
  }

  /**
   * Process the incoming request and respond accordingly.
   */
  _processTURNServerRequest(req, res) {
    res.type("application/json; charset=utf-8");
    res.send({
      description: 'List of TURN Servers to use',
      sessionId: req.params.sessionId,
      turnServers: this._calculateBestTURNServer(req.params.servingArea)
    });
  }

  /**
   * Process the update message and push them into the storage "array"
   */
  _processUpdateMessage(req, res) {
    var jsonBody = {};
    jsonBody = req.body;
    var nodeStatus = {
      servingArea: jsonBody.servingArea,
      from: jsonBody.from,
      to: jsonBody.to,
      rtt: jsonBody.rtt,
      ipAddress: jsonBody.ipAddress,
      turnPort: jsonBody.turnPort,
      turnUser: jsonBody.turnUser,
      turnPass: jsonBody.turnPass,
      agentAddress: jsonBody.agentAddress,
      ttl: lifetime
    }
    // console.log("Pushing " + JSON.stringify(nodeStatus) + " to states list");
    this._pushToStates(nodeStatus);
  }

  /*
   * Title: _pushToStates
   * Description: Pushes the State information into the given servingArea
   */
  _pushToStates(status) {
    if (status && status.servingArea) {
      // If this servingArea is not existing, create a new array.
      if (!nodeStates[status.servingArea]) {
        nodeStates[status.servingArea] = {};
      }
      // Extract the serving area data
      let servingArea = nodeStates[status.servingArea];

      // check if the Agent/TURN Node already exists
      if (!servingArea[status.from]) {
        servingArea[status.from] = {};
      }
      // Get the actual element
      let node = servingArea[status.from];

      // Check if the remote Hosts list is already existing
      if (!node.remoteList) {
        node.remoteList = {};
      }

      // Push the latest data
      node.remoteList[status.to] = status;

      // print overall object:
      // console.log("Current States:");
      // console.log(beautify(nodeStates, null, 2, 100));

    } else {
      console.log("ERR: Status JSON is undefined or Serving Area not set");
    }
  }

  /*
   * Process, based on information we store, the best/most feasible TURN Server
   */
  _calculateBestTURNServer(servingArea) {
    console.log("Try to get the data for the following servingArea: " + servingArea);
    let turnservers = {};

    turnservers = nodeStates[servingArea];
    return turnservers;
  }


  /**
   * Title: _processTURNServerListReq
   * Description: Responds the list of agents which are running inside the
   * same servingArea.
   */
  _processTURNServerAgentListReq(req, res) {
    let servingArea = req.params.servingArea;
    if (servingArea) {
      let nodes = nodeStates[servingArea]; // get all nodes from this serving area
      let outArray = [];
      // console.log(">>> LIST " + JSON.stringify(nodes));
      console.log("\nServing Area: " + servingArea);
      console.log("======");
      for (var nodeId in nodes) {
        console.log("Node: " + nodeId + "\t" + nodes[nodeId].remoteList[nodeId].agentAddress + "\tttl=" + nodes[nodeId].remoteList[nodeId].ttl);

        // Present the connections to the other agents
        for (var rAgent in nodes[nodeId].remoteList) {
          if(rAgent != nodeId) {
            console.log("\tto " + rAgent + "\trtt=" + nodes[nodeId].remoteList[rAgent].rtt);
          }
        }
        outArray.push(nodes[nodeId].remoteList[nodeId].agentAddress);
      }
      console.log("======");
      res.send(outArray);
  } else {
    res.send("Wrong or unsupported ServingArea");
  }
}

} // end class
