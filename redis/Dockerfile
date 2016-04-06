FROM ubuntu:14.04
MAINTAINER simon.becot@orange.com

RUN apt-get update -y && apt-get install gcc openssl libssl-dev make wget -y

WORKDIR /usr/local/src

RUN wget http://download.redis.io/releases/redis-3.0.7.tar.gz
RUN tar -xzvf redis-3.0.7.tar.gz
WORKDIR /usr/local/src/redis-3.0.7
RUN cd /usr/local/src/redis-3.0.7/deps && make hiredis lua jemalloc linenoise
RUN make && make install && ./utils/install_server.sh


COPY redis-rethink.conf /usr/local/src/redis-3.0.7/redis.conf
COPY launchRedis.sh /usr/local/src/redis-3.0.7/


ENTRYPOINT ./launchRedis.sh

EXPOSE 6379 
