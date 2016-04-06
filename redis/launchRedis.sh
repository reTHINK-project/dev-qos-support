#!/bin/bash

#configure and launch TURN Server

usage ()
{
  echo 'Usage : launchRedis.sh '
  exit
}

echo $# $1 $2 $3 $4 $5 $6 $7 $8 $9 $10

if [ "$#" -lt 0 ]
then
  usage
fi

while [ "$1" != "" ]; do
case $1 in
        -dbP )        shift
                       REDIS_PORT=$1
                       ;;
    esac
    shift
done

service redis_6379 restart
tail -f 

