package com.lastly.lab.web;

import com.lastly.lab.dto.*;
import com.lastly.lab.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        return OrderResponse.from(service.place(request, Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return OrderResponse.from(service.get(id, Long.valueOf(jwt.getSubject())));
    }
}
