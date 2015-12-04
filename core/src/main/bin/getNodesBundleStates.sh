#!/usr/bin/env bash
# example script to get a list of node report bundles state using the module manager's REST API
source ./common.sh
#start bundle REST call
STATES_OUTPUT=`curl $CURL_OPTIONS --request GET $DX_REST_URL/_states`
echo "States result=$STATES_OUTPUT"
