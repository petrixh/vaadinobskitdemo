#!/bin/sh
#Variables
AGENT_JAR_PATH=./target
AGENT_CONFIG_FILE=./observability-kit/agent-configs/agent-grafana.properties
GRAFANA_DIR=observability-grafana-setup
#APP_JAR - will be populated later...
#AGENT_JAR - will be populated later...



##Exit hook for cleanup...
onExit(){
  echo "Exit hook running..."
  cd observability-kit
  cd "$GRAFANA_DIR"
  docker-compose down
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

AGENT_JAR=$(ls ../target/vaadin-opentelemetry-javaagent*.jar)
echo "Vaadin Observability Kit Agent jar detected under target/$AGENT_JAR"


echo "Checking grafana docker project..."

if [ -d "$GRAFANA_DIR" ]; then
  echo "Grafana setup already cloned..."
else
  echo "Cloning Grafana Docker setup..."
  git clone https://github.com/vaadin/observability-grafana-setup.git
  cd "$GRAFANA_DIR"

  # Remove .git folder so that git doesn't want to try to tell you that files have changed...
  rm -rf .git

  # Fix docker version conflict on latest base debian installation
  # (official images don't yet use the latest docker binaries)
  replacement="version: \"3.7\""
  sed -i "1s/.*/$replacement/" docker-compose.yml
  # Fix tempo image being too new and not starting up with configs from the original repo
  # (this will likely break later)
  echo "****** Applying fix for 'tempo' container where latest container doesn't work with the configs from example project. For more details, see 'startObservabilityGrafana.sh' around line 54 *****"
  tempoFixSource="image: grafana/tempo:latest"
  tempoFixReplacement="image: grafana/tempo:main-de45a61-arm64"
  #Backing up the original with .orig suffix, just in case..
  #Using + as separator as the strings contain the default / separator...
  sed -i.orig "s+$tempoFixSource+$tempoFixReplacement+" docker-compose.yml

  cd ..
fi

echo "Pulling grafana images..."
cd "$GRAFANA_DIR"
docker-compose pull

echo "Starting Grafana containers..."
docker-compose up -d
cd ..
cd ..


echo "App will be on port 8080, Grafana on port 3000"
echo "Starting demo app... in 5 seconds"
sleep 5s
java -Xmx3G -javaagent:"$AGENT_JAR_PATH"/"$AGENT_JAR" -Dotel.javaagent.configuration-file="$AGENT_CONFIG_FILE"  -jar ./target/"$APP_JAR"

echo "Exiting..."
onExit


