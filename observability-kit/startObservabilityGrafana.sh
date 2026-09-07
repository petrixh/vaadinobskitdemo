#!/bin/sh
# Starts the local Grafana stack (OTel collector, Tempo, Prometheus, Loki, Grafana) and then
# the demo app with the 'grafana' Spring profile, which points Observability Kit 5's OTLP
# exporters at the collector on localhost:4318.
#
# Run from the observability-kit directory:  ./startObservabilityGrafana.sh
# Requires a production build first:         ./mvnw clean package -Pproduction

#Variables
GRAFANA_DIR=observability-grafana-setup
#APP_JAR - will be populated later...

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

# Just the file name; the app is launched from the project root below.
APP_JAR=$(basename "$(ls ../target/kitstest*.jar)")
echo "App jar detected under target/$APP_JAR"

echo "Updating grafana docker submodule project"
git submodule update --init --recursive

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
java -Xmx3G -jar ./target/"$APP_JAR" --spring.profiles.active=grafana

echo "Exiting..."
onExit
