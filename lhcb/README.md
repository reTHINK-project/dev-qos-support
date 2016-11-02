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
        Indicates the network bearer used for the
        current LWM2M communication session
        from the below network bearer list.

        0 ~20 are Cellular Bearers
        00:GSM cellular network

        01:TD - SCDMA cellular network
        02:WCDMA cellular network
        03:CDMA2000 cellular network
        04:WiMAX cellular network
        05:LTE - TDD cellular network
        06:LTE - FDD cellular network

        07~20: Reserved for other type cellular network
        21~40: Wireless Bearers

        21:WLAN network
        22:Bluetooth network
        23:IEEE 802.15 .4 network

        24~40: Reserved for other type local wireless network
        41~50: Wireline Bearers

        41:Ethernet
        42:DSL
        43:PLC

        44 ~50:reserved for others type wireline networks.

01 = Available Network Bearer (Multiple, Mandatory, Integer)
        Indicates list of current available network bearer.
        Each Resource Instance has a value from the network bearer list.

02 = Radio Signal Strength (Single, Mandatory, Integer, dBm)
        This node contains the average value of
        the received signal strength indication used
        in the current network bearer in case Network Bearer Resource indicates a
        Cellular Network (RXLEV range 0.. .64)0 is < 110dBm, 64 is > -48 dBm).
        Refer to[ 3 GPP 44.018]for more details on
        Network Measurement Report encoding
        and[3 GPP 45.008]or for Wireless
        Networks refer to the appropriate wireless
        standard.

03 = Link Quality (Single, Optional, Integer)
        This contains received link quality e.g., LQI
        for IEEE 802.15 .4, (Range(0...255)),
        RxQual Downlink ( for GSM range is 0...7).
        Refer to[ 3 GPP 44.018]for more details on
        Network Measurement Report encoding.

04 = IP Addresses (Multiple, Mandatory, String)
        The IP addresses assigned to the
        connectivity interface.(e.g.IPv4, IPv6, etc.)

05 = Router IP Address (Multiple, Optional, String)
        The IP address of the next-hop IP router.
        Note: This IP Address doesn’t indicate the
        Server IP address.

06 = Link Utilization (Single, Optional, Integer, 1-100%)
        The average utilization of the link to the
        next-hop IP router in %.

07 = APN (Multiple, Optional, String)
        Access Point Name in case Network
        Bearer Resource is a Cellular Network.

08 = Cell ID (Single, Optional, Integer)
        Serving Cell ID in case Network Bearer
        Resource is a Cellular Network.

        As specified in TS [3GPP 23.003] and in
        [3GPP. 24.008]. Range (0...65535) in
        GSM/EDGE

        UTRAN Cell ID has a length of 28 bits.
        Cell Identity in WCDMA/TD-SCDMA.
        Range: (0..268435455).

        LTE Cell ID has a length of 28 bits.

        Parameter definitions in [3GPP 25.331].

09 = SMNC (Single, Optional, Integer, 0-999)
        Serving Mobile Network Code. In case
        Network Bearer Resource has 0(cellular
        network). Range (0...999).
        As specified in TS [3GPP 23.003].

10 = SMCC (Single, Optional, Integer, 0-999)
        Serving Mobile Country Code. In case
        Network Bearer Resource has 0 (cellular
        network). Range (0...999).
        As specified in TS [3GPP 23.003].
```
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