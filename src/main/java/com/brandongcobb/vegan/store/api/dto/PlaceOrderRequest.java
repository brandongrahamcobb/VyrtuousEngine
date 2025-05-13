package com.brandongcobb.vegan.store.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlaceOrderRequest(
    @NotNull(message = "Customer ID is required")
    Long customerId,
    @NotEmpty(message = "Order must contain at least one item")
    List<OrderLineRequest> items
) {}