package com.brandongcobb.vegan.store.service.impl;

import com.brandongcobb.vegan.store.api.dto.AuthResponse;
import com.brandongcobb.vegan.store.api.dto.CustomerLoginRequest;
import com.brandongcobb.vegan.store.api.dto.CustomerRegistrationRequest;
import com.brandongcobb.vegan.store.api.dto.CustomerResponse;
import com.brandongcobb.vegan.store.domain.Customer;
import com.brandongcobb.vegan.store.repo.CustomerRepository;
import com.brandongcobb.vegan.store.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CustomerResponse register(CustomerRegistrationRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        Customer customer = new Customer(
                request.firstName(),
                request.lastName(),
                request.email(),
                encodedPassword
        );
        Customer saved = customerRepository.save(customer);
        return new CustomerResponse(saved.getId(), saved.getFirstName(), saved.getLastName(), saved.getEmail());
    }

    @Override
    public AuthResponse login(CustomerLoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), customer.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        String token = UUID.randomUUID().toString();
        return new AuthResponse(token);
    }
}