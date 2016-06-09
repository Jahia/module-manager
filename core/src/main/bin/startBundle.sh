#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
DX_DISPLAY_USAGE="Script to start a bundle using the module manager's REST API.\n
\nUsage:\n$0 bundleName\n"
source ./common.sh

#start bundle REST call
echo Starting bundle $1...
DATA="target=$2";
START_OUTPUT=`curl $CURL_OPTIONS --data DATA --request POST $DX_REST_URL/$1/_start`
echo "Start result=$START_OUTPUT"
