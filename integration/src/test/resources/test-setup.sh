#!/usr/bin/env bash

ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"

cd $ROOT/../../../../simulation-scripts/

sh ./start-tcs-assemblies.sh
