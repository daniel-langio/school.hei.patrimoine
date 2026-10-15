#!/bin/bash
# Usage: run.sh [-d <patrimoine dir>] [-v <major.minor>]   (defaults: wrapper.conf, latest version)
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
. "$here/wrapper.conf"
while getopts "d:v:" o; do
  case $o in d) PATRIMOINE_HOME=$OPTARG ;; v) version=$OPTARG ;; *) exit 1 ;; esac
done

version=${version:-$(ls "$here/versions" | sed -n 's/^patrimoine-\([0-9]*\.[0-9]*\)\.jar$/\1/p' | sort -t. -k1,1n -k2,2n | tail -1)}
jar="$here/versions/patrimoine-$version.jar"
[ -f "$jar" ] || { echo "No jar found (version '${version:-none}'). Run wrapper/build.sh first."; exit 1; }
[ -d "$PATRIMOINE_HOME/download/realises" ] || { echo "$PATRIMOINE_HOME not initialized. Run wrapper/init.sh -d $PATRIMOINE_HOME"; exit 1; }

echo "Launching patrimoine-$version on $PATRIMOINE_HOME"
# shellcheck disable=SC2086
exec java -Dpatrimoine.home="$PATRIMOINE_HOME" $JAVA_OPTS -jar "$jar"
