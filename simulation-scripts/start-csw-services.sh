#!/usr/bin/env bash

CSW_VERSION=9409307
cs launch csw-services:$CSW_VERSION -- start -e -c -k
