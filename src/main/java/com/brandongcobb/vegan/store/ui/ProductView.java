package com.brandongcobb.vegan.store.ui;

import com.brandongcobb.vegan.store.domain.Category;
import com.brandongcobb.vegan.store.domain.Product;
import com.brandongcobb.vegan.store.service.StoreService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;

@Route(value = "admin/products", layout = MainLayout.class)
@PageTitle("Products | Vegan Store Admin")
public class ProductView extends VerticalLayout {

    private final StoreService service;
    private final Grid<Product> grid = new Grid<>(Product.class, false);
    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final NumberField priceField = new NumberField("Price");
    private final IntegerField stockField = new IntegerField("Stock");
    private final ComboBox<Category> categoryCombo = new ComboBox<>("Category");
    private final Button save   = new Button("Save");
    private final Button delete = new Button("Delete");
    private final Button clear  = new Button("Clear");
    private final Binder<Product> binder = new Binder<>(Product.class);
    private Product currentProduct;

    @Autowired
    public ProductView(StoreService service) {
        this.service = service;
        setSizeFull();
        configureGrid();
        configureForm();
        buildLayout();
        updateGrid();
        clearForm();
    }

    private void configureGrid() {
        grid.addColumn(Product::getId)
            .setHeader("ID").setSortable(true).setAutoWidth(true);
        grid.addColumn(Product::getName)
            .setHeader("Name").setSortable(true).setAutoWidth(true);
        grid.addColumn(p -> Optional.ofNullable(p.getCategory())
                                   .map(Category::getName).orElse(""))
            .setHeader("Category").setAutoWidth(true);
        grid.addColumn(Product::getPrice)
            .setHeader("Price").setSortable(true).setAutoWidth(true);
        grid.addColumn(Product::getStock)
            .setHeader("Stock").setSortable(true).setAutoWidth(true);
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));
        grid.setSizeFull();
    }

    private void configureForm() {
        categoryCombo.setItemLabelGenerator(Category::getName);

        binder.forField(nameField)
              .asRequired("Name is required")
              .withValidator(n -> n.length() <= 150, "Max 150 chars")
              .bind(Product::getName, Product::setName);

        binder.forField(descriptionField)
              .withValidator(d -> d.length() <= 1000, "Max 1000 chars")
              .bind(Product::getDescription, Product::setDescription);

        // Two-lambda converter Double <-> BigDecimal
        binder.forField(priceField)
              .withConverter(
                  dbl -> dbl != null ? BigDecimal.valueOf(dbl) : null,
                  bd  -> bd != null ? bd.doubleValue() : 0.0
              )
              .asRequired("Price required")
              .withValidator(
                  price -> price != null && price.compareTo(BigDecimal.valueOf(0.01)) >= 0,
                  "Must be ≥ 0.01"
              )
              .bind(Product::getPrice, Product::setPrice);

        binder.forField(stockField)
              .asRequired("Stock required")
              .withValidator(new IntegerRangeValidator("Must be ≥ 0", 0, null))
              .bind(Product::getStock, Product::setStock);

        binder.forField(categoryCombo)
              .asRequired("Category required")
              .bind(Product::getCategory, Product::setCategory);

        save.addClickListener(e -> saveProduct());
        delete.addClickListener(e -> confirmDelete());
        clear.addClickListener(e -> clearForm());
    }

    private void buildLayout() {
        H2 header = new H2("Product Management");

        FormLayout form = new FormLayout();
        form.add(nameField, descriptionField, priceField, stockField, categoryCombo);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0",   1),
            new FormLayout.ResponsiveStep("600px",2)
        );

        HorizontalLayout buttons = new HorizontalLayout(save, delete, clear);
        buttons.setPadding(true);

        VerticalLayout formContainer = new VerticalLayout(header, form, buttons);
        formContainer.setWidth("420px");
        formContainer.setPadding(true);

        HorizontalLayout main = new HorizontalLayout(grid, formContainer);
        main.setSizeFull();
        main.setFlexGrow(2, grid);
        main.setFlexGrow(1, formContainer);

        add(main);
    }

    private void edit(Product p) {
        if (p == null) {
            clearForm();
            return;
        }
        currentProduct = service.findProductById(p.getId()).orElse(p);
        binder.readBean(currentProduct);
        delete.setEnabled(true);
        save.setText("Update");
    }

    private void saveProduct() {
        try {
            if (currentProduct == null) {
                currentProduct = new Product();
            }
            binder.writeBean(currentProduct);
            service.saveProduct(currentProduct);
            Notification.show("Product saved", 2000, Notification.Position.MIDDLE);
            updateGrid();
            clearForm();
        } catch (Exception ex) {
            Notification.show("Please fix validation errors", 3000, Notification.Position.MIDDLE);
        }
    }

    private void confirmDelete() {
        if (currentProduct == null || currentProduct.getId() == null) {
            Notification.show("No product selected", 2000, Notification.Position.MIDDLE);
            return;
        }
        ConfirmDialog dlg = new ConfirmDialog(
            "Delete Product",
            "Delete \"" + currentProduct.getName() + "\"?",
            "Delete", evt -> {
                service.deleteProduct(currentProduct.getId());
                Notification.show("Product deleted", 2000, Notification.Position.MIDDLE);
                updateGrid();
                clearForm();
            },
            "Cancel", e -> {}
        );
        dlg.open();
    }

    private void clearForm() {
        currentProduct = null;
        binder.readBean(new Product());
        grid.deselectAll();
        delete.setEnabled(false);
        save.setText("Save");
    }

    private void updateGrid() {
        grid.setItems(service.listProducts());
        categoryCombo.setItems(service.listCategories());
    }
}