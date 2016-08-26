# Last Hop Connectivity Broker

## Overview

  * LHCB consists of two parts: broker and client 
  * LHCB Broker central point which provides connectivity information about attached clients
  * LHCB Client feeds connectivity information about the client it runs on to the broker

## Starting the LHCB

### Starting the LHCB using docker images

### Starting the LHCB using the source code obtained from git

The start either the LHCB Broker or Client, obtain a copy of this git repository, e.g. via
```
git clone <url_to_this_git_repository>
```

Afterwards, proceed to the *lhcb* subdirectory via
```
cd <git_repo_root_dir>/lhcb
```

To start the LHCB Broker, run
```
java -jar lhcb_broker/target/rethink-lhcb-broker-*-jar-with-dependencies.jar
```
and for the LHCB Client
```
java -jar lhcb_client/target/rethink-lhcb-client-*-jar-with-dependencies.jar
```

## Configuring the LHCB

### Configuring the LHCB Broker
The LHCB Broker supports the following launch options for configuration:

option                      | description
--------------------------- | ---------------------------
-http, -h                   | set http port
-ssl, -s,                   | set https port
-coap, -c                   | set CoAP port
-coaps, -cs                 | set CoAPs port

### Configuring the LHCB Client
The LHCB Client supports the following launch options for configuration:

option                      | description
--------------------------- | ---------------------------
-host, -h                   | set LHCB Broker Host Name
-port, -p                   | set LHCB Broker CoAP Port
-dummy, -d                  | provide dummy data instead of real data
-name, -n                   | set client (endpoint) name
-websocket, -ws             | setup WebSocketServer of ConnectivityMonitor