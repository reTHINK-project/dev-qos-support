/**
 ******************************************************************************
 * @b Project : ReThink
 *
 * @b Sub-project : Broker
 *
 ******************************************************************************
 *
 *                       Copyright (C) Orange Labs
 *                       Orange Labs - PROPRIETARY
 *
 *       Disclosure to third parties or reproduction in any form what-
 *       soever, without prior written consent, is strictly forbidden
 *
 ******************************************************************************
 *
 * @brief Entry point of the Broker server
 *
 * @file
 *
 */
'use strict';
var fs = require('fs');
var config = JSON.parse(fs.readFileSync('./app/config.json','utf8'));
var DB_HOST = config.DB_HOST;
var DB_PORT = config.DB_PORT;
var SERVICE_PORT = config.SERVICE_PORT;
var url = require('url'),
    redis = require("redis"),
    dao = require("./app/dao")('redis',DB_PORT,DB_HOST),
    bodyParser = require('body-parser'),
    express = require('express'),
    app = express(),
    server = require('https').createServer({
        key: fs.readFileSync('sslkeys/server-key.pem'),
        cert: fs.readFileSync('sslkeys/server-crt.pem'),
        ca: fs.readFileSync('sslkeys/ca-crt.pem'),
        },
        app);
// No const available in strict mode ? oO

var basicAuth = require('basic-auth');

var auth = function (req, res, next) {
  function unauthorized(res) {
    res.set('WWW-Authenticate', 'Basic realm=Authorization Required');
    return res.sendStatus(401);
  }

  var user = basicAuth(req);

  if (!user || !user.name || !user.pass) {
    return unauthorized(res);
  }

  if (user.name == config.LOGIN && user.pass == config.PASSWD) {
    return next();
  } else {
    return unauthorized(res);
  }
};

server.listen(SERVICE_PORT);

//docRouter(app, "https://127.0.0.1:8181");

app.use(express.static(__dirname + '/public'));
app.use(bodyParser.urlencoded({ extended: false }));
app.use(function(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
  next();
});

// Importing routes
require('./app/routes.js')(app, auth, dao);

// Pattern subscription
dao.psubscribe("turn/*/*/realm/*/user/*/allocation/*/traffic");

// Start the HTTP server
console.log('Listen on 8181') ;
//http_server.listen(8181);
