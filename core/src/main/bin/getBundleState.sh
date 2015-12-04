#!/usr/bin/env bash
# example script to get a bundle state on a list of nodes using the module manager's REST API
source ./common.sh
#get bundle state REST call
echo Getting state for  bundle $1...
STATE_OUTPUT=`curl $CURL_OPTIONS --request GET $DX_REST_URL/$1/_state`
echo "State result=$STATE_OUTPUT"