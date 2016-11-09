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

## Using the HTTP interface

The LHCB Broker HTTP interface listens on port 8080 for HTTP and 8443 for HTTPs by default.
You can access information about a LHCB client that is connected to that Broker using the following URL path:
```
(http|https)://<broker_host>/.well-known/[<client_endpoint_name>/[<resource_id>]]

Resource IDs as standardized by lwM2M:
00 = Network Bearer (Single, Mandatory, Integer)
01 = Available Network Bearer (Multiple, Mandatory, Integer)
02 = Radio Signal Strength (Single, Mandatory, Integer, dBm)
03 = Link Quality (Single, Optional, Integer)
04 = IP Addresses (Multiple, Mandatory, String)
05 = Router IP Address (Multiple, Optional, String)
06 = Link Utilization (Single, Optional, Integer, 1-100%)
07 = APN (Multiple, Optional, String)
08 = Cell ID (Single, Optional, Integer)
09 = SMNC (Single, Optional, Integer, 0-999)
10 = SMCC (Single, Optional, Integer, 0-999)
```
A description of the values per resource ID is provided [here](./LWM2M-Connectivity-Monitoring-Object.md)

Not providing `<client_endpoint_name>` returns a json array with names of all connected LHCB Clients, for example:
```
[
  "1605099547",
  "alice",
  "358593926"
]
```

Not providing `<resource_id>` returns all available information for an endpoint, for example:
```
{
  "0": {
    "id": 0,
    "value": 41,
    "type": "INTEGER"
  },
  "1": {
    "id": 1,
    "values": {
      "0": 41
    },
    "type": "INTEGER"
  },
  "4": {
    "id": 4,
    "values": {
      "0": "172.17.0.1",
      "1": "fe80:0:0:0:225:64ff:fe8c:57a%eth0",
    },
    "type": "STRING"
  },
  "5": {
    "id": 5,
    "values": {
      "0": "192.168.1.1"
    },
    "type": "STRING"
  }
}
```
As you can see, the Broker returns a map with the resource ID as keys. The resource itself contains either a "value" or "values" entry, depending on whether or not the requested resource is a single value or a list of values.

Providing endpoint name and resource ID, the HTTP interface returns the object of the resource. So if we take the above information as base, and request the resource ID "4", the HTTP interface returns:
```
{
  "id": 4,
  "values": {
    "0": "172.17.0.1",
    "1": "fe80:0:0:0:225:64ff:fe8c:57a%eth0",
  },
  "type": "STRING"
}
```

## Using the WebSocket interface
The WebSocket interface is currently being developed for the Broker. [Please take a look at draft for the message schema that will be used.](./WebSocketMessageSchema.json)
