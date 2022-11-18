# KitsTest

A test/demo setup for running kit demos. For now ObservabilityKit being the only one included.

In order to run the demos, you'll need to have the appropriate Vaadin license installed. 
On your dev system you probably already have everything installed. For a VM, you might want to 
install one of the server (or offline, limited to 3, I think) licenses (production builds only): 
https://vaadin.com/myaccount/licenses 

Step 0 after cloning the project, run:

`mvn clean package -P production`

to make the JAR for the app itself. All other scripts expects the JAR to exist. 

If you're planning on using NewRelic, copy your NewRelic API key to: 
`observability-kit/agent-configs/agent-new-relic.properties`

UI ports on host system (depending on what you run): 
- 8080  -> DemoApp
- 16686 -> JaegerUI 
- 9090  -> Prometheus
- 3000  -> Grafana

All demos are made so that the do `not` require docker to run as `root`. You could still do that if you wanted to, 
however better practice is to run docker as local user. See https://docs.docker.com/engine/install/linux-postinstall/

Or on Linux just blindly run: 

`sudo groupadd docker`

`sudo usermod -aG docker $USER`

`newgrp docker`

and you should be able to run docker commands wihtout `sudo`. 

## How to run NewRelic Demo (easiest)
Make sure you have created a new relic account and setup the API key into the `agent-new-relic.properties` file. 

Run: 
`cd observability-kit`

`./startObservabilityNewRelic.sh` 

This should download the agent JAR and start the demo app. The terminal is intentionally left hanging. When you hit Ctrl + c it will kill the app...

New relic console: https://one.eu.newrelic.com/

## How to run Jaeger and Prometheus Demo
###(free, local only and probably best for dev/internal low security stuff)
Run:
`cd observability-kit`

`./startObservabilityJaegerPrometheusDocker.sh`

This sill download the agent JAR, the Jaeger and Prometheus containers (docker), start them up and start the demo app. 
The terminal is intentionally left hanging, once you hit Ctrl + c it will kill the app and bring down the containers.   

## How to run Grafana Demo

TODO: update script to do the next steps

Clone:

`git clone https://github.com/vaadin/observability-grafana-setup.git` 


Run: 

`cd observability-grafana-setup`

`docker-compose up`

Go back to the `observability-kit`-folder with the .sh scripts... 

Run:
`./startObservabilityGrafana.sh`

Ctrl + c kills the server... then remember to go back to the grafana folder and run `docker-compose down`

# Demos included

## Basic tracing and statistics
Navigate around the app a bit, click on a user, edit a name, save etc... just to generate some traffic. 

Go to your data collector of choice and show: 
- Traces and their nested spans
- The statistics like CPU/Memory usage
- Vaadin specific stats like `ui count` and `session count`

## Exceptions and logs
Navigate to the About page. There is a button "Blow up" that throws an unhandled exception. 
The app is running in production mode, so nothing is shown to the user (at least not a stacktrace). 

Go to your tracker for traces/spans (NewRelic/Jaeger). You might need to refresh the data but soon there should be an 
entry with an error. 

## CPU Cooker and Ram Leaker
If you need to demonstrate metrics in more definite ways. On the About view there is a CPU cooker that starts
totalThreads - 1 cpu trheads for 5 minutes to make the CPU graph spike.

Additionally on the ImageList view there are buttons for leaking RAM. It will leak about 1GB per click, so you can also 
demonstrate "out of heapspace" errors this way if you wish. It uses totalSystemThreads - 1 threads for this to make it
a bit faster (who thought generating random stuff actually takes much effort)... 

## Typical Vaadin slow view demo (longer demo, still WIP)
The MasterDetail view is implemented in a way where under "normal test" circumstances (like a developer would use) it 
behaves "fast enough" to slip through the cracks... However, there is a button... on the About page that adds users to 
the database... The additional 100k users in the db was enough on my system for the view to take about 10-ish 
seconds to load. 

Once you've opened the view, you might want to look at the traces. 

The code, while not good by any means, is not completely unlike what you could encounter. Basically, it is written in a 
way that cases refreshes and population of filters etc. to cause multiple refreshes hitting the DB 
several times and repopulating, filtering etc. the table several times due to the listener, firing a listener, firing a 
listener... pattern? :) 

TODO: Add a cleaned up version that has the same funcationality but works much faster...
