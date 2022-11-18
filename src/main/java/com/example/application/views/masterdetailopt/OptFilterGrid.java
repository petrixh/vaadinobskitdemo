package com.example.application.views.masterdetailopt;

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

public class OptFilterGrid extends VerticalLayout {

    private final Grid<SamplePerson> grid = new Grid<>(SamplePerson.class, false);
    private FilterGridListener listener;
    private ComboBox<String> firstNameFilter;
    private ComboBox<String> lastNameFilter;
    private ComboBox<String> emailFilter;
    private boolean isFiltering = false;

    public OptFilterGrid(FilterGridListener listener) {
        setWidthFull();
        setHeightFull();
        setFilterGridListener(listener);
    }

    public void init() {
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
                "<vaadin-icon icon='vaadin:${item.icon}' style='width: var(--lumo-icon-size-s); height: var(--lumo-icon-size-s); color: ${item" +
                    ".color};'></vaadin-icon>")
            .withProperty("icon", important -> important.isImportant() ? "check" : "minus").withProperty("color",
                important -> important.isImportant()
                    ? "var(--lumo-primary-text-color)"
                    : "var(--lumo-disabled-text-color)");
        // populateGrid();

        grid.addColumn(importantRenderer).setHeader("Important").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            onSelect(event.getValue());
        });

        createFirstNameFilter(filters);
        createLastNameFilter(filters);
        createEmailFilter(filters);

    }

    private ComboBox<String> createFilter(String caption) {
        ComboBox<String> filter = new ComboBox<>(caption);
        filter.setAllowCustomValue(true);
        filter.addCustomValueSetListener(comboBoxCustomValueSetEvent -> {
            ((ListDataProvider<String>) comboBoxCustomValueSetEvent.getSource().getDataProvider()).getItems().add(comboBoxCustomValueSetEvent.getDetail());
            comboBoxCustomValueSetEvent.getSource().getDataProvider().refreshAll();
            comboBoxCustomValueSetEvent.getSource().setValue(comboBoxCustomValueSetEvent.getDetail());
        });
        filter.addValueChangeListener(e -> onFilter());
        return filter;
    }

    private void createFirstNameFilter(HorizontalLayout filters) {
        firstNameFilter = createFilter("First Name");
        filters.add(firstNameFilter);
    }


    private void createLastNameFilter(HorizontalLayout filters) {
        lastNameFilter = createFilter("Last Name");
        filters.add(lastNameFilter);
    }

    private void createEmailFilter(HorizontalLayout filters) {
        emailFilter = createFilter("Email");
        filters.add(emailFilter);
    }

    private void onFilter() {
        listener.onFilter();
    }

    protected void populateGrid() {
        if (listener != null) {
            listener.requestRefresh();
        }
    }

    protected void onSelect(SamplePerson person) {
        if (listener != null) {
            listener.onSelect(person);
        }
    }

    public void setFilterGridListener(FilterGridListener listener) {
        this.listener = listener;
    }

    public void setItems(ListDataProvider<SamplePerson> persons) {
        grid.setItems(persons);

    }

    public void refreshGrid() {
        grid.select(null);
        listener.requestRefresh();
    }

    public FilterValues getFilterValues() {
        FilterValues filterValues = new FilterValues();
        filterValues.setFirstNameFilter(firstNameFilter.getValue());
        filterValues.setLastNameFilter(lastNameFilter.getValue());
        filterValues.setEmailFilter(emailFilter.getValue());
        return filterValues;
    }

    public void setFirstNameFilterItems(List<String> personList) {
        firstNameFilter.setItems(personList);
    }

    public void setLastNameFilterItems(List<String> lastNames) {
        lastNameFilter.setItems(lastNames);
    }

    public void setEmailNameFilterItems(List<String> emailsList) {
        emailFilter.setItems(emailsList);
    }

    public interface FilterGridListener {

        void requestRefresh();

        void onFilter();

        void onSelect(SamplePerson person);

    }

    public static class FilterValues {

        private String firstNameFilter;
        private String lastNameFilter;
        private String emailFilter;

        public String getFirstNameFilter() {
            return firstNameFilter;
        }

        public void setFirstNameFilter(String firstNameFilter) {
            this.firstNameFilter = firstNameFilter;
        }

        public String getLastNameFilter() {
            return lastNameFilter;
        }

        public void setLastNameFilter(String lastNameFilter) {
            this.lastNameFilter = lastNameFilter;
        }

        public String getEmailFilter() {
            return emailFilter;
        }

        public void setEmailFilter(String emailFilter) {
            this.emailFilter = emailFilter;
        }
    }
}
