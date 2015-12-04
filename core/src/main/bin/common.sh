#!/usr/bin/env bash
DX_REST_URL=http://localhost:8080/modules/api/bundles
DX_USERNAME=root
DX_PASSWORD=root
WAIT_INTERVAL=1
WAIT_TRIES=30
CURL_OPTIONS="-s --user $DX_USERNAME:$DX_PASSWORD"

# check the operation status in JCR
function checkOperation() {
OPERATION_OUTPUT=`curl $CURL_OPTIONS --request GET $DX_REST_URL/operation/$1/_state`
echo $OPERATION_OUTPUT;
}