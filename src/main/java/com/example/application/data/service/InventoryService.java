package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private final CustomerOrderRepository orderRepository;
    private final ObservationRegistry registry;

    public InventoryService(CustomerOrderRepository orderRepository, ObservationRegistry registry) {
        this.orderRepository = orderRepository;
        this.registry = registry;
    }

    @Observed(name = "order.reserve_inventory", contextualName = "order.reserve_inventory")
    public void reserveInventory(CustomerOrder order, String scenario) {
        Observation obs = Observations.current(registry);
        obs.highCardinalityKeyValue("inventory.product", String.valueOf(order.getProductName()));
        obs.highCardinalityKeyValue("inventory.quantity_requested", String.valueOf(order.getQuantity()));

        if ("slow".equals(scenario)) {
            slowInventoryCheck(obs, order);
            return;
        }

        // Happy and error paths: quick reservation
        simulateWork(80);
        obs.lowCardinalityKeyValue("inventory.warehouse", "US-EAST-1");
        obs.highCardinalityKeyValue("inventory.items_reserved", String.valueOf(order.getQuantity()));
        obs.highCardinalityKeyValue("inventory.stock_remaining", "142");
    }

    private void slowInventoryCheck(Observation obs, CustomerOrder order) {
        obs.lowCardinalityKeyValue("inventory.legacy_stock_check", "true");
        obs.lowCardinalityKeyValue("inventory.warehouse", "US-EAST-1");

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

        obs.highCardinalityKeyValue("inventory.legacy_queries_executed", String.valueOf(checked));
        obs.highCardinalityKeyValue("inventory.items_reserved", String.valueOf(order.getQuantity()));
        obs.highCardinalityKeyValue("inventory.stock_remaining", "87");
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
