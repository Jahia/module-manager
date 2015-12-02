#!/usr/bin/env bash
# example script to deploy a bundle using the manager module's REST API
source ./common.sh
#deploy bundle REST call
echo Deploying bundle file $1...
DEPLOY_OUTPUT=`curl $CURL_OPTIONS --form bundleFile=@$1 $DX_SERVER_BASE_URL/$DX_MM_REST_PREFIX/bundles`
echo "Deploy result=$DEPLOY_OUTPUT"
