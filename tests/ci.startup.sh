#!/bin/bash

source ./set-env.sh

echo " == Printing the most important environment variables"
echo " MANIFEST: ${MANIFEST}"
echo " NEXUS_USERNAME: ${NEXUS_USERNAME}"
echo " TESTS_IMAGE: ${TESTS_IMAGE}"
echo " JAHIA_IMAGE: ${JAHIA_IMAGE}"
echo " JAHIA_CLUSTER_ENABLED: ${JAHIA_CLUSTER_ENABLED}"
echo " MODULE_ID: ${MODULE_ID}"
echo " JAHIA_URL: ${JAHIA_URL}"
echo " SUPER_USER_PASSWORD: ${SUPER_USER_PASSWORD}"

version=$(node -p "require('./package.json').devDependencies['@jahia/cypress']")
echo Using @jahia/cypress@$version...
npx --yes --package @jahia/cypress@$version ci.startup
