package com.brandongcobb.vegan.store.api.controller;

import com.brandongcobb.vegan.store.api.dto.AuthResponse;
import com.brandongcobb.vegan.store.api.dto.CustomerLoginRequest;
import com.brandongcobb.vegan.store.api.dto.CustomerRegistrationRequest;
import com.brandongcobb.vegan.store.api.dto.CustomerResponse;
import com.brandongcobb.vegan.store.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class CustomerAuthController {

    private final CustomerService customerService;

    public CustomerAuthController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    public CustomerResponse register(@Valid @RequestBody CustomerRegistrationRequest request) {
        return customerService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody CustomerLoginRequest request) {
        return customerService.login(request);
    }
}