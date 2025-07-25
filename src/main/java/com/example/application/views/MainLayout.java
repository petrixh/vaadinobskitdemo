package com.example.application.views;

import com.example.application.views.about.AboutView;
import com.example.application.views.dashboard.DashboardView;
import com.example.application.views.helloworld.HelloWorldView;
import com.example.application.views.imagelist.ImageListView;
import com.example.application.views.masterdetailopt.OptMasterDetailView;
import com.example.application.views.masterdetailslow.MasterDetailView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    private H2 viewTitle;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        H1 appName = new H1("KitsTest");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {

        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.CHART.create()));
        nav.addItem(new SideNavItem("Hello World", HelloWorldView.class, VaadinIcon.GLOBE.create()));
        nav.addItem(new SideNavItem("About", AboutView.class, VaadinIcon.FILE.create()));
        nav.addItem(new SideNavItem("Image List", ImageListView.class, VaadinIcon.PICTURE.create()));
        nav.addItem(new SideNavItem("Master-Detail", MasterDetailView.class, VaadinIcon.TABLE.create()));
        nav.addItem(new SideNavItem("Opt Master-Detail", OptMasterDetailView.class, VaadinIcon.TABLE.create()));

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
