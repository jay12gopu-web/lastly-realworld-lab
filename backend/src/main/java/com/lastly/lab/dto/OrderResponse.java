package com.lastly.lab.dto;

import com.lastly.lab.domain.CustomerOrder;
import com.lastly.lab.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    String id, Long productId, String productName, int quantity,
    BigDecimal totalPrice, OrderStatus status, Instant createdAt
) {
    public static OrderResponse from(CustomerOrder o) {
        return new OrderResponse(o.getId(), o.getProductId(), o.getProductName(), o.getQuantity(),
            o.getTotalPrice(), o.getStatus(), o.getCreatedAt());
    }
}
