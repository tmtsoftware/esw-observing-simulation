#!/bin/sh

# Note; Jenkins was getting "address in use" for Redis when testing this project.
# This script tries to shutdown any redis server.

set -x
#redis-cli shutdown || echo "XXX: Redis was not running"
#kill `lsof -t -i:6379`
#kill `lsof -t -i:26379`
#exit 0

ps -efl | grep 6379 | grep -v grep | awk '{ print $4 }' | xargs kill
ps -efl | grep 26379 | grep -v grep | awk '{ print $4 }' | xargs kill
ps auwx | grep redis | grep -v grep
