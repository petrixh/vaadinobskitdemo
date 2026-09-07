package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    public static class PaymentDeclinedException extends RuntimeException {
        private final String declineReason;
        private final String errorCode;

        public PaymentDeclinedException(String declineReason, String errorCode) {
            super("Payment declined: " + declineReason);
            this.declineReason = declineReason;
            this.errorCode = errorCode;
        }

        public String getDeclineReason() {
            return declineReason;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public record PaymentResult(String transactionId, double amount) {}

    private final ObservationRegistry registry;

    public PaymentService(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Observed(name = "order.process_payment", contextualName = "order.process_payment")
    public PaymentResult processPayment(CustomerOrder order, String scenario) {
        Observation obs = Observations.current(registry);
        obs.lowCardinalityKeyValue("payment.gateway", "stripe");
        obs.highCardinalityKeyValue("payment.amount", String.valueOf(order.getTotalAmount()));
        obs.lowCardinalityKeyValue("payment.currency", "USD");

        if ("error".equals(scenario)) {
            simulateWork(100);
            PaymentDeclinedException ex = new PaymentDeclinedException(
                    "insufficient_funds", "card_declined");
            obs.lowCardinalityKeyValue("payment.decline_reason", "insufficient_funds");
            obs.lowCardinalityKeyValue("payment.error_code", "card_declined");
            obs.error(ex);
            throw ex;
        }

        // Happy and slow paths
        long delay = "slow".equals(scenario) ? 200 : 150;
        simulateWork(delay);

        String transactionId = UUID.randomUUID().toString();
        obs.highCardinalityKeyValue("payment.transaction_id", transactionId);
        obs.lowCardinalityKeyValue("payment.status", "success");
        return new PaymentResult(transactionId, order.getTotalAmount());
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
