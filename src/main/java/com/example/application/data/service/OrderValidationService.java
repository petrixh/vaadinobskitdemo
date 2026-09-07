package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Service
public class OrderValidationService {

    private final ObservationRegistry registry;

    public OrderValidationService(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Observed(name = "order.validate", contextualName = "order.validate")
    public void validate(CustomerOrder order) {
        Observation obs = Observations.current(registry);
        // High cardinality: recorded on the span only, never turned into a metric tag.
        obs.highCardinalityKeyValue("order.customer_name", String.valueOf(order.getCustomerName()));
        obs.highCardinalityKeyValue("order.product", String.valueOf(order.getProductName()));
        obs.highCardinalityKeyValue("order.quantity", String.valueOf(order.getQuantity()));

        // Simulate validation logic
        simulateWork(50);

        if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
            obs.lowCardinalityKeyValue("order.validation.result", "failed");
            throw new IllegalArgumentException("Customer name is required");
        }
        if (order.getQuantity() <= 0) {
            obs.lowCardinalityKeyValue("order.validation.result", "failed");
            throw new IllegalArgumentException("Quantity must be positive");
        }

        obs.lowCardinalityKeyValue("order.validation.result", "passed");
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
