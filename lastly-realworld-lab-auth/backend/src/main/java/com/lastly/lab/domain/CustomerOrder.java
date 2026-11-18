package com.lastly.lab.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id
    @Column(length = 36, nullable = false)
    private String id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;
    @Column(name = "customer_id")
    private Long customerId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CustomerOrder() {}

    public CustomerOrder(String idempotencyKey, Product product, int quantity, Long customerId) {
        this.customerId = customerId;
        this.id = UUID.randomUUID().toString();
        this.idempotencyKey = idempotencyKey;
        this.productId = product.getId();
        this.productName = product.getName();
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.totalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        this.status = OrderStatus.PLACED;
        this.createdAt = Instant.now();
    }
    public String getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
