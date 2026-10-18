#!/usr/bin/env bash
set -e
command -v java >/dev/null || { echo "Java 17 not found: https://adoptium.net"; exit 1; }
if [ ! -x gradle-8.8/bin/gradle ]; then
  curl -L -o gradle.zip https://services.gradle.org/distributions/gradle-8.8-bin.zip
  unzip -q gradle.zip && rm gradle.zip
fi
./gradle-8.8/bin/gradle build --no-daemon
echo "DONE: build/libs/megaworld-0.8.0.jar (not the -sources one)"
