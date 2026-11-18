package com.lastly.lab.service;

import com.lastly.lab.domain.*;
import com.lastly.lab.dto.CreateOrderRequest;
import com.lastly.lab.repository.CustomerOrderRepository;
import com.lastly.lab.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class OrderService {
    private final ProductRepository products;
    private final CustomerOrderRepository orders;
    private final boolean writesEnabled;

    public OrderService(ProductRepository products, CustomerOrderRepository orders,
                        @Value("${app.orders.write-enabled:false}") boolean writesEnabled) {
        this.products = products;
        this.orders = orders;
        this.writesEnabled = writesEnabled;
    }

    @Transactional
    public CustomerOrder place(CreateOrderRequest request, Long customerId) {
        if (!writesEnabled) throw new DomainException("Order creation disabled in this environment");
        // Lock FIRST. Under MySQL REPEATABLE READ, a preliminary ordinary SELECT could
        // establish a stale snapshot and cause a simultaneous retry to miss the first order.
        Product product = products.findByIdForUpdate(request.productId())
            .orElseThrow(() -> new NotFoundException("Product not found"));

        // With the product lock held, the first consistent read sees earlier committed orders.
        Optional<CustomerOrder> existing = orders.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) return verifySameRequest(existing.get(), request, customerId);

        if (product.getStockQuantity() < request.quantity()) {
            throw new DomainException("Not enough stock; available: " + product.getStockQuantity());
        }
        product.reserve(request.quantity());
        // Order + stock update commit atomically in this same database transaction.
        // Unique constraint on idempotency_key is the final guard against cross-product races.
        return orders.saveAndFlush(new CustomerOrder(request.idempotencyKey(), product, request.quantity(), customerId));
    }

    private CustomerOrder verifySameRequest(CustomerOrder existing, CreateOrderRequest incoming, Long customerId) {
        if (!customerId.equals(existing.getCustomerId())) throw new DomainException("Idempotency key belongs to another request");
        if (!existing.getProductId().equals(incoming.productId()) || existing.getQuantity() != incoming.quantity()) {
            throw new DomainException("Idempotency key already used for different order details");
        }
        return existing;
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(String id, Long customerId) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new NotFoundException("Order not found"));
        // 404 instead of 403 avoids confirming another customer's order ID.
        if (!customerId.equals(order.getCustomerId())) throw new NotFoundException("Order not found");
        return order;
    }
}
