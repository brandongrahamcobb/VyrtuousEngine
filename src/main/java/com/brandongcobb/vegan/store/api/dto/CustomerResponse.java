package com.brandongcobb.vegan.store.api.dto;

public record CustomerResponse(
    Long id,
    String firstName,
    String lastName,
    String email
) {}