package com.brandongcobb.vegan.store.ui;

import com.brandongcobb.vegan.store.domain.Category;
import com.brandongcobb.vegan.store.service.StoreService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;

public class StoreLayout extends AppLayout {

  private final StoreService storeService;

  @Autowired
  public StoreLayout(StoreService storeService) {
    this.storeService = storeService;

    // 1) Header with hamburger toggle, logo, cart button
    DrawerToggle toggle = new DrawerToggle();
    H1 logo = new H1("Vegan Store"); // or “Shop Vegan Essentials”
    Button cart = new Button(VaadinIcon.CART.create(), e -> UI.getCurrent().navigate("store#cart"));

    HorizontalLayout header = new HorizontalLayout(toggle, logo, cart);
    header.expand(logo);
    header.setWidthFull();
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    addToNavbar(header);

    // 2) Drawer menu with categories
    VerticalLayout menu = new VerticalLayout();
    menu.setPadding(false);
    menu.setSpacing(false);
    for (Category c : storeService.listCategories()) {
      Button link = new Button(c.getName(),
        e -> UI.getCurrent().navigate("store?category=" + c.getId()));
      menu.add(link);
    }
    addToDrawer(menu);
  }
}
