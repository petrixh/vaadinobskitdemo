package com.example.application.views.helloworld;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@PageTitle("Hello World")
@Route(value = "hello", layout = MainLayout.class)
public class HelloWorldView extends VerticalLayout {

    private TextField name;
    private Button sayHello;

    public HelloWorldView() {
        add(new H1("Custom span/attribute example"));
        add(new Text("Look for a span 'My Custom Span' and/or a span attribute: 'hello.value' with the value from the TextField")); 
        name = new TextField("Your name");
        sayHello = new Button("Say hello");
        sayHello.addClickListener(e -> {

            Tracer trace = GlobalOpenTelemetry.getTracer("app-instrumentation", "1.0.0"); 
            final Span span = trace.spanBuilder("My Custom Span").startSpan();

            try{
                span.setAttribute("hello.value", name.getValue());
                Notification.show("Hello " + name.getValue());
            } finally {
                span.end();
            }
        });
        sayHello.addClickShortcut(Key.ENTER);

        HorizontalLayout hl = new HorizontalLayout(name, sayHello); 
        setMargin(true);
        hl.setVerticalComponentAlignment(Alignment.END, name, sayHello);
        add(hl);
    }

}
