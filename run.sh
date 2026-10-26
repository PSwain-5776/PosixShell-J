#!/bin/sh
#
# Builds and runs the shell locally.

set -e # Exit early if any commands fail

(
  cd "$(dirname "$0")"
  mvn -q -B package -Ddir=/tmp/posix-shell-j-build
)

exec java --enable-native-access=ALL-UNNAMED --enable-preview -jar /tmp/posix-shell-j-build/posix-shell-j.jar "$@"
