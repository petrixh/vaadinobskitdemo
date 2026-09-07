package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

@Service
public class FraudCheckService {

    public enum FraudResult {
        APPROVED, DECLINED
    }

    private final ObservationRegistry registry;

    public FraudCheckService(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Observed(name = "order.fraud_check", contextualName = "order.fraud_check")
    public FraudResult checkFraud(CustomerOrder order, String scenario) {
        Observation obs = Observations.current(registry);
        obs.lowCardinalityKeyValue("fraud.provider", "acme-fraud-api");
        obs.highCardinalityKeyValue("fraud.customer_email", String.valueOf(order.getCustomerEmail()));

        if ("slow".equals(scenario)) {
            return slowFraudCheck(obs, order);
        }

        // Happy and error paths: quick check
        simulateWork(100);
        obs.highCardinalityKeyValue("fraud.score", "12");
        obs.lowCardinalityKeyValue("fraud.result", "approved");
        obs.highCardinalityKeyValue("fraud.latency_ms", "100");
        return FraudResult.APPROVED;
    }

    private FraudResult slowFraudCheck(Observation parentObservation, CustomerOrder order) {
        // First attempt: times out after 3 seconds.
        // Child observations pick up the parent from the registry when they are started.
        Observation callObservation = Observation
                .createNotStarted("fraud_check.call_api", registry)
                .lowCardinalityKeyValue("fraud.attempt", "1")
                .start();
        try (Observation.Scope ignored = callObservation.openScope()) {
            simulateWork(3000);
            callObservation.event(Observation.Event.of("fraud.timeout"));
            callObservation.lowCardinalityKeyValue("fraud.timeout", "true");
            callObservation.error(new TimeoutException("API call timed out"));
        } finally {
            callObservation.stop();
        }

        // Retry: succeeds after 2 seconds
        Observation retryObservation = Observation
                .createNotStarted("fraud_check.retry", registry)
                .lowCardinalityKeyValue("fraud.attempt", "2")
                .start();
        try (Observation.Scope ignored = retryObservation.openScope()) {
            simulateWork(2000);
            retryObservation.highCardinalityKeyValue("fraud.score", "15");
            retryObservation.lowCardinalityKeyValue("fraud.result", "approved");
        } finally {
            retryObservation.stop();
        }

        parentObservation.highCardinalityKeyValue("fraud.score", "15");
        parentObservation.lowCardinalityKeyValue("fraud.result", "approved");
        parentObservation.highCardinalityKeyValue("fraud.retry_count", "1");
        parentObservation.highCardinalityKeyValue("fraud.total_latency_ms", "5000");
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
