#!/bin/bash
#gulp dist
#gulp dist
screen -dmS aa1 gulp start --address 127.0.0.1 --port 10000 --type access
sleep 1
screen -dmS aa2 gulp start --address 127.0.0.1 --port 10001 --type access
sleep 1
screen -dmS aa3 gulp start --address 127.0.0.1 --port 10002 --type access
sleep 1
screen -dmS aa4 gulp start --address 127.0.0.1 --port 10003 --type access
sleep 1
screen -dmS aa5 gulp start --address 127.0.0.1 --port 10004 --type access

sleep 1
screen -dmS at1 gulp start --address 127.0.0.1 --port 20000 --type turn
sleep 1
screen -dmS at2 gulp start --address 127.0.0.1 --port 20001 --type turn
sleep 1
screen -dmS at3 gulp start --address 127.0.0.1 --port 20002 --type turn
sleep 1
screen -dmS at4 gulp start --address 127.0.0.1 --port 20003 --type turn
sleep 1
screen -dmS at5 gulp start --address 127.0.0.1 --port 20004 --type turn
sleep 1
screen -dmS at6 gulp start --address 127.0.0.1 --port 20005 --type turn
sleep 1
screen -dmS at7 gulp start --address 127.0.0.1 --port 20006 --type turn
sleep 1
screen -dmS at8 gulp start --address 127.0.0.1 --port 20007 --type turn
sleep 1
screen -dmS at9 gulp start --address 127.0.0.1 --port 20008 --type turn
