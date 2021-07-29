#!/bin/bash
# This script can be used to manually build the docker images necessary to run the tests
# It should be executed from the tests folder

# It assumes that you previously built the module you're going to be testing
#   and that the modules artifacts are located one level up

# Copy the root POM
if [[ -e ../pom.xml ]]; then
	cp ../pom.xml ./pom.xml
fi

# Copy the Maven settings
if [ ! -d ./.circleci ]; then
	mkdir ./.circleci
fi
cp -R ../.circleci/.circleci.settings.xml ./.circleci/.circleci.settings.xml

# Copy the artifacts previously build locally by the CI tool
if [ ! -d ./artifacts ]; then
    mkdir -p ./artifacts
fi

if [ ! -d ./core ]; then
	mkdir ./core
fi
cp -R ../core/{.[!.],..?,}* ./core/
cp -R ./core/target/* ./artifacts/
cp ./artifacts/module-manager*SNAPSHOT.jar ./artifacts/module-manager-SNAPSHOT.jar

if [ ! -d ./test ]; then
	mkdir ./test
fi
cp -R ../test/* ./test/
cp -R ./test/target/* ./artifacts/
cp ./artifacts/module-manager-test*SNAPSHOT.jar ./artifacts/module-manager-test-SNAPSHOT.jar


docker build -t jahia/module-manager:latest .