package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderProcessingService {

    public record StepResult(String name, long durationMs, boolean success, String detail) {}

    public record OrderResult(boolean success, String orderId, long totalDurationMs,
                              String failureStep, String failureReason,
                              List<StepResult> steps) {}

    private final CustomerOrderRepository orderRepository;
    private final OrderValidationService validationService;
    private final FraudCheckService fraudCheckService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final ObservationRegistry registry;

    public OrderProcessingService(CustomerOrderRepository orderRepository,
                                  OrderValidationService validationService,
                                  FraudCheckService fraudCheckService,
                                  InventoryService inventoryService,
                                  PaymentService paymentService,
                                  ObservationRegistry registry) {
        this.orderRepository = orderRepository;
        this.validationService = validationService;
        this.fraudCheckService = fraudCheckService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.registry = registry;
    }

    @Observed(name = "order.process", contextualName = "order.process")
    public OrderResult processOrder(CustomerOrder order, String scenario) {
        Observation obs = Observations.current(registry);
        // Low cardinality values also become tags on the order.process timer, so only
        // bounded values go here; ids, names and durations stay on the span.
        obs.lowCardinalityKeyValue("order.scenario", String.valueOf(scenario));
        obs.highCardinalityKeyValue("order.customer_name", String.valueOf(order.getCustomerName()));
        obs.highCardinalityKeyValue("order.product", String.valueOf(order.getProductName()));
        obs.highCardinalityKeyValue("order.quantity", String.valueOf(order.getQuantity()));
        obs.highCardinalityKeyValue("order.total_amount", String.valueOf(order.getTotalAmount()));

        long overallStart = System.currentTimeMillis();
        List<StepResult> steps = new ArrayList<>();

        // Step 0: Persist initial order
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        obs.highCardinalityKeyValue("order.id", order.getId().toString());

        // Step 1: Validate
        long stepStart = System.currentTimeMillis();
        try {
            validationService.validate(order);
            order.setStatus("VALIDATED");
            orderRepository.save(order);
            steps.add(new StepResult("Validation", System.currentTimeMillis() - stepStart, true, "Passed"));
        } catch (Exception e) {
            return fail(order, obs, steps, "Validation", stepStart, e, overallStart);
        }

        // Step 2: Fraud Check
        stepStart = System.currentTimeMillis();
        try {
            FraudCheckService.FraudResult fraudResult = fraudCheckService.checkFraud(order, scenario);
            order.setStatus("FRAUD_CHECKED");
            orderRepository.save(order);
            steps.add(new StepResult("Fraud Check", System.currentTimeMillis() - stepStart, true,
                    "Result: " + fraudResult.name()));
        } catch (Exception e) {
            return fail(order, obs, steps, "Fraud Check", stepStart, e, overallStart);
        }

        // Step 3: Inventory
        stepStart = System.currentTimeMillis();
        try {
            inventoryService.reserveInventory(order, scenario);
            order.setStatus("INVENTORY_RESERVED");
            orderRepository.save(order);
            steps.add(new StepResult("Inventory", System.currentTimeMillis() - stepStart, true,
                    "Reserved " + order.getQuantity() + " items"));
        } catch (Exception e) {
            return fail(order, obs, steps, "Inventory", stepStart, e, overallStart);
        }

        // Step 4: Payment
        stepStart = System.currentTimeMillis();
        try {
            PaymentService.PaymentResult paymentResult = paymentService.processPayment(order, scenario);
            order.setStatus("PAYMENT_PROCESSED");
            orderRepository.save(order);
            steps.add(new StepResult("Payment", System.currentTimeMillis() - stepStart, true,
                    "Transaction: " + paymentResult.transactionId()));
        } catch (Exception e) {
            return fail(order, obs, steps, "Payment", stepStart, e, overallStart);
        }

        // Step 5: Fulfillment
        order.setStatus("FULFILLED");
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        long totalDuration = System.currentTimeMillis() - overallStart;
        obs.lowCardinalityKeyValue("order.status", "FULFILLED");
        obs.highCardinalityKeyValue("order.duration_ms", String.valueOf(totalDuration));

        return new OrderResult(true, order.getId().toString(), totalDuration,
                null, null, steps);
    }

    private OrderResult fail(CustomerOrder order, Observation obs, List<StepResult> steps,
                             String stepName, long stepStart, Exception e, long overallStart) {
        long stepDuration = System.currentTimeMillis() - stepStart;
        steps.add(new StepResult(stepName, stepDuration, false, e.getMessage()));

        order.setStatus("FAILED");
        order.setFailureReason(stepName + ": " + e.getMessage());
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        long totalDuration = System.currentTimeMillis() - overallStart;
        // The exception is swallowed here, so @Observed never sees it: mark the observation
        // errored by hand. This both flags the span and adds an "error" tag to the timer.
        obs.error(e);
        obs.lowCardinalityKeyValue("order.status", "FAILED");
        obs.lowCardinalityKeyValue("order.failure_step", stepName);
        obs.highCardinalityKeyValue("order.duration_ms", String.valueOf(totalDuration));

        return new OrderResult(false, order.getId().toString(), totalDuration,
                stepName, e.getMessage(), steps);
    }
}
