package com.lastly.lab.dto;

import jakarta.validation.constraints.*;

public record CreateOrderRequest(
    @NotNull @Positive Long productId,
    @Min(1) @Max(50) int quantity,
    @NotBlank @Size(max = 80) String idempotencyKey
) {}
