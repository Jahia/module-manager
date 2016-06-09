#!/usr/bin/env bash
# example script to uninstall a bundle using the module manager's REST API
DX_DISPLAY_USAGE="Script to uninstall a bundle using the module manager's REST API.\n
\nUsage:\n$0 bundleName\n"

source ./common.sh
#uninstall bundle REST call
echo Uninstalling bundle $1...
DATA="target=$2";
UNINSTALL_OUTPUT=`curl $CURL_OPTIONS --data DATA --request POST $DX_REST_URL/$1/_uninstall`
echo "Uninstall result=$UNINSTALL_OUTPUT"