# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vaadin Observability Kit Demo — a Vaadin 25.3.0-beta1 + Spring Boot 4.1.1 + Java 21 application demonstrating observability/tracing. Uses H2 in-memory database, Spring Data JPA, and Vaadin Observability Kit 5.0.0-beta1.

### Obskit 5 facts (read before changing observability plumbing)

Observability Kit 5 is a **plain Micrometer library**, not a Java agent. There is no agent JAR, no
`-javaagent`, no `otel.*` properties and no OpenTelemetry version ceiling. One dependency
(`com.vaadin:observability-kit-starter`) auto-configures on the classpath; its version comes from
`vaadin-bom` (`<observability.kit.starter.version>`), so it carries no explicit version here.
Export is not the kit's job — `spring-boot-starter-opentelemetry` does that. Requires Vaadin 25.3+,
Java 21, Spring Boot 4.

Meters go into the `MeterRegistry`, spans through the `ObservationRegistry`. **Everything was
renamed in 5.x**: `vaadin.session.count` -> `vaadin.sessions.active`, `vaadin.ui.count` ->
`vaadin.ui.active`, JVM metrics now come from Actuator (`jvm_memory_used_bytes`,
`process_cpu_usage`; `jvm_cpu_recent_utilization_ratio` no longer exists). Span names in Tempo are
the observations' *contextual* names: `vaadin.request.rpc`, `vaadin.navigation <route>`,
`vaadin.rpc.event`, plus `vaadin.data.fetch`, `vaadin.data.count`, `vaadin.db.query`. The old
`vaadin.navigation.route` attribute is gone; the route is a `route` attribute/tag.

**Four things that fail silently** — design around them:

1. **`@WithSpan` is inert.** Its replacement `@Observed` needs `spring-boot-starter-aspectj` (the
   Boot 4 name; `spring-boot-starter-aop` does not exist for 4.1.1) *and*
   `management.observations.annotations.enabled=true`. It is also proxy-based, so an annotated
   method only gets a span when called from another bean.
2. **Spring Boot does not register its OTel SDK as `GlobalOpenTelemetry`.** Any
   `GlobalOpenTelemetry.getTracer(...)` call returns a no-op tracer and emits nothing. Inject an
   `ObservationRegistry`.
3. **Tracing samples at 10% by default** (`management.tracing.sampling.probability`). This repo
   sets it to `1.0`.
4. **Timers publish count/sum/max only.** The OTLP registry emits a single `+Inf` bucket, so
   `histogram_quantile()` silently yields `NaN`. Latency panels use `rate(_sum)/rate(_count)` and
   `_max`.

The kit's runtime license check passes in **production mode**, so CI (`-Pproduction`) is fine. In
dev mode without a local Vaadin license key it logs `No valid vaadin-observability-kit license
found` and registers nothing.

To prove telemetry is actually flowing, don't trust a green build:

- `curl -s localhost:8080/actuator/prometheus | grep '^vaadin_'` — the kit's own meters.
- `curl localhost:8080/actuator/vaadin/observability` — the kit's insights endpoint.
- `PlaywrightIT.testVaadinInstrumentationActive` — searches Tempo for
  `{name=~"vaadin.navigation.*" && span.route="hello"}`; both halves are kit-only, so generic
  Spring/servlet tracing cannot satisfy it.
- `PlaywrightIT.testCustomObservationsExported` — runs the order scenarios and searches Tempo for
  `order.process`, `order.validate`, `vaadin.db.query` and an errored `order.process_payment`.
  This is the canary for failure modes 1 and 2 above.

When re-verifying by hand, wipe the stack first (`docker compose down -v` in
`observability-kit/observability-grafana-setup`) — Prometheus and Tempo retain the previous run's
data well inside the tests' lookback windows, so assertions can otherwise pass on stale telemetry.

## Build & Run Commands

```bash
# Development mode (hot reload)
./mvnw spring-boot:run

# Production build
./mvnw clean package -Pproduction

# Run integration tests (requires Docker for Playwright + Grafana containers)
./mvnw clean package -Pproduction && ./mvnw verify -Pproduction,it

# Start observability backends (needs a production build first)
cd observability-kit && ./startObservabilityGrafana.sh                                    # Grafana stack
cd observability-kit && NEW_RELIC_LICENSE_KEY=eu01...NRAL ./startObservabilityNewRelic.sh  # New Relic
```

Where telemetry goes is a Spring profile (`grafana` / `newrelic`, see
`src/main/resources/application-*.properties`). With no profile the app exports nothing.

## Architecture

**Layered Spring Boot + Vaadin Flow (server-driven UI):**

- **Views** (`src/main/java/.../views/`): Vaadin Flow `@Route` components. `MainLayout` provides the app shell with navigation drawer. Each view is a self-contained UI with its own route.
- **Data layer** (`src/main/java/.../data/`): JPA entities extending `AbstractEntity` (UUID-based), Spring Data repositories, and `@Service` classes.
- **Frontend** (`frontend/`): Vaadin theme "kitstest" and auto-generated TypeScript. No separate SPA framework — UI is server-driven.
- **Observability config**: kit and exporter properties live in `src/main/resources/application.properties` plus the `application-grafana.properties` / `application-newrelic.properties` profiles. `observability-kit/` only holds the launch scripts, the Grafana compose submodule and the New Relic host-infra extras.

## Testing

Integration tests use **Playwright** running in a Docker container (port 3001 via CDP) against the Spring Boot app (random port). Test class: `PlaywrightIT.java`. Tests navigate views, verify Grafana dashboards, and check Prometheus metrics. Screenshots are saved to `target/`.

The `it` Maven profile starts the app via `spring-boot-maven-plugin` with the `grafana` Spring profile (so it exports to the local collector) and configures Failsafe for integration tests.

## Key Ports

| Service     | Port |
|-------------|------|
| Demo App    | 8080 |
| Grafana     | 3000 |
| Playwright  | 3001 |
| Prometheus  | 9090 |

## Demo Views Purpose

The views are intentionally designed to demonstrate observability scenarios:
- **AboutView**: CPU cooker (stress test), bulk user generator, "Blow up" button for exception tracing
- **ImageListView**: Intentional memory leak demo (~1GB per click)
- **MasterDetailView**: Intentionally slow/unoptimized (cascading DB queries visible in traces)
- **OptMasterDetailView**: Optimized version of the same functionality
- **HelloWorldView**: Custom Micrometer observation example
- **DashboardView**: System metrics display

## Notes

- Requires a Vaadin license for production builds
- The `observability-grafana-setup` directory is a git submodule
- Docker should run as non-root user (no sudo)
