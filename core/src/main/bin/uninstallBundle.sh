#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#undeploy bundle REST call
echo Undeploying bundle $1...
UNDEPLOY_OUTPUT=`curl $CURL_OPTIONS --data '$2' --request POST $DX_REST_URL/$1/_uninstall`
echo "Undeploy result=$UNDEPLOY_OUTPUT"