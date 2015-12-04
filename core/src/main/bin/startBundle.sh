#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#start bundle REST call
echo Starting bundle $1...
START_OUTPUT=`curl $CURL_OPTIONS --data '$2' --request POST $DX_REST_URL/$1/_start`
echo "Start result=$START_OUTPUT"
#check the operation status
OPERATION_UUID=`echo $START_OUTPUT | python -m json.tool | sed -n -e '/"operationId":/ s/^.*"\(.*\)".*/\1/p'`
checkOperation $OPERATION_UUID