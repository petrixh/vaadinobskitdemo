package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private final CustomerOrderRepository orderRepository;

    public InventoryService(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @WithSpan("order.reserve_inventory")
    public void reserveInventory(CustomerOrder order, String scenario) {
        Span span = Span.current();
        span.setAttribute("inventory.product", order.getProductName());
        span.setAttribute("inventory.quantity_requested", order.getQuantity());

        if ("slow".equals(scenario)) {
            slowInventoryCheck(span, order);
            return;
        }

        // Happy and error paths: quick reservation
        simulateWork(80);
        span.setAttribute("inventory.warehouse", "US-EAST-1");
        span.setAttribute("inventory.items_reserved", order.getQuantity());
        span.setAttribute("inventory.stock_remaining", 142);
    }

    private void slowInventoryCheck(Span span, CustomerOrder order) {
        span.setAttribute("inventory.legacy_stock_check", true);
        span.setAttribute("inventory.warehouse", "US-EAST-1");

        // N+1 query pattern: first fetch all fulfilled orders...
        List<CustomerOrder> fulfilledOrders = orderRepository.findByStatus("FULFILLED");

        // ...then individually look up each one (N+1 anti-pattern)
        int checked = 0;
        for (CustomerOrder fulfilled : fulfilledOrders) {
            orderRepository.findById(fulfilled.getId());
            simulateWork(30);
            checked++;
            if (checked >= 10) break;
        }

        // If no fulfilled orders exist yet, simulate the N+1 with email lookups
        if (checked == 0) {
            for (int i = 0; i < 10; i++) {
                orderRepository.findByCustomerEmail("lookup-" + i + "@example.com");
                simulateWork(30);
            }
            checked = 10;
        }

        span.setAttribute("inventory.legacy_queries_executed", checked);
        span.setAttribute("inventory.items_reserved", order.getQuantity());
        span.setAttribute("inventory.stock_remaining", 87);
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
