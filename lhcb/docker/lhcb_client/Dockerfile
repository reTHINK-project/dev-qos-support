FROM lhcb_base

MAINTAINER robert.ende@fokus.fraunhofer.de

RUN cd /opt/reTHINK/lhcb/lhcb_client && mvn install

RUN cd /opt/reTHINK/lhcb/lhcb_client/target && mv rethink-lhcb-client-*-jar-with-dependencies.jar rethink-lhcb-client-jar-with-dependencies.jar

# ENTRYPOINT is the command that is always run.
ENTRYPOINT ["java", "-jar", "/opt/reTHINK/lhcb/lhcb_client/target/rethink-lhcb-client-jar-with-dependencies.jar"]