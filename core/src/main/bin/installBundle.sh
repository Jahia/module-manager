#!/usr/bin/env bash
# example script to deploy a bundle using the manager module's REST API
source ./common.sh
#deploy bundle REST call
echo Installing bundle file $1...
INSTALL_OUTPUT=`curl $CURL_OPTIONS --form bundle=@$1 --form target=$2 --form start=$3 $DX_REST_URL/`
echo "Install result=$INSTALL_OUTPUT"
