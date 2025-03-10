#!/bin/sh

# Note; Jenkins was getting "address in use" for Redis when testing this project.
# This script tries to shutdown any redis server.

set -x
ps -efl | grep 6379 | grep -v grep | awk '{ print $4 }' | xargs kill
ps -efl | grep 26379 | grep -v grep | awk '{ print $4 }' | xargs kill
ps auwx | grep redis | grep -v grep
