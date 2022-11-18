package com.example.application.views.masterdetailopt;

import com.example.application.data.entity.SamplePerson;
import com.example.application.data.service.SamplePersonService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@PageTitle("Optimized Master-Detail")
@Route(value = "opt-master-detail/:samplePersonID?/:action?(edit)", layout = MainLayout.class)
@Uses(Icon.class)
public class OptMasterDetailView extends Div implements BeforeEnterObserver {

    private final String SAMPLEPERSON_ID = "samplePersonID";
    private final String SAMPLEPERSON_EDIT_ROUTE_TEMPLATE = "master-detail/%s/edit";

    private TextField firstName;
    private TextField lastName;
    private TextField email;
    private TextField phone;
    private DatePicker dateOfBirth;
    private TextField occupation;
    private Checkbox important;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<SamplePerson> binder;

    private SamplePerson samplePerson;

    private final SamplePersonService samplePersonService;

    private OptFilterGrid optFilterGrid;
    private ListDataProvider<SamplePerson> personsListDataProvider;


    @Autowired
    public OptMasterDetailView(SamplePersonService samplePersonService) {
        this.samplePersonService = samplePersonService;
        addClassNames("master-detail-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();


        optFilterGrid = new OptFilterGrid(new OptFilterGrid.FilterGridListener() {
            @Override
            public void requestRefresh() {
                populateGrid();

            }

            @Override
            public void onFilter() {
                OptMasterDetailView.this.onFilter();
            }

            @Override
            public void onSelect(SamplePerson person) {
                if (person != null) {
                    UI.getCurrent().navigate(String.format(SAMPLEPERSON_EDIT_ROUTE_TEMPLATE, person.getId()));
                } else {
                    clearForm();
                    UI.getCurrent().navigate(OptMasterDetailView.class);
                }
            }
        });

        optFilterGrid.init();
        populateGrid();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);
        add(splitLayout);

        // Configure Form
        binder = new BeanValidationBinder<>(SamplePerson.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {

                if (this.samplePerson == null) {
                    this.samplePerson = new SamplePerson();
                }
                binder.writeBean(this.samplePerson);
                samplePersonService.update(this.samplePerson);
                clearForm();
                refreshGrid();
                Notification.show("SamplePerson details stored.");
                UI.getCurrent().navigate(OptMasterDetailView.class);
            } catch (ValidationException validationException) {
                Notification.show("An exception happened while trying to store the samplePerson details.");
            }
        });

    }

    private void populateGrid() {


        //Let's keep it fair and not lazy load... just optimize the UI.
        List<SamplePerson> personList = samplePersonService.findAll();
        personsListDataProvider = new ListDataProvider<>(personList);
        optFilterGrid.setItems(personsListDataProvider);

        List<String> firstNames = personList.stream().map(person -> person.getFirstName()).collect(Collectors.toList());
        List<String> lastNames = personList.stream().map(person -> person.getLastName()).collect(Collectors.toList());
        List<String> emailsList = personList.stream().map(person -> person.getEmail()).collect(Collectors.toList());

        optFilterGrid.setFirstNameFilterItems(firstNames);
        optFilterGrid.setLastNameFilterItems(lastNames);
        optFilterGrid.setEmailNameFilterItems(emailsList);


        //Lazy loading...
//        grid.setItems(query -> samplePersonService.list(
//                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
//                .stream());

        //Non-lazy loading
//        optFilterGrid.setItems(samplePersonService.findAll());
    }

    public void onFilter() {
        OptFilterGrid.FilterValues filterValues = optFilterGrid.getFilterValues();

        personsListDataProvider.setFilter(
            new PersonFilter(
                filterValues.getFirstNameFilter(),
                filterValues.getLastNameFilter(),
                filterValues.getEmailFilter()));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<UUID> samplePersonId = event.getRouteParameters().get(SAMPLEPERSON_ID).map(UUID::fromString);
        if (samplePersonId.isPresent()) {
            Optional<SamplePerson> samplePersonFromBackend = samplePersonService.get(samplePersonId.get());
            if (samplePersonFromBackend.isPresent()) {
                populateForm(samplePersonFromBackend.get());
            } else {
                Notification.show(
                    String.format("The requested samplePerson was not found, ID = %s", samplePersonId.get()), 3000,
                    Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(OptMasterDetailView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        firstName = new TextField("First Name");
        lastName = new TextField("Last Name");
        email = new TextField("Email");
        phone = new TextField("Phone");
        dateOfBirth = new DatePicker("Date Of Birth");
        occupation = new TextField("Occupation");
        important = new Checkbox("Important");
        formLayout.add(firstName, lastName, email, phone, dateOfBirth, occupation, important);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(optFilterGrid);
    }

    private void refreshGrid() {
        optFilterGrid.refreshGrid();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(SamplePerson value) {
        this.samplePerson = value;
        binder.readBean(this.samplePerson);

    }

    // A shame we don't have the handy simple filter helper API's anymore
    // but this is a simplified version of the examples in our docs...
    private static class PersonFilter implements SerializablePredicate<SamplePerson> {

        private String firstName;
        private String lastName;
        private String email;

        public PersonFilter(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        @Override
        public boolean test(SamplePerson person) {
            boolean matchesFullName = matches(person.getFirstName(), firstName);
            boolean matchesProfession = matches(person.getLastName(),
                lastName);
            boolean matchesEmail = matches(person.getEmail(), email);

            return matchesFullName && matchesEmail && matchesProfession;
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty()
                || value.toLowerCase().contains(searchTerm.toLowerCase());
        }

    }
}
