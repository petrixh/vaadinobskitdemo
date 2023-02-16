#!/bin/sh

# What version of the agent to use, and where to download it from if not already downloaded.
# Ensure the version in the download URL also matches the version of the agent....
AGENT_JAR=vaadin-opentelemetry-javaagent-1.0.0.jar
AGENT_DOWNLOAD_PATH=https://repo1.maven.org/maven2/com/vaadin/observability/vaadin-opentelemetry-javaagent/1.0.0

cd ../target
echo "Checking for agent JAR and downloading if necessary"
if [ -f "$AGENT_JAR" ]; then
  echo "Agent JAR already downloaded..."
else
  wget "$AGENT_DOWNLOAD_PATH"/"$AGENT_JAR"
fi
cd ..
cd observability-kit


