package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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

    public OrderProcessingService(CustomerOrderRepository orderRepository,
                                  OrderValidationService validationService,
                                  FraudCheckService fraudCheckService,
                                  InventoryService inventoryService,
                                  PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.validationService = validationService;
        this.fraudCheckService = fraudCheckService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    @WithSpan("order.process")
    public OrderResult processOrder(CustomerOrder order, String scenario) {
        Span span = Span.current();
        span.setAttribute("order.scenario", scenario);
        span.setAttribute("order.customer_name", order.getCustomerName());
        span.setAttribute("order.product", order.getProductName());
        span.setAttribute("order.quantity", order.getQuantity());
        span.setAttribute("order.total_amount", order.getTotalAmount());

        long overallStart = System.currentTimeMillis();
        List<StepResult> steps = new ArrayList<>();

        // Step 0: Persist initial order
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        span.setAttribute("order.id", order.getId().toString());

        // Step 1: Validate
        long stepStart = System.currentTimeMillis();
        try {
            validationService.validate(order);
            order.setStatus("VALIDATED");
            orderRepository.save(order);
            steps.add(new StepResult("Validation", System.currentTimeMillis() - stepStart, true, "Passed"));
        } catch (Exception e) {
            return fail(order, span, steps, "Validation", stepStart, e, overallStart);
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
            return fail(order, span, steps, "Fraud Check", stepStart, e, overallStart);
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
            return fail(order, span, steps, "Inventory", stepStart, e, overallStart);
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
            return fail(order, span, steps, "Payment", stepStart, e, overallStart);
        }

        // Step 5: Fulfillment
        order.setStatus("FULFILLED");
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        long totalDuration = System.currentTimeMillis() - overallStart;
        span.setAttribute("order.status", "FULFILLED");
        span.setAttribute("order.duration_ms", totalDuration);

        return new OrderResult(true, order.getId().toString(), totalDuration,
                null, null, steps);
    }

    private OrderResult fail(CustomerOrder order, Span span, List<StepResult> steps,
                             String stepName, long stepStart, Exception e, long overallStart) {
        long stepDuration = System.currentTimeMillis() - stepStart;
        steps.add(new StepResult(stepName, stepDuration, false, e.getMessage()));

        order.setStatus("FAILED");
        order.setFailureReason(stepName + ": " + e.getMessage());
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        long totalDuration = System.currentTimeMillis() - overallStart;
        span.setStatus(StatusCode.ERROR, "Failed at " + stepName);
        span.recordException(e);
        span.setAttribute("order.status", "FAILED");
        span.setAttribute("order.failure_step", stepName);
        span.setAttribute("order.duration_ms", totalDuration);

        return new OrderResult(false, order.getId().toString(), totalDuration,
                stepName, e.getMessage(), steps);
    }
}
