FROM lhcb_base_base

MAINTAINER robert.ende@fokus.fraunhofer.de

#RUN apt-get update -y && apt-get install -y git maven default-jdk

#RUN cd /opt/reTHINK && git clone https://github.com/reTHINK-project/dev-qos-support.git

RUN mkdir /opt/reTHINK/lhcb

COPY . /opt/reTHINK/lhcb

RUN cd /opt/reTHINK/lhcb && mvn install -T 1C