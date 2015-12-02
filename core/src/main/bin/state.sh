#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#start bundle REST call
echo Getting state for  bundle $1...
STATE_OUTPUT=`curl $CURL_OPTIONS --request POST $DF_SERVER_BASE_URL/$DF_MODULE_MANAGER_REST_PREFIX/$1/_state`
echo "Start result=STATE_OUTPUT"
if [[ STATE_OUTPUT == *"{\"successful\":true"* ]]
 then
waitForTransactions
fi
