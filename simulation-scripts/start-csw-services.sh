#!/usr/bin/env bash

CSW_VERSION=fd5bfc4
cs launch csw-services:$CSW_VERSION -- start -e -c -k
