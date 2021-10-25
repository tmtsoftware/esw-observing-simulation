#!/usr/bin/env bash

cd iris || exit

java -Xms2048m -Xmx2048m -XX:ReservedCodeCacheSize=512m -jar ../sbt-launch-1.5.5.jar -Dsbt.log.noformat=true -Dprod.publish=true clean publishM2

cd ../wfos || exit

java -Xms2048m -Xmx2048m -XX:ReservedCodeCacheSize=512m -jar ../sbt-launch-1.5.5.jar -Dsbt.log.noformat=true -Dprod.publish=true clean publishM2