#!/bin/sh

##Exit hook for cleanup...
onExit(){
  echo "Exit hook running..."
}

##Bring down containers if there is an error...
trap 'onExit' EXIT
##Bring down containers on Ctrl + c
trap 'onExit' 2

echo "App on port 8080, new relic on https://one.eu.newrelic.com/"
echo "Starting demo app in 3 seconds..."
sleep 3s
java -javaagent:./observability-kit/agent/vaadin-opentelemetry-javaagent-1.0.0.rc1.jar      -Dotel.javaagent.configuration-file=./observability-kit/agent-configs/agent-new-relic.properties  -jar ./target/kitstest-1.0-SNAPSHOT.jar

