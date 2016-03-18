 
#Installation of the Broker
The broker has been dockerised. It necessitates a redis database to work. Here is the procedure to launch the broker.

`git clone https://github.com/reTHINK-project/dev-qos-support.git`  
`cd dev-qos-support/broker/BrokerServer`  

At this stage you have to provide the good server certificates in order to work, with the FQDN of the server where it is installed (see [README](BrokerServer/sslkeys/README.md))  
`docker build -t rethink/broker .  `  
`docker run --name redis -p 6379:6379 -d redis  `  
`docker run --name broker -p 8080:8080 --link redis:redis -d rethink/broker`  

To verify that it works, https://domain_name:port/dashboard.html  
