package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Service;

@Service
public class OrderValidationService {

    @WithSpan("order.validate")
    public void validate(CustomerOrder order) {
        Span span = Span.current();
        span.setAttribute("order.customer_name", order.getCustomerName());
        span.setAttribute("order.product", order.getProductName());
        span.setAttribute("order.quantity", order.getQuantity());

        // Simulate validation logic
        simulateWork(50);

        if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
            span.setAttribute("order.validation.result", "failed");
            throw new IllegalArgumentException("Customer name is required");
        }
        if (order.getQuantity() <= 0) {
            span.setAttribute("order.validation.result", "failed");
            throw new IllegalArgumentException("Quantity must be positive");
        }

        span.setAttribute("order.validation.result", "passed");
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
