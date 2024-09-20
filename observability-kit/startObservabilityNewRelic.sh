#!/bin/sh
#Variables
AGENT_JAR_PATH=./target
#APP_JAR - will be populated later...
#AGENT_JAR - will be populated later...

##Exit hook for cleanup...
onExit(){
  echo "Exit hook running..."
}

##Bring down containers if there is an error...
trap 'onExit' EXIT
##Bring down containers on Ctrl + c
trap 'onExit' 2

APP_JAR=$(ls ../target/kitstest*.jar)
echo "App jar detected under target/$APP_JAR"

echo 'Checking for agent jar, downloading if necessary...'
./_downloadAgent.sh

AGENT_JAR=$(ls ../target/observability-kit-agent*.jar)
echo "Vaadin Observability Kit Agent jar detected under target/$AGENT_JAR"


cd ../target
echo "Checking for agent JAR and downloading if necessary"
if [ -f "$AGENT_JAR" ]; then
  echo "Agent JAR already downloaded..."
else
  wget http://tools.vaadin.com/nexus/content/repositories/vaadin-prereleases/com/vaadin/observability/vaadin-opentelemetry-javaagent/1.0.0.rc1/vaadin-opentelemetry-javaagent-1.0.0.rc1.jar
fi
cd ..

echo "App on port 8080, new relic on https://one.eu.newrelic.com/"
echo "Starting demo app in 3 seconds..."
sleep 3s
java -Xmx3G -javaagent:"$AGENT_JAR_PATH"/"$AGENT_JAR"      -Dotel.javaagent.configuration-file=./observability-kit/agent-configs/agent-new-relic.properties  -jar ./target/"$APP_JAR"

