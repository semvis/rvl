#!/bin/bash

export MAVEN_OPTS="-Xmx2048M"

cd ../rvl-server/

# the tracing parameter may not yet work as desired
mvn -U exec:java  -Djersey.config.server.tracing.type=ALL
