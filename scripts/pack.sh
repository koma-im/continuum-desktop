#!/bin/bash
set -ev
if [ "${TRAVIS_OS_NAME}" = "linux" ]; then
  ./gradlew jar
  mv build/libs/*-without-dependencies.jar ./deploy/$APPNAME-$TRAVIS_TAG-without-dependencies.jar
fi
