#!/bin/sh

#TODO move files and paths to variables for better maintenance...
AGENT_JAR_PATH=./observability-kit/agent/
AGENT_JAR=vaadin-opentelemetry-javaagent-1.0.0.rc1.jar
GRAFANA_DIR=observability-grafana-setup

##Exit hook for cleanup...
onExit(){
  echo "Exit hook running..."
  cd observability-kit
  cd "$GRAFANA_DIR"
  docker-compose down
  echo "Grafana containers brought down..."
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
cd observability-kit

echo "Checking grafana docker project..."

if [ -d "$GRAFANA_DIR" ]; then
  echo "Grafana setup already cloned..."
else
  echo "Cloning Grafana Docker setup..."
  git clone https://github.com/vaadin/observability-grafana-setup.git
  cd "$GRAFANA_DIR"
  replacement="version: \"3.7\""
  sed -i "1s/.*/$replacement/" docker-compose.yml
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
java -javaagent:./target/vaadin-opentelemetry-javaagent-1.0.0.rc1.jar      -Dotel.javaagent.configuration-file=./observability-kit/agent-configs/agent-grafana.properties  -jar ./target/kitstest-1.0-SNAPSHOT.jar

echo "Exiting..."
onExit


