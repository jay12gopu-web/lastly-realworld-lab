package com.lastly.lab.service;

import com.lastly.lab.domain.*;
import com.lastly.lab.dto.CreateOrderRequest;
import com.lastly.lab.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock ProductRepository products;
    @Mock CustomerOrderRepository orders;
    OrderService service;
    Product milk;

    @BeforeEach void setUp() {
        service = new OrderService(products, orders, true);
        milk = new Product(1L, "MILK", "Milk", new BigDecimal("58.00"), 1);
    }

    @Test void successfulOrderDecrementsStock() {
        var request = new CreateOrderRequest(1L, 1, "unique-request-1");
        when(orders.findByIdempotencyKey("unique-request-1")).thenReturn(Optional.empty());
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(milk));
        when(orders.saveAndFlush(any(CustomerOrder.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.place(request, 123L);
        assertEquals(0, milk.getStockQuantity());
        assertEquals(OrderStatus.PLACED, result.getStatus());
        assertEquals(new BigDecimal("58.00"), result.getTotalPrice());
    }

    @Test void noOversellingWhenStockIsZero() {
        milk.reserve(1);
        var request = new CreateOrderRequest(1L, 1, "unique-request-2");
        when(orders.findByIdempotencyKey("unique-request-2")).thenReturn(Optional.empty());
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(milk));
        assertThrows(DomainException.class, () -> service.place(request, 123L));
        verify(orders, never()).saveAndFlush(any());
    }

    @Test void duplicateRequestReturnsExistingWithoutReservingAgain() {
        var request = new CreateOrderRequest(1L, 1, "repeat-1");
        var existing = new CustomerOrder("repeat-1", milk, 1, 123L);
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(milk));
        when(orders.findByIdempotencyKey("repeat-1")).thenReturn(Optional.of(existing));
        assertSame(existing, service.place(request, 123L));
        assertEquals(1, milk.getStockQuantity());
        verify(orders, never()).saveAndFlush(any());
    }

    @Test void sameKeyWithDifferentQuantityIsRejected() {
        var existing = new CustomerOrder("repeat-2", milk, 1, 123L);
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(milk));
        when(orders.findByIdempotencyKey("repeat-2")).thenReturn(Optional.of(existing));
        assertThrows(DomainException.class,
            () -> service.place(new CreateOrderRequest(1L, 2, "repeat-2"), 123L));
    }
}
