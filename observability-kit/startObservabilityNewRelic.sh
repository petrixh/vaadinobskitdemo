#!/bin/sh
# Starts the demo app with the 'newrelic' Spring profile, which points Observability Kit 5's
# OTLP exporters at New Relic's EU endpoints. There is nothing to run locally.
#
# Run from the observability-kit directory:
#   NEW_RELIC_LICENSE_KEY=eu01xx...NRAL ./startObservabilityNewRelic.sh
# Requires a production build first:  ./mvnw clean package -Pproduction

##Exit hook for cleanup...
onExit(){
  echo "Exit hook running..."
}

##Bring down containers if there is an error...
trap 'onExit' EXIT
##Bring down containers on Ctrl + c
trap 'onExit' 2

if [ -z "$NEW_RELIC_LICENSE_KEY" ]; then
  echo "Set NEW_RELIC_LICENSE_KEY to your New Relic ingest license key first." >&2
  exit 1
fi

# Just the file name; the app is launched from the project root below.
APP_JAR=$(basename "$(ls ../target/kitstest*.jar)")
echo "App jar detected under target/$APP_JAR"

cd ..

echo "App on port 8080, new relic on https://one.eu.newrelic.com/"
echo "Starting demo app in 3 seconds..."
sleep 3s
java -Xmx3G -jar ./target/"$APP_JAR" --spring.profiles.active=newrelic
