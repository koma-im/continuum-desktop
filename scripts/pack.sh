#!/bin/bash
set -ev

if [ "${TRAVIS_OS_NAME}" = "linux" ]; then
  mkdir -p bin
  cd bin
  if [ ! -f linuxdeploy-x86_64.AppImage ]; then
    wget https://github.com/linuxdeploy/linuxdeploy/releases/download/continuous/linuxdeploy-x86_64.AppImage
  fi
  chmod +x linuxdeploy-x86_64.AppImage
  cd ..
  gradle runtime
  mkdir -p build/image/usr/bin
  cd build/image/usr/bin
  ln -sb ../../bin/* .
  cd -
  env VERSION="$TRAVIS_TAG" ./bin/linuxdeploy-x86_64.AppImage \
    --appdir build/image \
    -i src/main/resources/icon/koma.png \
    -d scripts/continuum.desktop \
    --output appimage
  mv Continuum*.AppImage ./deploy/"$APPNAME"-"$TRAVIS_TAG"-linux.AppImage
fi

