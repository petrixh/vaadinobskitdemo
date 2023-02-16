#!/bin/sh

#TODO move files and paths to variables for better maintenance...
AGENT_JAR_PATH=./observability-kit/agent/
# What version of the agent to use, and where to download it from if not already downloaded.
# Ensure the version in the download URL also matches the version of the agent....
AGENT_JAR=vaadin-opentelemetry-javaagent-1.0.0.jar
AGENT_DOWNLOAD_PATH=https://repo1.maven.org/maven2/com/vaadin/observability/vaadin-opentelemetry-javaagent/1.0.0
#Agent config file with path
AGENT_CONFIG_FILE=./observability-kit/agent-configs/agent-grafana.properties
GRAFANA_DIR=observability-grafana-setup

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

cd ../target
echo "Checking for agent JAR and downloading if necessary"
if [ -f "$AGENT_JAR" ]; then
  echo "Agent JAR already downloaded..."
else
  #wget http://tools.vaadin.com/nexus/content/repositories/vaadin-prereleases/com/vaadin/observability/vaadin-opentelemetry-javaagent/1.0.0.rc1/vaadin-opentelemetry-javaagent-1.0.0.rc1.jar
  wget "$AGENT_DOWNLOAD_PATH"/"$AGENT_JAR"
fi
cd ..
cd observability-kit

echo "Checking grafana docker project..."

if [ -d "$GRAFANA_DIR" ]; then
  echo "Grafana setup already cloned..."
else
  echo "Cloning Grafana Docker setup..."
  git clone https://github.com/vaadin/observability-grafana-setup.git
  cd "$GRAFANA_DIR"
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

#TODO clone the grafana exaample and run the docker-compose from there...

echo "App will be on port 8080, Grafana on port 3000"
echo "Starting demo app... in 5 seconds"
sleep 5s
java -Xmx3G -javaagent:./target/"$AGENT_JAR"      -Dotel.javaagent.configuration-file="$AGENT_CONFIG_FILE"  -jar ./target/kitstest-1.0-SNAPSHOT.jar

echo "Exiting..."
onExit


