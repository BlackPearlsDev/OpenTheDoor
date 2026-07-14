#!/usr/bin/env sh
set -e
mkdir -p build
javac -encoding UTF-8 -cp "libs/netty-all-4.1.68.Final.jar" -d build \
  src/openthedoor/Main.java \
  src/openthedoor/config/*.java \
  src/openthedoor/detect/*.java \
  src/openthedoor/log/*.java \
  src/openthedoor/util/*.java \
  src/openthedoor/scan/*.java \
  src/openthedoor/ui/*.java \
  src/openthedoor/net/tcp/*.java \
  src/openthedoor/net/http/*.java \
  src/openthedoor/net/proxy/*.java
if [ -d resources ]; then
  cp -R resources/. build/
fi
jar cfe OpenTheDoor.jar openthedoor.Main -C build .
echo "Built OpenTheDoor.jar"
