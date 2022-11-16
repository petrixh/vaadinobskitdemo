#!/bin/sh

##Bring down cotainers on exit...
onExit(){
  echo "Killing containers..."
  cd observability-kit
  cd vaadin-kits-docker
  docker-compose down
  cd ../..
  echo "Containers killed!"
}

##Bring down containers if there is an error...
trap 'onExit' EXIT
##Bring down containers on Ctrl + c
trap 'onExit' 2

cd vaadin-kits-docker
echo "Pulling containers..."
docker-compose pull

# Make the prometheus data dir so that it has a place to write if enabled...
# If running docker as non-sudo this must be done before prometheus starts...
# See docker-compose.yml
#mkdir prometheus_data

echo "Starting Jaeger and Prometheus containers..."
docker-compose up -d

echo "App will be on port 8080, Jaeger on port 16686, Prometheus on port: 9090... Sleeping 5 seconds to let containers initialize..."
sleep 5s
echo "Done waiting for services... Starting app..."

cd ../..

echo "Starting demo app..."
java -javaagent:./observability-kit/agent/vaadin-opentelemetry-javaagent-1.0.0.rc1.jar      -Dotel.javaagent.configuration-file=./observability-kit/agent-configs/agent-jaeger-prometheus.properties  -jar ./target/kitstest-1.0-SNAPSHOT.jar

##Bring down the containers...
echo "Exiting..."
onExit
