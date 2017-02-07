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

## Using the HTTP & WebSocket interfaces

### Broker HTTP
The LHCB Broker HTTP interface listens on port 8080 for `http` and 8443 for `https` by default.
You can access information about a LHCB client that is connected to that Broker using the following URL path, and providing the necessary information for the request:
```
(http|https)://<broker_host>/.well-known/?[<property>=<p_value>]
```
This is your entry point for the HTTP interface. Requests are built from the HTTP query parameters. 

### Broker WebSocket
The LHCB Broker WebSocket interface listens on the same ports used by the HTTP interface (port 8080 for `ws` and 8443 for `wss` by default).
You can access information about a LHCB client that is connected to that Broker by connecting to the following URL, and providing the necessary information for the request in the payload:
```
(ws|wss)://<broker_host>/ws
```
This is your entry point for the WebSocket interface. Requests are built from the message payload.

### Client WebSocket
The LHCB Client has a WebSocket interface in order to retrieve Client information locally, e.g. from the same device/network.

The interface is almost identical to the Broker WebSocket interface, but serves on port 9443 by default.
The URL is also the same:
```
(ws|wss)://<client_host>/ws
```
Just as with the Broker WebSocket interface, requests are created from the message payload.

### Examples
In order to read the state of "Alice", the corresponding requests for HTTP and WebSocket look like this:
```
https://localhost:8443/.well-known/?type=read&client=Alice

equivalent WebSocket message:
{
  "type": "read",
  "client": "alice"
}
```

This is what a response from may look like:
```
{
  "type": "response",
  "mid": 3,
  "client": "alice",
  "value": {
    "Network Bearer": 41,
    "Available Network Bearer": [
      41
    ],
    "IP Addresses": [
      "123.17.0.1",
      "10.141.65.123"
    ],
    "Router IP Addresse": [
      "10.141.65.1"
    ]
  }
}
```
The `mid` property is always present in the response, but optional for your requests.
It can be provided in order to keep track of which response belongs to which request.
If no `mid` is provided, an unused `mid` is automatically assigned.

Not providing `client` returns a json array in the value field of the response, with names of all connected LHCB Clients, for example:
```
{
  "type": "response",
  "mid": 0,
  "value": [
    "bob",
    "alice"
  ]
}
```