#!/bin/sh

#TODO update to pull and run the local versions...

echo "Starting prometheus..."
./prometheus/prometheus-2.39.1.linux-amd64/prometheus --config.file=/home/deb/prometheus/config.yml &

echo "Starting Jaeger..."
./jaeger/jaeger-1.38.1-linux-amd64/jaeger-all-in-one &

sleep 5s
echo "Done starting services..."

echo "Starting demo app..."
java -javaagent:/home/deb/observabilityDemo/vaadin-opentelemetry-javaagent-1.0.0.rc1.jar      -Dotel.javaagent.configuration-file=/home/deb/observabilityDemo/agent-jaeger-prometheus.properties  -jar /home/deb/observabilityDemo/kitstest-1.0-SNAPSHOT.jar &

sleep 20s

echo "App on port 8080, Jaeger on port 16686, Prometheus on port: 9090"
