package com.example.application.views.orderprocessing;

import com.example.application.data.entity.CustomerOrder;
import com.example.application.data.service.OrderProcessingService;
import com.example.application.data.service.OrderProcessingService.OrderResult;
import com.example.application.data.service.OrderProcessingService.StepResult;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

@PageTitle("Order Processing")
@Route(value = "order-processing", layout = MainLayout.class)
public class OrderProcessingView extends VerticalLayout {

    private final OrderProcessingService orderProcessingService;

    private final TextField customerName = new TextField("Customer Name");
    private final TextField customerEmail = new TextField("Email");
    private final TextField productName = new TextField("Product");
    private final NumberField quantity = new NumberField("Quantity");
    private final NumberField unitPrice = new NumberField("Unit Price ($)");

    private final VerticalLayout resultsArea = new VerticalLayout();
    private final Button happyBtn;
    private final Button slowBtn;
    private final Button errorBtn;

    public OrderProcessingView(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
        setMargin(true);
        setSpacing(true);

        // Header
        add(new H2("Order Processing Demo"));
        add(new Paragraph("Simulates a multi-step order workflow. Each scenario generates different " +
                "trace patterns in Grafana. Run a scenario, then inspect the traces to see " +
                "how observability helps diagnose real problems."));

        // Form with pre-filled demo data
        customerName.setValue("Jane Smith");
        customerEmail.setValue("jane@example.com");
        productName.setValue("Vaadin Pro License");
        quantity.setValue(2.0);
        quantity.setMin(1);
        quantity.setStep(1);
        unitPrice.setValue(499.00);

        HorizontalLayout formRow1 = new HorizontalLayout(customerName, customerEmail, productName);
        HorizontalLayout formRow2 = new HorizontalLayout(quantity, unitPrice);
        formRow1.setWidthFull();
        add(formRow1, formRow2);

        // Scenario buttons
        happyBtn = new Button("Happy Path", e -> runScenario("happy"));
        happyBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        slowBtn = new Button("Slow Path", e -> runScenario("slow"));
        slowBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_PRIMARY);

        errorBtn = new Button("Error Path", e -> runScenario("error"));
        errorBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttons = new HorizontalLayout(happyBtn, slowBtn, errorBtn);
        add(buttons);

        // Results area
        resultsArea.setPadding(false);
        resultsArea.setSpacing(true);
        add(resultsArea);

        // Trace hints
        add(buildTraceHints());
    }

    private void runScenario(String scenario) {
        setButtonsEnabled(false);
        resultsArea.removeAll();
        resultsArea.add(new Paragraph("Processing order..."));

        CustomerOrder order = new CustomerOrder();
        order.setCustomerName(customerName.getValue());
        order.setCustomerEmail(customerEmail.getValue());
        order.setProductName(productName.getValue());
        order.setQuantity(quantity.getValue().intValue());
        order.setUnitPrice(unitPrice.getValue());
        order.setTotalAmount(quantity.getValue() * unitPrice.getValue());

        try {
            OrderResult result = orderProcessingService.processOrder(order, scenario);
            showResults(result, scenario);
        } catch (Exception e) {
            resultsArea.removeAll();
            resultsArea.add(createResultBanner(false, "Unexpected error: " + e.getMessage(), 0));
        } finally {
            setButtonsEnabled(true);
        }
    }

    private void showResults(OrderResult result, String scenario) {
        resultsArea.removeAll();

        // Summary banner
        String summary = result.success()
                ? "Order fulfilled successfully"
                : "Order failed at: " + result.failureStep();
        resultsArea.add(createResultBanner(result.success(), summary, result.totalDurationMs()));

        // Step-by-step breakdown
        H3 stepsHeader = new H3("Processing Steps");
        resultsArea.add(stepsHeader);

        for (StepResult step : result.steps()) {
            resultsArea.add(createStepRow(step));
        }

        // Notification
        if (result.success()) {
            Notification.show("Order " + result.orderId() + " fulfilled in " + result.totalDurationMs() + "ms",
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show("Order failed: " + result.failureReason(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Div createResultBanner(boolean success, String message, long durationMs) {
        Div banner = new Div();
        banner.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BorderRadius.MEDIUM);
        banner.getStyle()
                .set("background-color", success
                        ? "var(--lumo-success-color-10pct)"
                        : "var(--lumo-error-color-10pct)")
                .set("border", "1px solid " + (success
                        ? "var(--lumo-success-color)"
                        : "var(--lumo-error-color)"));

        Span icon = new Span(success ? "\u2705" : "\u274c");
        Span text = new Span(" " + message);
        text.getStyle().set("font-weight", "600");
        Span duration = new Span(" (" + durationMs + "ms)");
        duration.getStyle().set("color", "var(--lumo-secondary-text-color)");

        banner.add(icon, text, duration);
        return banner;
    }

    private Div createStepRow(StepResult step) {
        Div row = new Div();
        row.addClassNames(LumoUtility.Padding.SMALL, LumoUtility.BorderRadius.SMALL);
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        // Status icon
        Span icon = new Span(step.success() ? "\u2705" : "\u274c");

        // Step name
        Span name = new Span(step.name());
        name.getStyle().set("font-weight", "500").set("min-width", "120px");

        // Duration with color coding
        Span duration = new Span(step.durationMs() + "ms");
        String durationColor;
        if (!step.success()) {
            durationColor = "var(--lumo-error-text-color)";
        } else if (step.durationMs() > 1000) {
            durationColor = "var(--lumo-warning-text-color, orange)";
        } else {
            durationColor = "var(--lumo-success-text-color)";
        }
        duration.getStyle().set("color", durationColor).set("font-family", "monospace").set("min-width", "80px");

        // Detail
        Span detail = new Span(step.detail());
        detail.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        row.add(icon, name, duration, detail);
        return row;
    }

    private VerticalLayout buildTraceHints() {
        VerticalLayout hints = new VerticalLayout();
        hints.setPadding(false);
        hints.setSpacing(false);
        hints.add(new H3("What to Look for in Traces"));

        Details happyHint = new Details("Happy Path traces",
                new Paragraph("Clean waterfall with ~480ms total. The whole thing hangs under the " +
                        "Observability Kit's 'vaadin.rpc' span for the button click. The parent span " +
                        "'order.process' contains child spans for each step (order.validate, " +
                        "order.fraud_check, order.reserve_inventory, order.process_payment). Between each " +
                        "step, you'll see 'vaadin.db.query' spans from the status updates. All spans are " +
                        "green/OK."));
        happyHint.setOpened(false);

        Details slowHint = new Details("Slow Path traces",
                new Paragraph("The 'order.fraud_check' span dominates the waterfall at ~5 seconds. " +
                        "Expand it to see two child spans: 'fraud_check.call_api' (3s, marked ERROR with " +
                        "a timeout event) and 'fraud_check.retry' (2s, succeeds). " +
                        "The 'order.reserve_inventory' span shows many small child 'vaadin.db.query' spans " +
                        "from the N+1 query pattern -- each individual lookup is visible, with its SQL in " +
                        "the 'db.statement' attribute. " +
                        "Total trace duration ~6 seconds."));
        slowHint.setOpened(false);

        Details errorHint = new Details("Error Path traces",
                new Paragraph("The 'order.process_payment' span is marked red/ERROR. Click it to see " +
                        "the recorded PaymentDeclinedException with 'insufficient_funds' as the decline reason. " +
                        "The parent 'order.process' span also shows ERROR status with attribute " +
                        "'order.failure_step=Payment'. Notice that the trace is shorter because " +
                        "processing stopped at the payment step."));
        errorHint.setOpened(false);

        hints.add(happyHint, slowHint, errorHint);
        return hints;
    }

    private void setButtonsEnabled(boolean enabled) {
        happyBtn.setEnabled(enabled);
        slowBtn.setEnabled(enabled);
        errorBtn.setEnabled(enabled);
    }
}
