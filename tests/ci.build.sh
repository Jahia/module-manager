#!/bin/bash
# This script can be used to manually build the docker images necessary to run the tests
# It should be executed from the tests folder

echo " ci.build.sh == Build test image"

source ./set-env.sh

# It assumes that you previously built the module you're going to be testing
#   and that the module artifact is located in ../core/target

if [ ! -d ./artifacts ]; then
  mkdir -p ./artifacts
fi

if [[ -e ../core/target ]]; then
  cp ../core/target/module-manager-*-SNAPSHOT.jar ./artifacts/
fi

docker build -t ${TESTS_IMAGE} .
