#!/bin/bash
# Build the jar and store it as wrapper/versions/patrimoine-<major>.<minor>.jar
# Minor auto-increments from the highest existing version. --major bumps major, resets minor to 0.
set -euo pipefail
cd "$(dirname "$0")/.."
V=wrapper/versions

last=$(ls "$V" 2>/dev/null | sed -n 's/^patrimoine-\([0-9]*\.[0-9]*\)\.jar$/\1/p' | sort -t. -k1,1n -k2,2n | tail -1)
major=${last%%.*}; minor=${last##*.}
if [ -z "$last" ]; then major=1; minor=0
elif [ "${1:-}" = "--major" ]; then major=$((major + 1)); minor=0
else minor=$((minor + 1)); fi

mvn -DskipTests clean package
cp target/patrimoine-1.0-SNAPSHOT-exec.jar "$V/patrimoine-$major.$minor.jar"
echo "Built $V/patrimoine-$major.$minor.jar"
