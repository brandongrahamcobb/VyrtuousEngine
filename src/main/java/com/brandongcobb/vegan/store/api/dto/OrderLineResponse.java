package com.brandongcobb.vegan.store.api.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
    Long productId,
    String productName,
    int quantity,
    BigDecimal price
) {}