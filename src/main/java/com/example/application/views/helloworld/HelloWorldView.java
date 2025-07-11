package com.example.application.views.helloworld;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Hello World")
@Route(value = "hello", layout = MainLayout.class)
public class HelloWorldView extends HorizontalLayout {

    private TextField name;
    private Button sayHello;

    public HelloWorldView() {
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

        setMargin(true);
        setVerticalComponentAlignment(Alignment.END, name, sayHello);

        add(name, sayHello);
    }

}
