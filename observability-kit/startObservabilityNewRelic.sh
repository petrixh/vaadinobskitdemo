#!/bin/sh
#TODO move files and paths to variables for better maintenance...
AGENT_JAR_PATH=./target/
AGENT_JAR=vaadin-opentelemetry-javaagent-1.0.0.rc1.jar

##Exit hook for cleanup...
onExit(){
  echo "Exit hook running..."
}

##Bring down containers if there is an error...
trap 'onExit' EXIT
##Bring down containers on Ctrl + c
trap 'onExit' 2

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
java -Xmx3G -javaagent:./target/vaadin-opentelemetry-javaagent-1.0.0.rc1.jar      -Dotel.javaagent.configuration-file=./observability-kit/agent-configs/agent-new-relic.properties  -jar ./target/kitstest-1.0-SNAPSHOT.jar

