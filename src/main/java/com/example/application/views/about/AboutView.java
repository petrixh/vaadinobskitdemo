package com.example.application.views.about;

import com.example.application.data.service.LeakyProtoInterfaceImpl;
import com.example.application.data.service.LeakyProtoService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
public class AboutView extends VerticalLayout {

    public static final String CPUCOOKER = "cpucooker";
    private volatile AtomicBoolean cpuCookerEnabled = new AtomicBoolean(true);

    public AboutView() {
        setSpacing(false);



        Image img = new Image("images/empty-plant.png", "placeholder plant");
        img.setWidth("200px");
        add(img);

        add(new H2("This place intentionally left empty"));
        add(new Paragraph("It’s a place where you can grow your own UI 🤗"));

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");

        add(new Button("Blow Up!", e -> {
            RuntimeException simulated_error = new RuntimeException("Simulated error");
            simulated_error.fillInStackTrace();
            throw simulated_error;
        }));

        add(new Button("Enable Cpu Cooker", e -> {
            enableCpuCooker();
        }));

        add(new Button("Disable Cpu Cooker", e -> {
            disableCpuCooker();
        }));

    }

    private void enableCpuCooker() {

        ExecutorService cpuCooker = getCpuCooker();
        if (cpuCooker.isShutdown() || cpuCooker.isTerminated()) {
            VaadinSession.getCurrent().setAttribute("cpucooker", null);
        }

        cpuCooker = getCpuCooker();

        int systemThreadCount = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < systemThreadCount - 1; i++) {
            cpuCooker.submit(() -> {

                long end = System.currentTimeMillis();
                end += 1000 * 60 * 3; // run for a max of 3 min... or flag...
                while (cpuCookerEnabled.get() && System.currentTimeMillis() < end) {
                    //heat things up...
                }
            });
        }
    }

    private void disableCpuCooker() {
        cpuCookerEnabled.set(false);
    }



    public ExecutorService getCpuCooker() {

        ExecutorService cpucooker = (ExecutorService) VaadinSession.getCurrent().getAttribute(CPUCOOKER);

        if (cpucooker == null) {
            int systemThreadCount = LeakyProtoInterfaceImpl.getSystemThreadCount();
            cpucooker = Executors.newFixedThreadPool(systemThreadCount - 1);

            VaadinSession.getCurrent().setAttribute(CPUCOOKER, cpucooker);
        }

        return cpucooker;
    }
}
