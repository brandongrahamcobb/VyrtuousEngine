package com.brandongcobb.vegan.store.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        // The button to toggle the drawer
        DrawerToggle toggle = new DrawerToggle();

        // Your app title
        H1 title = new H1("Vegan Store Admin");
        title.getStyle().set("margin", "0");

        // A simple MenuBar with a sign-out icon
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
        menuBar.addItem(VaadinIcon.SIGN_OUT.create(), e -> doLogout());

        // Layout the header
        HorizontalLayout header = new HorizontalLayout(toggle, title, menuBar);
        header.expand(title);
        header.setAlignItems(Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding", "0 1rem");

        addToNavbar(header);
    }

    private void createDrawer() {
        // Main navigation
        addToDrawer(
            new RouterLink("Products",   ProductView.class),
            new RouterLink("Categories", CategoryView.class)
            // add more links here as you add more views...
        );
    }

    private void doLogout() {
        // Trigger Spring logout endpoint, then redirect to /login
        UI.getCurrent().getPage().executeJs(
            "fetch($0, { method: 'POST' }).then(_ => window.location.href = '/login');",
            "logout"
        );
    }
}