package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Service;

@Service
public class FraudCheckService {

    public enum FraudResult {
        APPROVED, DECLINED
    }

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("order-processing", "1.0.0");

    @WithSpan("order.fraud_check")
    public FraudResult checkFraud(CustomerOrder order, String scenario) {
        Span span = Span.current();
        span.setAttribute("fraud.provider", "acme-fraud-api");
        span.setAttribute("fraud.customer_email", order.getCustomerEmail());

        if ("slow".equals(scenario)) {
            return slowFraudCheck(span, order);
        }

        // Happy and error paths: quick check
        simulateWork(100);
        span.setAttribute("fraud.score", 12);
        span.setAttribute("fraud.result", "approved");
        span.setAttribute("fraud.latency_ms", 100);
        return FraudResult.APPROVED;
    }

    private FraudResult slowFraudCheck(Span parentSpan, CustomerOrder order) {
        // First attempt: times out after 3 seconds
        Span callSpan = tracer.spanBuilder("fraud_check.call_api")
                .startSpan();
        try (Scope ignored = callSpan.makeCurrent()) {
            callSpan.setAttribute("fraud.attempt", 1);
            simulateWork(3000);
            callSpan.addEvent("fraud.timeout");
            callSpan.setAttribute("fraud.timeout", true);
            callSpan.setStatus(StatusCode.ERROR, "API call timed out");
        } finally {
            callSpan.end();
        }

        // Retry: succeeds after 2 seconds
        Span retrySpan = tracer.spanBuilder("fraud_check.retry")
                .startSpan();
        try (Scope ignored = retrySpan.makeCurrent()) {
            retrySpan.setAttribute("fraud.attempt", 2);
            simulateWork(2000);
            retrySpan.setAttribute("fraud.score", 15);
            retrySpan.setAttribute("fraud.result", "approved");
        } finally {
            retrySpan.end();
        }

        parentSpan.setAttribute("fraud.score", 15);
        parentSpan.setAttribute("fraud.result", "approved");
        parentSpan.setAttribute("fraud.retry_count", 1);
        parentSpan.setAttribute("fraud.total_latency_ms", 5000);
        return FraudResult.APPROVED;
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
