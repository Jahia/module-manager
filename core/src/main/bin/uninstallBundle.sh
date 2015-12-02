#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#undeploy bundle REST call
echo Undeploying bundle $1...
UNDEPLOY_OUTPUT=`curl $CURL_OPTIONS --request POST $DX_SERVER_BASE_URL/$DX_MM_REST_PREFIX/bundles/$1/_uninstall`
echo "Undeploy result=$UNDEPLOY_OUTPUT"