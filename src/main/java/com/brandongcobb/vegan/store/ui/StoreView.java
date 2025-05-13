package com.brandongcobb.vegan.store.ui;

import com.brandongcobb.vegan.store.domain.Product;
import com.brandongcobb.vegan.store.repo.CustomerRepository;
import com.brandongcobb.vegan.store.service.OrderService;
import com.brandongcobb.vegan.store.service.StoreService;
import com.brandongcobb.vegan.store.api.dto.OrderLineRequest;
import com.brandongcobb.vegan.store.api.dto.OrderResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A storefront view for browsing products, adding to cart, and checking out.
 */
@Route("store")
@PageTitle("Store | Vegan Store")
public class StoreView extends VerticalLayout {

    private final StoreService storeService;
    private final OrderService orderService;
    private final CustomerRepository customerRepository;

    private final Grid<Product> productGrid = new Grid<>(Product.class, false);
    private final Grid<CartItem> cartGrid = new Grid<>(CartItem.class, false);
    private final Button checkoutButton = new Button("Checkout");
    private final Button clearCartButton = new Button("Clear Cart");

    private final java.util.Map<Product, Integer> cart = new java.util.LinkedHashMap<>();

    public StoreView(StoreService storeService,
                     OrderService orderService,
                     CustomerRepository customerRepository) {
        this.storeService = storeService;
        this.orderService = orderService;
        this.customerRepository = customerRepository;

        setSizeFull();
        configureProductGrid();
        configureCartGrid();
        buildLayout();
        updateProductGrid();
        updateCartGrid();
    }

    private void configureProductGrid() {
        productGrid.addColumn(Product::getName).setHeader("Name").setAutoWidth(true);
        productGrid.addColumn(Product::getDescription).setHeader("Description").setAutoWidth(true);
        productGrid.addColumn(Product::getPrice).setHeader("Price").setAutoWidth(true);
        productGrid.addColumn(Product::getStock).setHeader("Stock").setAutoWidth(true);
        productGrid.addComponentColumn(product -> {
            Button add = new Button("Add to Cart", e -> addToCart(product));
            add.setEnabled(product.getStock() > 0);
            return add;
        }).setHeader("Action");
        productGrid.setSizeFull();
    }

    private void configureCartGrid() {
        cartGrid.addColumn(item -> item.product.getName()).setHeader("Name").setAutoWidth(true);
        cartGrid.addColumn(item -> item.quantity).setHeader("Qty").setAutoWidth(true);
        cartGrid.addComponentColumn(item -> {
            Button remove = new Button("Remove", e -> removeFromCart(item.product));
            return remove;
        }).setHeader("Action");
        cartGrid.setSizeFull();
    }

    private void buildLayout() {
        H2 title = new H2("Vegan Storefront");
        title.getStyle().set("margin-bottom", "0");

        HorizontalLayout grids = new HorizontalLayout(productGrid, cartGrid);
        grids.setSizeFull();
        grids.setFlexGrow(2, productGrid);
        grids.setFlexGrow(1, cartGrid);

        clearCartButton.addClickListener(e -> {
            cart.clear();
            updateCartGrid();
        });
        checkoutButton.addClickListener(e -> doCheckout());
        HorizontalLayout actions = new HorizontalLayout(clearCartButton, checkoutButton);

        add(title, grids, actions);
        expand(grids);
    }

    private void updateProductGrid() {
        productGrid.setItems(storeService.listProducts());
    }

    private void updateCartGrid() {
        var items = cart.entrySet().stream()
                .map(e -> new CartItem(e.getKey(), e.getValue()))
                .toList();
        cartGrid.setItems(items);
    }

    private void addToCart(Product product) {
        cart.merge(product, 1, Integer::sum);
        updateCartGrid();
    }

    private void removeFromCart(Product product) {
        cart.remove(product);
        updateCartGrid();
    }

    private void doCheckout() {
        if (cart.isEmpty()) {
            Notification.show("Cart is empty", 2000, Notification.Position.MIDDLE);
            return;
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        var orderItems = cart.entrySet().stream()
                .map(e -> new OrderLineRequest(e.getKey().getId(), e.getValue()))
                .toList();
        var request = new com.brandongcobb.vegan.store.api.dto.PlaceOrderRequest(customer.getId(), orderItems);
        try {
            OrderResponse response = orderService.placeOrder(request);
            Notification.show("Order placed (#" + response.orderId() + ")", 3000, Notification.Position.MIDDLE);
            cart.clear();
            updateProductGrid();
            updateCartGrid();
        } catch (Exception ex) {
            Notification.show("Checkout failed: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private static class CartItem {
        private final Product product;
        private final int quantity;

        CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }
}