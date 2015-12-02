#!/usr/bin/env bash
# example script to start a bundle using the module manager's REST API
source ./common.sh
#undeploy bundle REST call
echo Undeploying bundle $1...
UNDEPLOY_OUTPUT=`curl $CURL_OPTIONS --request POST $DF_SERVER_BASE_URL/$DF_MODULE_MANAGER_REST_PREFIX/bundles/$1/_uninstall`
echo "Undeploy result=$UNDEPLOY_OUTPUT"
if [[ $UNDEPLOY_OUTPUT == *"{\"successful\":true"* ]]
 then
waitForTransactions
fi