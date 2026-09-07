# Order Processing Demo — Trace Flow

![Order Processing View](demo-view-initial.png)

The Order Processing view simulates a multi-step order workflow with three scenario buttons that each produce different trace patterns. This document describes the pipeline and the Micrometer observations (exported as OpenTelemetry spans) it produces across all three scenarios.

The whole pipeline hangs under Observability Kit's own spans for the button click:
`http post /order-processing` -> `vaadin.request.rpc` -> `vaadin.rpc.event` -> `order.process`.
The custom observations nest there automatically because they are started on the request thread.

## Pipeline Overview

```mermaid
flowchart TD
    UI["OrderProcessingView<br/><i>User submits order</i>"]
    PROC["order.process<br/><i>parent of the step spans</i>"]
    DB0[("DB: save<br/>status = PENDING")]
    VAL["order.validate<br/><i>~50ms</i>"]
    DB1[("DB: save<br/>status = VALIDATED")]
    FRAUD["order.fraud_check<br/><i>~100ms / ~5s</i>"]
    DB2[("DB: save<br/>status = FRAUD_CHECKED")]
    INV["order.reserve_inventory<br/><i>~80ms / ~300ms</i>"]
    DB3[("DB: save<br/>status = INVENTORY_RESERVED")]
    PAY["order.process_payment<br/><i>~150ms</i>"]
    DB4[("DB: save<br/>status = PAYMENT_PROCESSED")]
    DONE[("DB: save<br/>status = FULFILLED")]

    UI --> PROC
    PROC --> DB0 --> VAL --> DB1 --> FRAUD --> DB2 --> INV --> DB3 --> PAY --> DB4 --> DONE

    style PROC fill:#4a9eff,color:#fff
    style VAL fill:#2d8659,color:#fff
    style FRAUD fill:#2d8659,color:#fff
    style INV fill:#2d8659,color:#fff
    style PAY fill:#2d8659,color:#fff
    style DONE fill:#2d8659,color:#fff
    style DB0 fill:#444,color:#ccc
    style DB1 fill:#444,color:#ccc
    style DB2 fill:#444,color:#ccc
    style DB3 fill:#444,color:#ccc
    style DB4 fill:#444,color:#ccc
```

Each box with a green background is an `@Observed`-annotated method that creates an explicit
Micrometer observation — a span in Tempo and a timer in Prometheus at the same time. The grey
database nodes are the kit's own `vaadin.db.query` spans (opt-in via
`vaadin.observability.database=true`), which carry `db.rows` and, with
`vaadin.observability.database-statement=true`, the parameterised SQL as `db.statement`. They
appear as child spans of the step that triggered them.

## Scenario: Happy Path (~480ms)

All steps succeed quickly. The resulting trace is a clean waterfall.

```mermaid
gantt
    title Happy Path Trace Waterfall
    dateFormat X
    axisFormat %Lms

    section order.process
    Parent span                       :0, 480

    section   order.validate
    Validate order                    :0, 50

    section   order.fraud_check
    Check fraud (quick)               :50, 150

    section   order.reserve_inventory
    Reserve inventory                 :150, 230

    section   order.process_payment
    Process payment                   :230, 380

    section   vaadin.db.query
    INSERT/UPDATE (status changes)    :milestone, 0, 0
```

**What to look for in Grafana:** A balanced waterfall where each step takes a proportional amount of time. All spans are green/OK. Between each step you'll see `vaadin.db.query` spans from the status persistence.

## Scenario: Slow Path (~6s)

The fraud check API times out and retries, and inventory uses an N+1 query pattern.

```mermaid
gantt
    title Slow Path Trace Waterfall
    dateFormat X
    axisFormat %Lms

    section order.process
    Parent span                           :0, 6000

    section   order.validate
    Validate order                        :0, 50

    section   order.fraud_check
    Fraud check (total)                   :50, 5050

    section     fraud_check.call_api
    API call - TIMEOUT ⚠️                 :crit, 50, 3050

    section     fraud_check.retry
    Retry - succeeds                      :3050, 5050

    section   order.reserve_inventory
    Inventory (N+1 queries)               :5050, 5400

    section     vaadin.db.query (x10)
    Individual lookups (30ms each)        :5050, 5350

    section   order.process_payment
    Process payment                       :5400, 5600
```

**What to look for in Grafana:**
- The `fraud_check.call_api` child span dominates at ~3s and is marked **ERROR** with a `fraud.timeout` event
- The `fraud_check.retry` child span succeeds after ~2s
- The `order.reserve_inventory` span contains **many small `vaadin.db.query` spans** — this is the N+1 anti-pattern, visible as a cascade of individual `SELECT` operations
- The same N+1 also shows up as metrics: `vaadin_db_query_milliseconds_count` and
  `vaadin_db_fetch_rows_sum`, both tagged by `route`
- Total duration is ~6s vs ~480ms on the happy path

## Scenario: Error Path (~380ms)

Payment is declined. The pipeline stops and the order is marked as failed.

```mermaid
flowchart TD
    PROC["order.process ❌<br/><i>status = ERROR</i>"]
    VAL["order.validate ✅<br/><i>~50ms</i>"]
    FRAUD["order.fraud_check ✅<br/><i>~100ms</i>"]
    INV["order.reserve_inventory ✅<br/><i>~80ms</i>"]
    PAY["order.process_payment ❌<br/><i>~100ms — DECLINED</i>"]
    EXC(["PaymentDeclinedException<br/>insufficient_funds<br/>card_declined"])
    SKIP1["Fulfillment<br/><i>skipped</i>"]

    PROC --> VAL --> FRAUD --> INV --> PAY
    PAY --> EXC
    PAY -.->|"not reached"| SKIP1

    style PROC fill:#c0392b,color:#fff
    style PAY fill:#c0392b,color:#fff
    style EXC fill:#922,color:#fff
    style VAL fill:#2d8659,color:#fff
    style FRAUD fill:#2d8659,color:#fff
    style INV fill:#2d8659,color:#fff
    style SKIP1 fill:#555,color:#999,stroke-dasharray: 5 5
```

**What to look for in Grafana:**
- The `order.process_payment` span is marked **ERROR** with a recorded `PaymentDeclinedException`
- Span attributes show `payment.decline_reason = insufficient_funds` and `payment.error_code = card_declined`
- The parent `order.process` span also shows ERROR with `order.failure_step = Payment`
- The trace is **shorter** than the happy path because processing stops at the payment step
- This trace appears in both the **Traces** and **Error traces** dashboard panels

## Span Attributes Reference

Under Micrometer, an observation's key values come in two flavours. **Low cardinality** values
become both span attributes and Prometheus tags on the observation's timer; **high cardinality**
values are span-only. The bounded ones below (`order.scenario`, `order.status`,
`order.failure_step`, `order.validation.result`, `fraud.result`, `fraud.provider`,
`inventory.warehouse`, `payment.gateway`, `payment.currency`, `payment.status`,
`payment.error_code`) are low cardinality; ids, names, e-mails, amounts and durations are high
cardinality so they never explode the metric label space.

| Span | Key Attributes |
|------|----------------|
| `order.process` | `order.scenario`, `order.customer_name`, `order.product`, `order.quantity`, `order.total_amount`, `order.status`, `order.duration_ms`, `order.failure_step` (on error) |
| `order.validate` | `order.validation.result` |
| `order.fraud_check` | `fraud.provider`, `fraud.score`, `fraud.result`, `fraud.retry_count` (slow), `fraud.total_latency_ms` (slow) |
| `fraud_check.call_api` | `fraud.attempt`, `fraud.timeout` (slow — marked ERROR) |
| `fraud_check.retry` | `fraud.attempt`, `fraud.score`, `fraud.result` (slow) |
| `order.reserve_inventory` | `inventory.product`, `inventory.quantity_requested`, `inventory.warehouse`, `inventory.items_reserved`, `inventory.legacy_queries_executed` (slow) |
| `order.process_payment` | `payment.gateway`, `payment.amount`, `payment.currency`, `payment.transaction_id` (success), `payment.decline_reason` + `payment.error_code` (error) |
