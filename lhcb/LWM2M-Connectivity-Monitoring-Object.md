# LWM2M Connectivity Monitoring Object

## Introduction

"The goal of the Connectivity Monitoring Object is to carry information reflecting the more up to date values of the current connection for monitoring purposes. Resources such as Link Quality, Radio Signal Strenght, Cell ID are retrieved during connected mode at least for cellular networks." [OMA-TS-LighweitghtM2M-V1.0]

## Object definition

* Name: Connectivity Monitoring
* Object ID: 4
* Mandatory: Optional
* Object URN: urn:oma:lwm2m:oma:4

## Resource definition

### 00 = Network Bearer (Single, Mandatory, Integer)

Indicates the network bearer used for the current LWM2M communication session from the below network bearer list.
0-20 are Cellular Bearers:
  * 0: GSM cellular network
  * 1: TD-SCDMA cellular network 2: WCDMA cellular network
  * 3: CDMA2000 cellular network 4: WiMAX cellular network
  * 5: LTE-TDD cellular network
  6: LTE-FDD cellular network
  * 7-20: Reserved for other type cellular network
  * 21-40 are Wireless Bearers
  * 21: WLAN network
  * 22: Bluetooth network
  * 23: IEEE 802.15.4 network
  * 24-40: Reserved for other type local wireless network
  * 41-50 are Wireline Bearers 41: Ethernet
  * 42: DSL
  * 43: PLC
  * 44-50: reserved for others type wireline networks.

### 01 = Available Network Bearer (Multiple, Mandatory, Integer)

Indicates list of current available network bearer. Each Resource Instance has a value from the network bearer list.

### 02 = Radio Signal Strength (Single, Mandatory, Integer, dBm)

This node contains the average value of the received signal strength indication used in the current network bearer in case Network Bearer Resource indicates a Cellular Network (RXLEV range 0...64) 0 is < 110dBm, 64 is >-48 dBm).
Refer to [3GPP 44.018] for more details on Network Measurement Report encoding and [3GPP 45.008] or for Wireless Networks refer to the appropriate wireless standard.

### 03 = Link Quality (Single, Optional, Integer)

This contains received link quality e.g., LQI for IEEE 802.15.4, (Range (0..255)), RxQual Downlink (for GSM range is 0...7).
Refer to [3GPP 44.018] for more details on Network Measurement Report encoding.

### 04 = IP Addresses (Multiple, Mandatory, String)

The IP addresses assigned to the connectivity interface. (e.g. IPv4, IPv6, etc.)

### 05 = Router IP Address (Multiple, Optional, String)

The IP address of the next-hop IP router.

Note: This IP Address doesn’t indicate the Server IP address.

### 06 = Link Utilization (Single, Optional, Integer, 1-100\%)

The average utilization of the link to the next-hop IP router in \%.

### 07 = APN (Multiple, Optional, String)

Access Point Name in case Network Bearer Resource is a Cellular Network.

### 08 = Cell ID (Single, Optional, Integer)

Serving Cell ID in case Network Bearer Resource is a Cellular Network.

As specified in TS [3GPP 23.003] and in [3GPP. 24.008]. Range (0...65535) in GSM/EDGE
UTRAN Cell ID has a length of 28 bits.

Cell Identity in WCDMA/TD-SCDMA. Range: (0..268435455).

LTE Cell ID has a length of 28 bits. Parameter definitions in [3GPP 25.331].

### 09 = SMNC (Single, Optional, Integer, 0-999)

Serving Mobile Network Code. In case Network Bearer Resource has 0(cellular network). Range (0...999).

As specified in TS [3GPP 23.003].

### 10 = SMCC (Single, Optional, Integer, 0-999)

Serving Mobile Country Code. In case Network Bearer Resource has 0 (cellular network). Range (0...999).

As specified in TS [3GPP 23.003].
