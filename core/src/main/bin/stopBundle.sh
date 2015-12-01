#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#start bundle REST call
echo Stopping bundle $1...
STOP_OUTPUT=`curl $CURL_OPTIONS --request POST $DF_SERVER_BASE_URL/$DF_MODULE_MANAGER_REST_PREFIX/$1/_stop`
echo "Stop result=$STOP_OUTPUT"
if [[ $STOP_OUTPUT == *"{\"successful\":true"* ]]
 then
waitForTransactions
fi
