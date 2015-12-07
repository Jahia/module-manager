#!/usr/bin/env bash
DX_REST_URL=http://localhost:8080/modules/api/bundles
DX_USERNAME=root
DX_PASSWORD=root
WAIT_INTERVAL=1
WAIT_TRIES=30
CURL_OPTIONS="-s --user $DX_USERNAME:$DX_PASSWORD"

# check the operation status in JCR
function checkOperation() {
if [ $1 != "operationId" ]; then
TRANSACTIONS_OUTPUT=
TRIES=0
echo "Waiting for transaction to end."

while [[ $TRANSACTIONS_OUTPUT != *",\"completed\":true"* ]] && [ $TRIES -lt $WAIT_TRIES ]; do
    #now let's retrieve the transaction list
    let TRIES=TRIES+1
    TRANSACTIONS_OUTPUT=`curl $CURL_OPTIONS --request GET $DX_REST_URL/operation/$1/_state`
    echo Operation $TRANSACTIONS_OUTPUT tries=$TRIES
    sleep $WAIT_INTERVAL
done
if [ "$TRIES" -eq "$WAIT_TRIES" ]; then
echo Reached maximum number of wait tries, transaction did not complete successfully after elapsed time!
else
echo Transaction completed successfully after $TRIES trie"(s)"
fi
else
echo Cannot get the operation Id
fi
}