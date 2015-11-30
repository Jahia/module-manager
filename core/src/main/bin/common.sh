#!/usr/bin/env bash
DF_SERVER_BASE_URL=http://localhost:8080
DF_USERNAME=root
DF_PASSWORD=root1234
DF_MODULE_MANAGER_REST_PREFIX=modules/api/cluster/v1
WAIT_INTERVAL=1
WAIT_TRIES=30
CURL_OPTIONS="-s --user $DF_USERNAME:$DF_PASSWORD"

function waitForTransactions() {
TRANSACTIONS_OUTPUT=
TRIES=0
echo -n "Waiting for transaction to end."
while [[ $TRANSACTIONS_OUTPUT != *",\"open\":false"* ]] && [ $TRIES -lt $WAIT_TRIES ]; do
    #now let's retrieve the transaction list
    TRANSACTIONS_OUTPUT=`curl $CURL_OPTIONS $DF_SERVER_BASE_URL/modules/api/cluster/v1/transaction`
    # echo $TRANSACTIONS_OUTPUT tries=$TRIES
    echo -n "."
    sleep $WAIT_INTERVAL
    let TRIES=TRIES+1
done
if [ "$TRIES" -eq "$WAIT_TRIES" ]; then
echo Reached maximum number of wait tries, transaction did not complete successfully after elapsed time!
else
echo Transaction completed successfully.
fi
}