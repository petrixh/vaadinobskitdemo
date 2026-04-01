# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vaadin Observability Kit Demo — a Vaadin 24.9.5 + Spring Boot 3.5.7 + Java 21 application demonstrating observability/tracing with OpenTelemetry. Uses H2 in-memory database, Spring Data JPA, and the Vaadin Observability Kit Agent (3.1.0).

## Build & Run Commands

```bash
# Development mode (hot reload)
./mvnw spring-boot:run

# Production build
./mvnw clean package -Pproduction

# Run integration tests (requires Docker for Playwright + Grafana containers)
./mvnw clean package -Pproduction && ./mvnw verify -Pproduction,it

# Start observability backends (from project root)
cd observability-kit && ./startObservabilityGrafana.sh      # Grafana stack
cd observability-kit && ./startObservabilityNewRelic.sh      # New Relic
cd observability-kit && ./startObservabilityJaegerPrometheusDocker.sh  # Jaeger + Prometheus
```

## Architecture

**Layered Spring Boot + Vaadin Flow (server-driven UI):**

- **Views** (`src/main/java/.../views/`): Vaadin Flow `@Route` components. `MainLayout` provides the app shell with navigation drawer. Each view is a self-contained UI with its own route.
- **Data layer** (`src/main/java/.../data/`): JPA entities extending `AbstractEntity` (UUID-based), Spring Data repositories, and `@Service` classes.
- **Frontend** (`frontend/`): Vaadin theme "kitstest" and auto-generated TypeScript. No separate SPA framework — UI is server-driven.
- **Observability config** (`observability-kit/`): Agent config files for different backends (Grafana OTLP, Jaeger, Prometheus, New Relic), shell scripts to start each stack, and Docker compose for Grafana.

## Testing

Integration tests use **Playwright** running in a Docker container (port 3001 via CDP) against the Spring Boot app (random port). Test class: `PlaywrightIT.java`. Tests navigate views, verify Grafana dashboards, and check Prometheus metrics. Screenshots are saved to `target/`.

The `it` Maven profile starts the app with the observability agent via `spring-boot-maven-plugin` and configures Failsafe for integration tests.

## Key Ports

| Service     | Port |
|-------------|------|
| Demo App    | 8080 |
| Grafana     | 3000 |
| Playwright  | 3001 |
| Prometheus  | 9090 |
| Jaeger UI   | 16686 |

## Demo Views Purpose

The views are intentionally designed to demonstrate observability scenarios:
- **AboutView**: CPU cooker (stress test), bulk user generator, "Blow up" button for exception tracing
- **ImageListView**: Intentional memory leak demo (~1GB per click)
- **MasterDetailView**: Intentionally slow/unoptimized (cascading DB queries visible in traces)
- **OptMasterDetailView**: Optimized version of the same functionality
- **HelloWorldView**: Custom OpenTelemetry span example
- **DashboardView**: System metrics display

## Notes

- Requires a Vaadin license for production builds
- The `observability-grafana-setup` directory is a git submodule
- Docker should run as non-root user (no sudo)
- The observability agent JAR is downloaded at runtime by shell scripts from Maven Central
