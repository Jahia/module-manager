#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#start bundle REST call
echo Stopping bundle $1...
STOP_OUTPUT=`curl $CURL_OPTIONS --data '$2' --request POST $DX_REST_URL/$1/_stop`
echo "Stop result=$STOP_OUTPUT"
#check the operation status
#OPERATION_UUID=`echo $STOP_OUTPUT | python -m json.tool | sed -n -e '/"operationId":/ s/^.*"\(.*\)".*/\1/p'`
#checkOperation $OPERATION_UUID