# dev qos support

This is the reThink QoS dedicaded development folder.   
The different elements have been dockerised. We need first to launch install redis database, then broker, and coturn server.

## Download sources to build the packages.
`git clone https://github.com/reTHINK-project/dev-qos-support.git`  

##installation of redis database (mandatory for Broker and TURN usage).  
`cd dev-qos-support/redis`  

From the redis folder:  
`docker build -t rethink/redis .`  
`docker run -d -it --name="redis" rethink/redis`   

##Installation of the Broker

`cd ../broker/BrokerServer`  

At this stage you have to provide the good server certificates in order to work, with the FQDN of the server where it is installed (see [README](broker/BrokerServer/sslkeys/README.md))  
`docker build -t rethink/broker .  `  
`docker run --name broker -d rethink/broker -dbIP IP-REDIS  -dbP PORT-REDIS `  

To verify that it works, https://domain_name:port/dashboard.html  

##installation coTURN server  
`cd ../coturn`  
From the broker/coturn folder:  
`docker build -t rethink/coturn .`   
`docker run -d -it --name="coturn" rethink/coturn -dbIP IP-REDIS -realm REALM`  




