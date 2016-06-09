#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
DX_DISPLAY_USAGE="Script to stop a bundle using the module manager's REST API.\n
\nUsage:\n$0 bundleName\n"


source ./common.sh
#start bundle REST call
echo Stopping bundle $1...
STOP_OUTPUT=`curl $CURL_OPTIONS --request POST $DX_REST_URL/$1/_stop`
echo "Stop result=$STOP_OUTPUT"