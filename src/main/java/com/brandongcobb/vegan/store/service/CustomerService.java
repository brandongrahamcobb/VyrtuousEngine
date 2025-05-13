package com.brandongcobb.vegan.store.service;

import com.brandongcobb.vegan.store.api.dto.AuthResponse;
import com.brandongcobb.vegan.store.api.dto.CustomerLoginRequest;
import com.brandongcobb.vegan.store.api.dto.CustomerRegistrationRequest;
import com.brandongcobb.vegan.store.api.dto.CustomerResponse;

public interface CustomerService {
    CustomerResponse register(CustomerRegistrationRequest request);
    AuthResponse login(CustomerLoginRequest request);
}