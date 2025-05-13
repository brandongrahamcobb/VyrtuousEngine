package com.brandongcobb.vegan.store.ui;

import com.brandongcobb.vegan.store.domain.Category;
import com.brandongcobb.vegan.store.service.StoreService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "admin/categories", layout = MainLayout.class)
@PageTitle("Categories | Vegan Store Admin")
public class CategoryView extends VerticalLayout {

    private final StoreService service;

    private final Grid<Category> grid = new Grid<>(Category.class, false);
    private final TextField nameField = new TextField("Category Name");

    private final Button save   = new Button("Save");
    private final Button delete = new Button("Delete");
    private final Button clear  = new Button("Clear");

    private final Binder<Category> binder = new Binder<>(Category.class);
    private Category currentCategory;

    @Autowired
    public CategoryView(StoreService service) {
        this.service = service;
        setSizeFull();
        configureGrid();
        configureForm();
        buildLayout();
        updateGrid();
        clearForm();  // start in “new” mode
    }

    private void configureGrid() {
        grid.addColumn(Category::getId)
            .setHeader("ID").setSortable(true).setAutoWidth(true);
        grid.addColumn(Category::getName)
            .setHeader("Name").setSortable(true).setAutoWidth(true);
        grid.asSingleSelect().addValueChangeListener(evt -> {
            edit(evt.getValue());
        });
        grid.setSizeFull();
    }

    private void configureForm() {
        // Binder rules for name
        binder.forField(nameField)
              .asRequired("Name is required")
              .withValidator(new StringLengthValidator(
                  "Must be between 1 and 50 characters", 1, 50))
              .bind(Category::getName, Category::setName);

        save.addClickListener(e -> saveCategory());
        delete.addClickListener(e -> confirmDelete());
        clear.addClickListener(e -> clearForm());
    }

    private void buildLayout() {
        H2 header = new H2("Category Management");

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField);
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 1)
        );

        HorizontalLayout buttons = new HorizontalLayout(save, delete, clear);
        buttons.setPadding(true);

        VerticalLayout formContainer = new VerticalLayout(header, formLayout, buttons);
        formContainer.setWidth("350px");
        formContainer.setPadding(true);

        HorizontalLayout main = new HorizontalLayout(grid, formContainer);
        main.setSizeFull();
        main.setFlexGrow(2, grid);
        main.setFlexGrow(1, formContainer);

        add(main);
    }

    private void edit(Category category) {
        if (category == null) {
            clearForm();
        } else {
            this.currentCategory = service.findCategoryById(category.getId())
                                          .orElse(category);
            binder.readBean(this.currentCategory);
            delete.setEnabled(true);
            save.setText("Update");
        }
    }

    private void saveCategory() {
        try {
            if (currentCategory == null) {
                currentCategory = new Category();
            }
            binder.writeBean(currentCategory);
            service.saveCategory(currentCategory);
            Notification.show("Category saved", 2000, Notification.Position.MIDDLE);
            updateGrid();
            clearForm();
        } catch (ValidationException ex) {
            Notification.show("Validation errors—please fix.", 3000, Notification.Position.MIDDLE);
        }
    }

    private void confirmDelete() {
        if (currentCategory == null || currentCategory.getId() == null) {
            Notification.show("No category selected", 2000, Notification.Position.MIDDLE);
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Category");
        dialog.setText("Really delete \"" + currentCategory.getName() + "\"?");
        dialog.setConfirmText("Delete");
        dialog.setCancelText("Cancel");
        dialog.addConfirmListener(evt -> {
            service.deleteCategory(currentCategory.getId());
            Notification.show("Category deleted", 2000, Notification.Position.MIDDLE);
            updateGrid();
            clearForm();
        });
        dialog.open();
    }

    private void clearForm() {
        currentCategory = null;
        binder.readBean(new Category());
        grid.deselectAll();
        delete.setEnabled(false);
        save.setText("Save");
    }

    private void updateGrid() {
        grid.setItems(service.listCategories());
    }
}
