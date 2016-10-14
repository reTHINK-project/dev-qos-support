FROM lhcb_base

MAINTAINER robert.ende@fokus.fraunhofer.de

#RUN apt-get install git maven default-jre default-jdk

RUN cd /opt/reTHINK/lhcb/lhcb_broker && mvn install

RUN cd /opt/reTHINK/lhcb/lhcb_broker/target && mv rethink-lhcb-broker-*-jar-with-dependencies.jar rethink-lhcb-broker-jar-with-dependencies.jar

# ENTRYPOINT is the command that is always run.
ENTRYPOINT ["java", "-jar", "/opt/reTHINK/lhcb/lhcb_broker/target/rethink-lhcb-broker-jar-with-dependencies.jar"]