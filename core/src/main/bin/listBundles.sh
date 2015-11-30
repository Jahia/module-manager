#!/usr/bin/env bash
# example script to list deployed bundles using the module manager's REST API
source ./common.sh
#list bundles REST call
echo Listing bundles deployed on Digital Factory server...
curl $CURL_OPTIONS $DF_SERVER_BASE_URL/$DF_MODULE_MANAGER_REST_PREFIX/bundles | python -m json.tool
