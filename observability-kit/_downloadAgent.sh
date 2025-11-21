#!/bin/sh

# What version of the agent to use, and where to download it from if not already downloaded.
# Ensure the version in the download URL also matches the version of the agent....
AGENT_JAR=observability-kit-agent-3.1.0.jar
AGENT_DOWNLOAD_PATH=https://repo1.maven.org/maven2/com/vaadin/observability-kit-agent/3.1.0

cd ../target
echo "Checking for agent JAR and downloading if necessary"
if [ -f "$AGENT_JAR" ]; then
  echo "Agent JAR already downloaded..."
else
  wget "$AGENT_DOWNLOAD_PATH"/"$AGENT_JAR"
fi
echo "Observability kit agent downloaded: "
pwd 
ls 
cd ..
cd observability-kit


