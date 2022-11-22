# KitsTest

A test/demo setup for running kit demos. For now ObservabilityKit being the only one included.

It is recommended that you run this on a system with: 
- 4 or "multiple" cores/threads (some leaky ops are heavy, but it will always use one less than available unless only one is available)
- some ram... 4GB for instance if you show the leaky ram demo
- You might want to tweak the -Xmx3G parameter in the launch scripts if you're running less than 4GB of ram 

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
Make sure you have created a new relic account and setup the API key into the `agent-new-relic.properties` file. Should look something like: 

```otel.exporter.otlp.headers=Api-Key=eu0xxxxx...```

Run: 

`cd observability-kit`

`./startObservabilityNewRelic.sh` 

This should download the agent JAR and start the demo app. The terminal is intentionally left hanging. When you hit Ctrl + c it will kill the app...

New relic console: https://one.eu.newrelic.com/

## How to run Jaeger and Prometheus Demo
(free, local only and probably best for dev/internal low security stuff)

Run:

`cd observability-kit`

`./startObservabilityJaegerPrometheusDocker.sh`

This will download the agent JAR, the Jaeger and Prometheus containers (docker), start them up and start the demo app. 
The terminal is intentionally left hanging, once you hit Ctrl + c it will kill the app and bring down the containers.

TODO: figure out how to get Jaeger to show Prometheus metrics for spans... 

## How to run Grafana Demo
(if you get an error about docker-compose version see hint below): 

This script will: 
- Pull the Vaadin Grafana example docker setup from git
- Downgrade the docker-compose.yml file to version 3.7
- Pull and run the grafana docker-compose setup... 
- Start the demo app

Run:
`./startObservabilityGrafana.sh`

Ctrl + c kills the server and bring down the grafana containers... 

If you get an error about docker-compose version mismatch, try to downgrade the compose file version. This is already
done in the script to v3.7. Version 3.8 brought a bunch of "stack" features that I don't think are needed in the Grafana build...
https://docs.docker.com/compose/compose-file/compose-versioning/#version-38

## How to run NewRelic host monitor (for demoing NewRelic more) 
To run the NewRelic host infrastructure monitor you can find the scripts in `observability-kit/extras`.

*NOTE: the container will run in privileged mode and will have read access to your hosts processes and file systems!*

1. edit `new-relic-host-infra-monitor/newrelic-infra.yml`
2. put your 40-char license key in the file
3. (optionally) change the name of agent if you wish..

The license key in this case should look something like: 

```license_key: eu0xxxxx..``` (so no ApiKey= at the front like in the other NR file) 

Run `./newRelicHostMonitorDocker.sh` which will ask you if you want to force rebuild the docker image even if it has been created. 
If you changed the configs or if you want a new version of the NR agent, rebuilding will do this for you and start the container. 


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

Additionally, on the ImageList view there are buttons for leaking RAM. It will leak about 1GB per click, so you can also 
demonstrate "out of heap space" errors this way if you wish. It uses totalSystemThreads - 1 threads for this to make it
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

TODO: Add a cleaned up version that has the same functionality but works much faster...
