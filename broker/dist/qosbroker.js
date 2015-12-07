(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
'use strict';

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

var _restRESTServer = require('./rest/RESTServer');

var _restRESTServer2 = _interopRequireDefault(_restRESTServer);

// WebSocket
var BROKER_CONFIG = {
  WS_PORT: 8666,
  REST_PORT: 8667
};

// Start REST SERVER server
var restServer = new _restRESTServer2['default'](BROKER_CONFIG);
restServer.start();

},{"./rest/RESTServer":3}],2:[function(require,module,exports){
"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var QoSRequestHandler =

/**
 * construction of the QoS Request Handler
 * @param  {Object} config      configuration object
 */
function QoSRequestHandler() {
  _classCallCheck(this, QoSRequestHandler);

  console.log("Instantiated the QoS Request Handler");
};

exports["default"] = QoSRequestHandler;
module.exports = exports["default"];

},{}],3:[function(require,module,exports){
'use strict';

Object.defineProperty(exports, '__esModule', {
  value: true
});

var _createClass = (function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ('value' in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; })();

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError('Cannot call a class as a function'); } }

var _qosrequesthandler = require('../qosrequesthandler');

var _qosrequesthandler2 = _interopRequireDefault(_qosrequesthandler);

var Express = require('express');
var app = Express();

var nodeStates = [];

/**
 * The REST Server
 */

var RESTServer = (function () {

  /**
   * construction of the REST Server
   * @param  {Object} config      configuration object
   */

  function RESTServer(config) {
    _classCallCheck(this, RESTServer);

    console.log("Instantiated the REST Server");
    this.config = config;
    this.initialize();
  }

  // end class

  _createClass(RESTServer, [{
    key: 'start',
    value: function start() {
      app.listen(this.config.REST_PORT);
      console.log('Listening on port ' + this.config.REST_PORT + '...');
    }
  }, {
    key: 'initialize',
    value: function initialize() {
      var _this = this;

      console.log("Initializing the resources");

      app.get('/turn-servers/:sessionId', function (req, res) {
        _this._processTURNServerRequest(req, res);
      });

      // Todo: Needs to become a post with JSON as content.. easier to handle....
      app.get('/turn-servers/update/:servingArea/:from/:to/:rtt', function (req, res) {
        _this._processUpdateMessage(req, res);
      });

      this.qosHandler = new _qosrequesthandler2['default']();
      console.log("Done Initializing");
    }

    /**
     * Generate a UUID
     * (credits go to http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript)
     * @return uuid {String} ... the generated unique Identifier
     **/
  }, {
    key: '_generateUUID',
    value: function _generateUUID() {
      var d = new Date().getTime();
      var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = (d + Math.random() * 16) % 16 | 0;
        d = Math.floor(d / 16);
        return (c == 'x' ? r : r & 0x3 | 0x8).toString(16);
      });
      return uuid;
    }

    /**
     * Process the incoming request and respond accordingly.
     */
  }, {
    key: '_processTURNServerRequest',
    value: function _processTURNServerRequest(req, res) {
      res.type("application/json; charset=utf-8");
      res.send({
        description: 'List of TURN Servers to use',
        sessionId: req.params.sessionId,
        turnServers: this._calculateBestTURNServer()
      });
    }

    /**
     * Process the update message and push them into the storage "array"
     */
  }, {
    key: '_processUpdateMessage',
    value: function _processUpdateMessage(req, res) {
      var servingArea = req.params.servingArea;
      var from = req.params.from;
      var to = req.params.to;

      var nodeStatus = {
        servingArea: req.params.servingArea,
        from: req.params.from,
        to: req.params.to,
        rtt: req.params.rtt
      };

      console.log("pushing to array: " + JSON.stringify(nodeStatus));
      nodeStates.push(nodeStatus);
    }

    /*
     * Process, based on information we store, the best/most feasible TURN Server
     */
  }, {
    key: '_calculateBestTURNServer',
    value: function _calculateBestTURNServer() {
      var turnservers = [];

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
  }]);

  return RESTServer;
})();

exports['default'] = RESTServer;
module.exports = exports['default'];

},{"../qosrequesthandler":2,"express":undefined}]},{},[2,1]);
