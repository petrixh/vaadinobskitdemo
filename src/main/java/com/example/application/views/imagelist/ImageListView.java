package com.example.application.views.imagelist;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.application.data.service.LeakyProtoService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Image List")
@Route(value = "image-list", layout = MainLayout.class)
public class ImageListView extends Main implements HasComponents, HasStyle {

    private final Button purgeButton;
    private OrderedList imageContainer;
    public static final String LEAKY_BYTES = "plants";

    private LeakyProtoService leakyProtoService;

    public ImageListView(@Autowired LeakyProtoService leakyProtoService) {
        this.leakyProtoService = leakyProtoService;

        int leakyListCount = leakyProtoService.getLeakyListCount();
        purgeButton = new Button("Purge button...", e -> {
            leakyProtoService.clearLeakyList();

            //Free the leaked ram...
            Runtime.getRuntime().gc();
            updatePurgeButtonCaption(leakyProtoService.getLeakyListCount());
        });
        updatePurgeButtonCaption(leakyListCount);

        add(new Button("Leak some RAM Sync ", e -> {
            leakyProtoService.addToLeakyListSynchronized();
            updatePurgeButtonCaption(leakyProtoService.getLeakyListCount());
        }));

        add(new Button("Leak some RAM Async ", e -> {
            leakyProtoService.addToLeakyListAsync().whenCompleteAsync((integer, throwable) -> {
                if (throwable == null) {
                    e.getSource().getUI().get().access(() -> updatePurgeButtonCaption(integer));
                }
            });
        }));
        add(purgeButton);

        constructUI();

        imageContainer.add(new ImageListViewCard("Snow mountains under stars",
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?ixid=MXwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHw%3D&ixlib=rb-1.2" +
                ".1&auto=format&fit=crop&w=750&q=80"));
        imageContainer.add(new ImageListViewCard("Snow covered mountain",
            "https://images.unsplash.com/photo-1512273222628-4daea6e55abb?ixid=MXwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHw%3D&ixlib=rb-1.2" +
                ".1&auto=format&fit=crop&w=750&q=80"));
        imageContainer.add(new ImageListViewCard("River between mountains",
            "https://images.unsplash.com/photo-1536048810607-3dc7f86981cb?ixid=MXwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHw%3D&ixlib=rb-1.2" +
                ".1&auto=format&fit=crop&w=375&q=80"));
        imageContainer.add(new ImageListViewCard("Milky way on mountains",
            "https://images.unsplash.com/photo-1515705576963-95cad62945b6?ixlib=rb-1.2" +
                ".1&ixid=MXwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHw%3D&auto=format&fit=crop&w=750&q=80"));
        imageContainer.add(new ImageListViewCard("Mountain with fog",
            "https://images.unsplash.com/photo-1513147122760-ad1d5bf68cdb?ixid=MXwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHw%3D&ixlib=rb-1.2" +
                ".1&auto=format&fit=crop&w=1000&q=80"));
        imageContainer.add(new ImageListViewCard("Mountain at night",
            "https://images.unsplash.com/photo-1562832135-14a35d25edef?ixid=MXwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHw%3D&ixlib=rb-1.2" +
                ".1&auto=format&fit=crop&w=815&q=80"));

    }

    private void updatePurgeButtonCaption(int leakyListCount) {
        purgeButton.setText("Purge " + leakyListCount + " leaks");
    }

    private void constructUI() {
        addClassNames("image-list-view", "max-w-screen-lg", "mx-auto", "pb-l", "px-l");

        HorizontalLayout container = new HorizontalLayout();
        container.addClassNames("items-center", "justify-between");

        VerticalLayout headerContainer = new VerticalLayout();
        H2 header = new H2("Beautiful photos");
        header.addClassNames("mb-0", "mt-xl", "text-3xl");
        Paragraph description = new Paragraph("Royalty free photos and pictures, courtesy of Unsplash");
        description.addClassNames("mb-xl", "mt-0", "text-secondary");
        headerContainer.add(header, description);

        Select<String> sortBy = new Select<>();
        sortBy.setLabel("Sort by");
        sortBy.setItems("Popularity", "Newest first", "Oldest first");
        sortBy.setValue("Popularity");

        imageContainer = new OrderedList();
        imageContainer.addClassNames("gap-m", "grid", "list-none", "m-0", "p-0");

        container.add(header, sortBy);
        add(container, imageContainer);
    }

}