package com.lastly.lab.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String sku;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;
    @Version
    private long version;

    protected Product() {}

    public Product(Long id, String sku, String name, BigDecimal price, int stockQuantity) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public long getVersion() { return version; }

    public void reserve(int quantity) {
        if (quantity <= 0 || quantity > stockQuantity) {
            throw new IllegalArgumentException("Invalid quantity for available stock");
        }
        stockQuantity -= quantity;
    }
}
