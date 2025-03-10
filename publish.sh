#!/bin/sh

# Note: This script is called from jitpack.yml.
# The sbt version should be kept up to date with build.properties.

cd iris || exit

java -Xms2048m -Xmx2048m -XX:ReservedCodeCacheSize=512m -jar ../sbt-launch-1.10.6.jar -Dsbt.color=false -Dprod.publish=true clean publishM2

cd ../wfos || exit

java -Xms2048m -Xmx2048m -XX:ReservedCodeCacheSize=512m -jar ../sbt-launch-1.10.6.jar -Dsbt.color=false -Dprod.publish=true clean publishM2
