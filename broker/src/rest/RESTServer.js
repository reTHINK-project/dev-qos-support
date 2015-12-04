var Express = require('express');
var app = Express();

import QoSRequestHandler from '../qosrequesthandler';

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
    app.get('/turn-servers/:sessionId', (req, res) => {
      this._processTURNServerRequest(req, res)
    });
    this.qosHandler = new QoSRequestHandler();
    console.log("Done Initializing");
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
      turnServers: this._calculateBestTURNServer()
    });
  }

  /*
   * Process, based on information we store, the best/most feasible TURN Server
   */
  _calculateBestTURNServer() {
    let turnservers = [];

    // TODO: Grab information and calculate the best turn server for
    // the current situation.
    // ... by now some example turn addresses and credentials.
    turnservers.push({
      name: 'turnserver-' + this._generateUUID(),
      ipAddress: '12.13.14.15',
      port: '3478',
      protocol: 'udp',
      username: this._generateUUID(),
      password: this._generateUUID(),
      score: 100
    });
    turnservers.push({
      name: 'turnserver-' + this._generateUUID(),
      ipAddress: '13.14.15.16',
      port: '3478',
      protocol: 'tcp',
      username: this._generateUUID(),
      password: this._generateUUID(),
      score: 70
    });
    return turnservers;
  }

} // end class
