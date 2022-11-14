package com.example.application.views.about;

import com.example.application.data.entity.SamplePerson;
import com.example.application.data.service.LeakyProtoInterfaceImpl;
import com.example.application.data.service.LeakyProtoService;
import com.example.application.data.service.SamplePersonService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.model.VerticalAlign;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
public class AboutView extends VerticalLayout {

    private static Logger logger = LoggerFactory.getLogger(AboutView.class);

    public static final String CPUCOOKER = "cpucooker";
    private final SamplePersonService samplePersonService;
    private volatile AtomicBoolean cpuCookerEnabled = new AtomicBoolean(true);
    private NumberField batchAdd = new NumberField("Batch add sample people");

    public AboutView(@Autowired SamplePersonService samplePersonService) {
        this.samplePersonService = samplePersonService;

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

        Button addUsersButton = new Button("Add users", e -> addUsers(batchAdd.getValue().intValue()));
        HorizontalLayout batchAddLayout = new HorizontalLayout();
        batchAddLayout.setDefaultVerticalComponentAlignment(Alignment.END);
        batchAddLayout.add(batchAdd, addUsersButton);
        add(batchAddLayout);
        batchAdd.setValue(100000d);

    }

    private void addUsers(int count) {

        long start = System.currentTimeMillis();
        int threads = LeakyProtoInterfaceImpl.getSystemThreadCount() - 1;

        logger.info("Adding {} sample persons using {} threads", count, threads);

        IntStream.range(0, threads).parallel().forEach(thread -> {

            final ArrayList<SamplePerson> persons = new ArrayList<>();
            int countToCreate = count / threads;

            IntStream.range(0, countToCreate).forEach(value -> {
                SamplePerson samplePerson = new SamplePerson();
                samplePerson.setFirstName("Sample " + value);
                samplePerson.setFirstName("Clone " + value);
                samplePerson.setEmail("sample" + value + "@example.com");
                samplePerson.setOccupation("Sampler");
                samplePerson.setPhone("1231231234");
                persons.add(samplePerson);
            });

            //This is what actually takes time... hence threads..
            samplePersonService.addSamplePersons(persons);

        });

        String format = String.format("Added %d sample persons in %d ms", count, (System.currentTimeMillis() - start));
        logger.info(format);

        Notification.show(format);
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
