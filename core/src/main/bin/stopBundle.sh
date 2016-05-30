#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#start bundle REST call
echo Stopping bundle $1...
DATA="target=$2";
STOP_OUTPUT=`curl $CURL_OPTIONS --data $DATA --request POST $DX_REST_URL/$1/_stop`
echo "Stop result=$STOP_OUTPUT"