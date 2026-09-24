package com.example.application.data.service;

import com.example.application.data.entity.CustomerOrder;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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

    @WithSpan("order.process_payment")
    public PaymentResult processPayment(CustomerOrder order, String scenario) {
        Span span = Span.current();
        span.setAttribute("payment.gateway", "stripe");
        span.setAttribute("payment.amount", order.getTotalAmount());
        span.setAttribute("payment.currency", "USD");

        if ("error".equals(scenario)) {
            simulateWork(100);
            PaymentDeclinedException ex = new PaymentDeclinedException(
                    "insufficient_funds", "card_declined");
            span.setStatus(StatusCode.ERROR, "Payment declined");
            span.recordException(ex);
            span.setAttribute("payment.decline_reason", "insufficient_funds");
            span.setAttribute("payment.error_code", "card_declined");
            throw ex;
        }

        // Happy and slow paths
        long delay = "slow".equals(scenario) ? 200 : 150;
        simulateWork(delay);

        String transactionId = UUID.randomUUID().toString();
        span.setAttribute("payment.transaction_id", transactionId);
        span.setAttribute("payment.status", "success");
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
