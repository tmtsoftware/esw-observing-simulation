#!/bin/sh

# Note; Jenkins was getting "address in use" for Redis when testing this project.
# This script tries to shutdown any redis server.

set -x
redis-cli shutdown || echo "XXX: Redis was not running"
kill `lsof -t -i:6379`
kill `lsof -t -i:26379`
exit 0
