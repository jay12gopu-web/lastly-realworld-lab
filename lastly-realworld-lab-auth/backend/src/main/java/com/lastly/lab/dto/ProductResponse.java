package com.lastly.lab.dto;

import com.lastly.lab.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String sku, String name, BigDecimal price, int stockQuantity) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStockQuantity());
    }
}
