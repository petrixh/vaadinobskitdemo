# Observability Kit Demo

A test/demo setup for Observability Kit using **Vaadin 25.3.0-beta1** and **Spring Boot 4.1.1** (Java 21), with **Observability Kit 5.0.0-beta1**. For the Grafana part, this project references (as a submodule) the project: https://github.com/petrixh/observability-grafana-setup

> **Observability Kit 5 is a Micrometer library, not a Java agent.** There is no agent JAR to
> download, no `-javaagent` flag and no `otel.*` properties. If you are looking for the 4.x
> agent setup, see the history of this repo before the obskit 5 migration.

It is recommended that you run this on a system with: 
- 4 or "multiple" cores/threads (some leaky ops are heavy, but it will always use one less than available unless only one is available)
- some ram... 4GB for instance if you show the leaky ram demo
- You might want to tweak the -Xmx3G parameter in the launch scripts if you're running less than 4GB of ram 

In order to run the demos, you'll need to have the appropriate Vaadin license installed. 
On your dev system you probably already have everything installed. For a VM, you might want to 
install one of the server (or offline but they are limited in nr) licenses (production builds only): 
https://vaadin.com/myaccount/licenses 

Observability Kit 5 also checks a license at runtime. In **production mode** the check always
passes, so the packaged JAR and CI need nothing extra. In **dev mode** (`./mvnw spring-boot:run`)
it needs a local Vaadin license key; without one the kit logs
`No valid vaadin-observability-kit license found`, registers nothing, and the app otherwise runs
normally. If you see empty dashboards from a dev-mode run, check for that line first.

Step 0 after cloning the project, run:

```
./mvnw clean package -Pproduction
```

to make the JAR for the app itself. All other scripts expects the JAR to exist. 

In the `observability-kit` folder or from the project root: 

```
cd observability-kit && chmod +x *.sh && cd ..
```

in order to enxure they are executable... 

If you're planning on using NewRelic, put your ingest license key in the environment before
starting the app - there is no config file to copy any more:

```
export NEW_RELIC_LICENSE_KEY=eu01xx......NRAL
```

UI ports on host system (depending on what you run): 
- 8080  -> DemoApp
- 9090  -> Prometheus
- 3000  -> Grafana

All demos are made so that the do `not` require docker to run as `root`. You could still do that if you wanted to, 
however better practice is to run docker as local user. See https://docs.docker.com/engine/install/linux-postinstall/

Or on Linux just blindly run: 

`sudo groupadd docker`

`sudo usermod -aG docker $USER`

`newgrp docker`

and you should be able to run docker commands without `sudo`. 

## What Observability Kit 5 needs

One dependency auto-configures the kit; Spring Boot's OpenTelemetry starter does the exporting.
All versions come from `vaadin-bom` / the Spring Boot parent, so none of these carry a `<version>`:

```xml
<!-- The kit itself. Version comes from vaadin-bom (observability.kit.starter.version). -->
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>observability-kit-starter</artifactId>
</dependency>
<!-- ObservationRegistry, /actuator/prometheus and the kit's /actuator/vaadin endpoint -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- OTLP export of metrics + traces -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
<!-- @Observed / @Timed / @Counted are AOP aspects; without this they silently do nothing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
<!-- Optional: lets Prometheus scrape /actuator/prometheus instead of pushing over OTLP -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

and the configuration, from `src/main/resources/application.properties`:

```properties
# Becomes the OpenTelemetry service.name; the Grafana dashboard filters on service_name="vaadin"
spring.application.name=vaadin
management.endpoints.web.exposure.include=health,prometheus,vaadin

# Kit features that are off by default
vaadin.observability.database=true            # vaadin.db.query spans + vaadin.db.fetch.rows per route
vaadin.observability.database-statement=true  # attach the SQL as db.statement (demo only)
vaadin.observability.ui-state=true            # vaadin.ui.state.* gauges
vaadin.observability.insights-details=true    # exception details in the insights endpoint (demo only)

# @Observed needs the aspect enabled; the default trace sample rate is 10%
management.observations.annotations.enabled=true
management.tracing.sampling.probability=1.0
```

Where the telemetry goes is a Spring profile: `application-grafana.properties` points the OTLP
exporters at the local collector, `application-newrelic.properties` at New Relic. With no profile
the app exports nothing, so `./mvnw spring-boot:run` stays quiet.

### Watch out for

Four things that fail **silently** rather than with an error:

1. **`@WithSpan` is inert.** It only ever worked through the 4.x agent. Use Micrometer's
   `@Observed`, and keep `spring-boot-starter-aspectj` on the classpath with
   `management.observations.annotations.enabled=true` - without either, the annotation compiles
   and does nothing.
2. **`GlobalOpenTelemetry` is empty.** Spring Boot does not register its OTel SDK there, so
   `GlobalOpenTelemetry.getTracer(...)` hands back a no-op tracer. Inject an `ObservationRegistry`
   instead.
3. **Traces are sampled at 10% by default.** Set `management.tracing.sampling.probability=1.0` for
   a demo or it looks broken.
4. **Timers publish count/sum/max only.** `histogram_quantile()` panels show "No data" unless
   percentile histograms are enabled per meter. Build latency panels on
   `rate(_sum[1m]) / rate(_count[1m])` and the `_max` series.

## What you get

**Meters** (all renamed from 4.x - `vaadin.session.count` is now `vaadin.sessions.active`,
`vaadin.ui.count` is now `vaadin.ui.active`):
`vaadin_sessions_active`, `vaadin_ui_active`, `vaadin_request_duration_*`,
`vaadin_navigation_*` (tagged by `route`), `vaadin_rpc_duration_*`, `vaadin_errors_total`
(tagged by `exception`, `route`, `component`), `vaadin_db_query_*` and `vaadin_db_fetch_rows_*`
(tagged by `route`), `vaadin_ui_state_*`, plus everything Spring Boot Actuator binds -
`jvm_memory_used_bytes`, `process_cpu_usage` and friends. The agent-only
`jvm_cpu_recent_utilization_ratio` is gone.

**Spans**: `vaadin.request.*`, `vaadin.navigation <route>`, `vaadin.rpc.*`, `vaadin.data.fetch`,
`vaadin.data.count`, `vaadin.db.query`. Custom observations started on the request thread nest
underneath them automatically.

**Endpoints**:
- `GET /actuator/vaadin/observability` - the kit's insights endpoint: recent failed and slow
  interactions and queries. Click "Blow Up!" on the About view first so it has something to show.
- `GET /actuator/prometheus` - every meter above, in Prometheus text format. The quickest way to
  see the real meter names locally:
  ```
  curl -s localhost:8080/actuator/prometheus | grep '^vaadin_'
  ```

## Metrics: push vs scrape

The `grafana` and `newrelic` profiles **push** metrics over OTLP. Prometheus then scrapes them
from the collector, which is why the labels carry `exported_job="vaadin"`. The alternative is to
let Prometheus **scrape** the app directly at `/actuator/prometheus`; that needs a second target
in the Grafana setup's `prometheus/prometheus.yml`, and is documented but not enabled there.

## How to run the Grafana Demo

This script will: 
- Pull the Vaadin Grafana example docker setup from git 
- Pull and run the grafana docker-compose setup... 
- Start the demo app with the `grafana` Spring profile

Run:
```
cd observability-kit
./startObservabilityGrafana.sh
```

Ctrl + c kills the server and brings down the grafana containers... 

Grafana is on http://localhost:3000 (anonymous admin, no login) - open **Vaadin Dashboard - 5.0.0**
in the Vaadin folder.

If you get an error about docker-compose version mismatch, try to downgrade the compose file version. This is already
done in the script to v3.7. Version 3.8 brought a bunch of "stack" features that I don't think are needed in the Grafana build...
https://docs.docker.com/compose/compose-file/compose-versioning/#version-38

## How to run the NewRelic Demo

Make sure you have a New Relic account and its **ingest license key** (the `eu01...NRAL` one, not
a user API key). Then:

```
cd observability-kit
NEW_RELIC_LICENSE_KEY=eu01xx......NRAL ./startObservabilityNewRelic.sh
```

The script refuses to start without that variable. The terminal is intentionally left hanging;
Ctrl + c kills the app.

New relic console: https://one.eu.newrelic.com/

The endpoints in `application-newrelic.properties` are the EU ones. For a US account, swap
`otlp.eu01.nr-data.net` for `otlp.nr-data.net`.

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
- Vaadin specific stats like `vaadin.ui.active` and `vaadin.sessions.active`

## Exceptions and logs
Navigate to the About page. There is a button "Blow up" that throws an unhandled exception. 
The app is running in production mode, so nothing is shown to the user (at least not a stacktrace). 

Go to your tracker for traces/spans. You might need to refresh the data but soon there should be an 
entry with an error. The same exception also shows up in `vaadin_errors_total` (tagged with
`exception="RuntimeException"`, `route="about"`, `component="Button"`) and in
`GET /actuator/vaadin/observability`.

## CPU Cooker and Ram Leaker
If you need to demonstrate metrics in more definite ways. On the About view there is a CPU cooker that starts
totalThreads - 1 cpu trheads for 5 minutes to make the CPU graph spike.

Additionally, on the ImageList view there are buttons for leaking RAM. It will leak about 1GB per click, so you can also 
demonstrate "out of heap space" errors this way if you wish. It uses totalSystemThreads - 1 threads for this to make it
a bit faster (who thought generating random stuff actually takes much effort)... 

## Order Processing (multi-step workflow tracing)
The Order Processing view simulates a realistic order pipeline — validation, fraud check, inventory reservation, and payment — where each step creates a Micrometer observation (exported as an OpenTelemetry span) with rich attributes. Three scenario buttons produce different trace patterns:

- **Happy Path** (~480ms) — clean waterfall, all steps succeed
- **Slow Path** (~6s) — fraud API timeout + retry, N+1 inventory queries
- **Error Path** (~380ms) — payment declined, pipeline stops early with recorded exception

See [docs/order-processing-flow.md](docs/order-processing-flow.md) for detailed flow diagrams and a span attributes reference.

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
