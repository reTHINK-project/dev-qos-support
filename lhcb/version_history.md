# version history

## 0.1.0
* initial version
* provides information about used and available networks, IPs and gateways, and WiFi info (if iwconfig is installed)

## 0.2.0
* uses leshan verion 0.1.11-M12
* added LHCB Android Client
* better logging
* code optimization & documentation
* added ability to set client name
* added ability to provide dummy data

## 0.2.1
* added (optional) WebSocketServer to ConnectivityMonitor in order to request its current state via WebSocket
* added -websocket launch option to client
* fixed Runner Thread never stopping even though stopRunner() was called

## 0.2.2
* prints current version on start