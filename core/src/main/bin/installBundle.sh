#!/usr/bin/env bash
# example script to deploy a bundle using the manager module's REST API
source ./common.sh
#deploy bundle REST call
echo Deploying bundle file $1...
DEPLOY_OUTPUT=`curl $CURL_OPTIONS --form bundleFile=@$1 $DX_REST_URL/_install`
echo "Deploy result=$DEPLOY_OUTPUT"
#check the operation status
OPERATION_UUID=`echo $DEPLOY_OUTPUT | python -m json.tool | sed -n -e '/"operationId":/ s/^.*"\(.*\)".*/\1/p'`
checkOperation $OPERATION_UUID
