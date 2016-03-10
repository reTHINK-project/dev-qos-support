# dev-qos-broker
The repository for the Broker called to provide the best TURN Server to use when Specialized Network Services are requested.

It includes
* the Broker server
* an example appWebRTC interacting with the Broker server
* a TURN server implementation based on [coturn](https://github.com/coturn/coturn)
 

# Pre-requisites
Both Broker and TURN servers are using a shared redis database.

TODO  : provide the configuration instructions for the redis database.

# BrokerServer
* server.js<br>
  it is the entry point for the BrokerServer.<br>
  Its configurations' parameters can be found in file app/config.json
* app/config.json<br>
  contains the followings configuration parameters :
  * DB_HOST : IP or name of the redis database server
  * DB_PORT : Port to join the redis database server
  * SERVICE_PORT : Port used to access to the Broker's provisionning Web server (dashboard)
  * LOGIN : Identifier to access to the Broker's provisionning Web server
  * PASSWD : Password to access to the Broker's provisionning Web server

# appWebRTC - application example
* appWebRTC.js<br>
  The entry point for this example application.<br>
* public/js/main.js<br>
  contains the principal script of the application.<br>
  followings parameters can me modified if needed :
  * BROKERURL : IP and Port to access to the REST API of the BrokerServer
  * RTCSERVICEURL : IP Address (port) of the server that will host the webrtc app
  * CSPNAME : the name of the corresponding CSP defined in the BrokerServer Web interface (dashboard)
  * CLIENTNAME : an identifier of the client using the webrtc appl. this parameter is just stored in redis database for statistical information
  
#Installation
`git clone https://github.com/reTHINK-project/dev-qos-support.git`  
`cd dev-qos-support/broker/BrokerServer`  
`docker build -t rethink/broker .  `  
`docker run --name redis -p 6379:6379 -d redis  `  
`docker run --name broker -p 8080:8080 --link redis:redis -d rethink/broker`  

