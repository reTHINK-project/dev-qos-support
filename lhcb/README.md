# Last Hop Connectivity Broker

## Overview

  * LHCB consists of two parts: broker and client 
  * LHCB Broker central point which provides connectivity information about attached clients
  * LHCB Client feeds connectivity information about the client it runs on to the broker

## Starting the LHCB

### Starting the LHCB using docker images

Docker images are provided online, their respective names are:

* [`rethink/lhcb-broker`](https://hub.docker.com/r/rethink/lhcb-broker/)
* [`rethink/lhcb-client`](https://hub.docker.com/r/rethink/lhcb-client/)

To launch the docker images, you can use the following commands:
```
docker run -it rethink/lhcb-broker
docker run -it rethink/lhcb-database
```
Starting the images this way, you can also use the supported launch options declared below. For example, in order to have the LHCB Client connect to a specific IP and setup a WebSocket, you can use the following command:
```
docker run -it rethink/lhcb-database -h 172.17.0.2 -ws
```
Please note that the LHCB Client by default tries to connect to a LHCB Broker using `localhost`. Therefore, when running the docker image for the LHCB CLient, you have to specify the IP/host of the LHCB Broker, unless you use the docker specific `--net=host` option (for both LHCB Broker and LHCB Client).

For more information on how to configure docker images, please visit the [Docker documentation website](https://docs.docker.com/).

### Starting the LHCB using the source code obtained from git

The start either the LHCB Broker or Client, obtain a copy of this git repository, e.g. via
```
git clone <url_to_this_git_repository>
```

Afterwards, proceed to the *lhcb* subdirectory via
```
cd <git_repo_root_dir>/lhcb
```

Before running both LHCB Broker and Client, you first have to build the respective jar files via
```
mvn install
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
