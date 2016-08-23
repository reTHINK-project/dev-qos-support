
#Installation of the Broker
The broker has been dockerised. It necessitates a redis database to work. Here is the procedure to launch the broker.

`git clone https://github.com/reTHINK-project/dev-qos-support.git`  
`cd dev-qos-support/broker/BrokerServer`  

At this stage you have to provide the good server certificates in order to work, with the FQDN of the server where it is installed (see [README](BrokerServer/sslkeys/README.md))  
`docker build -t rethink/broker .  `  
`docker run --name redis -p 6379:6379 -d redis  `  
`docker run -it --name broker -p 8080:8080 -p 8181:8181 --link redis:redis rethink/broker -dbIP <REDISIPADDR> -dbP <REDISIPPORT>`  

If nothing is changed in the configuration. The redis database runs on the localhost/127.0.0.1 and is able to provide the information via port 6379.
The dashboard is available via the given port 8181, if the configuration has not been changed.

To verify that it works, https://domain_name:port/dashboard
