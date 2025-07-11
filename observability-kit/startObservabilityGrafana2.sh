#!/bin/sh
#Variables
AGENT_JAR_PATH=./target
AGENT_CONFIG_FILE=./observability-kit/agent-configs/agent-grafana.properties
GRAFANA_DIR=grafana-2025
#APP_JAR - will be populated later...
#AGENT_JAR - will be populated later...



##Exit hook for cleanup...
onExit(){
  echo "Exit hook running..."
  cd observability-kit
  cd "$GRAFANA_DIR"
  docker compose down
  echo "Grafana containers brought down..."
  cd ..
  cd ..
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


echo "Pulling grafana images..."
cd "$GRAFANA_DIR"
docker compose pull

echo "Starting Grafana containers..."
docker compose up -d
cd ..

cd ..

echo "App will be on port 8080, Grafana on port 3000"
echo "Starting demo app... in 5 seconds"
sleep 5s
java -Xmx3G -javaagent:"$AGENT_JAR_PATH"/"$AGENT_JAR" -Dotel.javaagent.configuration-file="$AGENT_CONFIG_FILE"  -jar ./target/"$APP_JAR"

echo "Exiting..."
onExit


