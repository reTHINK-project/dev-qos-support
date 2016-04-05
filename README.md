# dev qos support

This is the reThink QoS dedicaded development folder.  

##installation of redis database (mandatory for Broker and TURN usage).

From the redis folder:
docker build -t rethink/redis .
docker run -d -it --name="redis" rethink/redis 

##installation coTURN server
From the broker/coturn folder:
docker build -t rethink/coturn .
docker run -d -it --name="coturn" rethink/coturn -dbIP IP-REDIS -realm REALM




