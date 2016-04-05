FROM ubuntu:14.04
MAINTAINER simon.becot@orange.com

RUN apt-get update -y && apt-get install gcc openssl libssl-dev make wget git -y

WORKDIR /usr/local/src

RUN git clone https://github.com/redis/hiredis.git
RUN cd hiredis && make && make install

RUN wget https://github.com/libevent/libevent/releases/download/release-2.0.22-stable/libevent-2.0.22-stable.tar.gz
RUN tar -xzvf libevent-2.0.22-stable.tar.gz
RUN cd libevent-2.0.22-stable && ./configure && make && make install
RUN apt-get install libevent-dev -y

RUN mkdir /usr/local/coturn
WORKDIR /usr/local/coturn
COPY . /usr/local/coturn
RUN chmod a+x configure
RUN ./configure && make && make install
RUN cp -p /usr/local/coturn/examples/etc/turnserver-rethink.conf /usr/local/etc/turnserver.conf


ENTRYPOINT ["./launchTurn.sh"]


