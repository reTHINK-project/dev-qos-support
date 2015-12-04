import RESTServer from './rest/RESTServer';

// WebSocket
let BROKER_CONFIG = {
  WS_PORT: 8666,
  REST_PORT: 8667
};

// Start REST SERVER server
let restServer = new RESTServer( BROKER_CONFIG );
restServer.start();
