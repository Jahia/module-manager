#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#undeploy bundle REST call
echo Uninstalling bundle $1...
UNINSTALL_OUTPUT=`curl $CURL_OPTIONS --data '$2' --request POST $DX_REST_URL/$1/_uninstall`
echo "Uninstall result=$UNINSTALL_OUTPUT"