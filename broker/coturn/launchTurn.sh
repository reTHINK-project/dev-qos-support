#!/bin/bash

#configure and launch TURN Server

usage ()
{
  echo 'Usage : launchTurn.sh -dbIP <redisIP> -dbP <redisPort> -realm <realm>'
  exit
}

echo $# $1 $2 $3 $4 $5 $6 $7 $8 $9 $10

if [ "$#" -lt 1 ]
then
  usage
fi

while [ "$1" != "" ]; do
case $1 in
        -dbIP )        shift
                       REDIS_IP=$1
                       ;;
        -dbP )         shift
                       REDIS_PORT=$1
                       ;;
        -realm )       shift
                       REALM=$1
                       ;;
    esac
    shift
done

# extra validation suggested by @technosaurus
if [ "$REDIS_IP" = "" ]
then
    usage
fi
if [ "$REDIS_PORT" = "" ]
then
   REDIS_PORT=6379 
fi
if [ "$REALM" = "" ]
then
  usage
fi


echo configuring coturn to use REDIS database on IP $REDIS_IP and port $REDIS_PORT
sed -i "s/<redis_IP>/"$REDIS_IP"/" /usr/local/etc/turnserver.conf
sed -i "s/<redis_PORT>/"$REDIS_PORT"/" /usr/local/etc/turnserver.conf
sed -i "s/<realm>/"$REALM"/" /usr/local/etc/turnserver.conf

/usr/local/coturn/bin/turnserver

