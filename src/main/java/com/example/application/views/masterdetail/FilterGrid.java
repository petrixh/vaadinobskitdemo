package com.example.application.views.masterdetail;

import com.example.application.data.entity.SamplePerson;
import com.example.application.data.service.SamplePersonService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.LitRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilterGrid extends VerticalLayout {

    private final Grid<SamplePerson> grid = new Grid<>(SamplePerson.class, false);
    private FilterGridListener listener;
    private ComboBox<String> firstNameFilter;
    private ComboBox<String> lastNameFilter;
    private ComboBox<String> emailFilter;
    private boolean isFiltering = false;

    public FilterGrid(FilterGridListener listener) {
        setWidthFull();
        setHeightFull();
        setFilterGridListener(listener);
    }

    public void init(SamplePersonService serviceThatShouldNotBeHere){
        HorizontalLayout filters = new HorizontalLayout();
        filters.setDefaultVerticalComponentAlignment(Alignment.END);
        add(filters);

        add(grid);

        grid.removeAllColumns();

        // Configure Grid
        grid.addColumn("firstName").setAutoWidth(true);
        grid.addColumn("lastName").setAutoWidth(true);
        grid.addColumn("email").setAutoWidth(true);
        grid.addColumn("phone").setAutoWidth(true);
        grid.addColumn("dateOfBirth").setAutoWidth(true);
        grid.addColumn("occupation").setAutoWidth(true);
        LitRenderer<SamplePerson> importantRenderer = LitRenderer.<SamplePerson>of(
                "<vaadin-icon icon='vaadin:${item.icon}' style='width: var(--lumo-icon-size-s); height: var(--lumo-icon-size-s); color: ${item.color};'></vaadin-icon>")
            .withProperty("icon", important -> important.isImportant() ? "check" : "minus").withProperty("color",
                important -> important.isImportant()
                    ? "var(--lumo-primary-text-color)"
                    : "var(--lumo-disabled-text-color)");
        populateGrid();

        grid.addColumn(importantRenderer).setHeader("Important").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            onSelect(event.getValue());
        });

        createFirstNameFilter(filters, serviceThatShouldNotBeHere);
        createLastNameFilter(filters, serviceThatShouldNotBeHere);
        createEmailFilter(filters, serviceThatShouldNotBeHere);

        //Due to bugs (mostly in our own filter logic)
        // we also set the filters here once more... Fix JIRA-99887 or whatever..
        // Guess if each one of these will cause a dataprovider refresh... and a filter?
        firstNameFilter.setValue(null);
        lastNameFilter.setValue(null);
        emailFilter.setValue(null);

    }

    private void createFirstNameFilter(HorizontalLayout filters, SamplePersonService samplePersonService) {

        // Why use a text field and have to wait for results,
        // a populated combo will show you if you're getting
        // close as you type...
        firstNameFilter = new ComboBox<>("First Name");
        firstNameFilter.setAllowCustomValue(true);
        firstNameFilter.addCustomValueSetListener(comboBoxCustomValueSetEvent -> {
            ((ListDataProvider<String>)comboBoxCustomValueSetEvent.getSource().getDataProvider()).getItems().add(comboBoxCustomValueSetEvent.getDetail());
            comboBoxCustomValueSetEvent.getSource().getDataProvider().refreshAll();
            comboBoxCustomValueSetEvent.getSource().setValue(comboBoxCustomValueSetEvent.getDetail());
        });
        //We add the VCL early so we get more events
        firstNameFilter.addValueChangeListener(e -> onFilter());
        filters.add(firstNameFilter);
        //Set default value (will trigger a filter which will refresh the grid)
        firstNameFilter.setItems(new ListDataProvider<>(new ArrayList<>()));
        firstNameFilter.setValue("");

        List<SamplePerson> persons = samplePersonService.findAll();
        List<String> allFirstNames = persons.stream().map(person -> person.getFirstName()).collect(Collectors.toList());
        firstNameFilter.setItems(allFirstNames);

    }

    //Yes, we copy don't reuse...
    private void createLastNameFilter(HorizontalLayout filters, SamplePersonService samplePersonService) {

        // Why use a text field and have to wait for results,
        // a populated combo will show you if you're getting
        // close as you type...
        lastNameFilter = new ComboBox<>("Last Name");
        lastNameFilter.setAllowCustomValue(true);
        lastNameFilter.addCustomValueSetListener(comboBoxCustomValueSetEvent -> {
            ((ListDataProvider<String>)comboBoxCustomValueSetEvent.getSource().getDataProvider()).getItems().add(comboBoxCustomValueSetEvent.getDetail());
            comboBoxCustomValueSetEvent.getSource().getDataProvider().refreshAll();
            comboBoxCustomValueSetEvent.getSource().setValue(comboBoxCustomValueSetEvent.getDetail());
        });
        //We add the VCL early so we get more events
        lastNameFilter.addValueChangeListener(e -> onFilter());
        filters.add(lastNameFilter);
        //Set default value (will trigger a filter which will refresh the grid)
        lastNameFilter.setItems(new ListDataProvider<>(new ArrayList<>()));
        lastNameFilter.setValue("");

        List<SamplePerson> persons = samplePersonService.findAll();
        List<String> allFirstNames = persons.stream().map(person -> person.getLastName()).collect(Collectors.toList());
        lastNameFilter.setItems(allFirstNames);
    }

    //Yes, we copy don't reuse...
    private void createEmailFilter(HorizontalLayout filters, SamplePersonService samplePersonService) {

        // Why use a text field and have to wait for results,
        // a populated combo will show you if you're getting
        // close as you type...
        emailFilter = new ComboBox<>("Email");
        emailFilter.setAllowCustomValue(true);
        emailFilter.addCustomValueSetListener(comboBoxCustomValueSetEvent -> {
            ((ListDataProvider<String>)comboBoxCustomValueSetEvent.getSource().getDataProvider()).getItems().add(comboBoxCustomValueSetEvent.getDetail());
            comboBoxCustomValueSetEvent.getSource().getDataProvider().refreshAll();
            comboBoxCustomValueSetEvent.getSource().setValue(comboBoxCustomValueSetEvent.getDetail());
        });
        //We add the VCL early so we get more events
        emailFilter.addValueChangeListener(e -> onFilter());
        filters.add(emailFilter);
        //Set default value (will trigger a filter which will refresh the grid)
        emailFilter.setItems(new ListDataProvider<>(new ArrayList<>()));
        emailFilter.setValue("");

        List<SamplePerson> persons = samplePersonService.findAll();
        List<String> allFirstNames = persons.stream().map(person -> person.getEmail()).collect(Collectors.toList());
        emailFilter.setItems(allFirstNames);
    }

    private void onFilter() {
        //Refresh data in order to be sure we have recent data... (also slows things down)
        listener.requestRefresh();

        ListDataProvider<SamplePerson> dataProvider = (ListDataProvider) grid.getDataProvider();
        // a filter....
        dataProvider.setFilter(samplePerson -> {

            String firstNameFilterValue = firstNameFilter.getValue();
            String lastNameFilterValue = lastNameFilter.getValue();
            String emailFilterValue = emailFilter.getValue();

            boolean oldTimeOr = firstNameFilterValue == null && lastNameFilterValue == null && emailFilterValue == null;
            if(firstNameFilterValue != null){
                if(samplePerson.getFirstName() != null){
                    if(samplePerson.getFirstName().toLowerCase().startsWith(firstNameFilterValue.toLowerCase()) || samplePerson.getFirstName().equalsIgnoreCase(firstNameFilterValue)){
                        oldTimeOr = true;
                    }
                }
            }

            if(lastNameFilterValue != null){
                if(samplePerson.getLastName() != null){
                    if(samplePerson.getLastName().toLowerCase().startsWith(lastNameFilterValue.toLowerCase()) || samplePerson.getLastName().equalsIgnoreCase(lastNameFilterValue)){
                        oldTimeOr = true;
                    }
                }
            }

            if(emailFilterValue != null){
                if(samplePerson.getEmail() != null){
                    if(samplePerson.getEmail().toLowerCase().startsWith(emailFilterValue.toLowerCase()) || samplePerson.getEmail().equalsIgnoreCase(emailFilterValue)){
                        oldTimeOr = true;
                    }
                }
            }

            return oldTimeOr;
        });

    }

    protected void populateGrid() {
        if(listener != null){
            listener.requestRefresh();
        }
    }

    protected void onSelect(SamplePerson person){
        if(listener != null){
            listener.onSelect(person);
        }
    }

    public void setFilterGridListener(FilterGridListener listener){
        this.listener = listener; 
    }

    public void setItems(List<SamplePerson> persons) {
        grid.setItems(new ListDataProvider<>(persons));
        //Filter as we change the dataset...

        //More bugs due to awesome coding... yes these flags do exist..
        isFiltering = true;

        // Do you know why this pattern exists? ;) fixes ARIJ-63454
        try {
            onFilter();
        }finally {
            isFiltering = false;
        }
    }

    public boolean isFiltering(){
        return isFiltering;
    }

    public void refreshGrid(){
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    public interface FilterGridListener{

        public void requestRefresh();

        public void onSelect(SamplePerson person);

    }
}
