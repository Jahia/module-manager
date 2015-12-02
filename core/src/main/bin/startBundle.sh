#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#start bundle REST call
echo Starting bundle $1...
START_OUTPUT=`curl $CURL_OPTIONS --request POST $DX_SERVER_BASE_URL/$DX_MM_REST_PREFIX/$1/_start`
echo "Start result=$START_OUTPUT"
