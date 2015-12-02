#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#start bundle REST call
echo Starting bundle $1...
STATES_OUTPUT=`curl $CURL_OPTIONS --request GET $DX_SERVER_BASE_URL/$DX_MM_REST_PREFIX/_states`
echo "Start result=$STATES_OUTPUT"
