#!/usr/bin/env bash
# example script to install a bundle using the manager module's REST API

DX_DISPLAY_USAGE="Script to  if the  a bundle using the module manager's REST API.\n
if the second parameter is set to true, the bundle is started\n
\nUsage:\n$0 bundleName  [ true ]\n"

source ./common.sh

#deploy bundle REST call
echo Installing bundle file $1...
INSTALL_OUTPUT=`curl $CURL_OPTIONS --form bundle=@$1 --form start=$2 $DX_REST_URL/`
echo "Install result=$INSTALL_OUTPUT"
