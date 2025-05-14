package com.brandongcobb.vegan.store.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
/**
 * A login view rendered by Spring Security if the user is not authenticated.
 */
@Route("login")
@PageTitle("Login | Vegan Store")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        login.setAction("login");
        // login.setOpened(true); LoginForm is always visible in the layout
        login.setForgotPasswordButtonVisible(false);
        Anchor registerLink = new Anchor("register", "Don’t have an account? Sign up");
        registerLink.getStyle().set("margin-top","1em");
        add(login, registerLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Inform the user about an authentication error
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
