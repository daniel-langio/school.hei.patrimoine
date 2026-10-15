#!/bin/bash
# Usage: init.sh [-d <patrimoine dir>]   Creates the minimal folder structure (non destructive).
set -euo pipefail
. "$(dirname "$0")/wrapper.conf"
while getopts "d:" o; do
  case $o in d) PATRIMOINE_HOME=$OPTARG ;; *) exit 1 ;; esac
done
for s in planifies realises justificatifs; do mkdir -p "$PATRIMOINE_HOME/download/$s"; done
mkdir -p "$PATRIMOINE_HOME"/{cache,staged,compile,drive,google/tokens}
echo "Initialized $PATRIMOINE_HOME (put google/client.json and your .cas.md files in it)"
