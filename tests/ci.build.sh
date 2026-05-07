#!/bin/bash
source ./set-env.sh

if [ ! -d ./artifacts ]; then
  mkdir -p ./artifacts
fi

if [[ -e ../core/target ]]; then
  cp ../core/target/*-SNAPSHOT.jar ./artifacts/
fi

version=$(node -p "require('./package.json').devDependencies['@jahia/cypress']")
echo Using @jahia/cypress@$version...
npx --yes --package @jahia/cypress@$version ci.build
