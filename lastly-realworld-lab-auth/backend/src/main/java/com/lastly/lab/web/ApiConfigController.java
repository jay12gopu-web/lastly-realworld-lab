package com.lastly.lab.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ApiConfigController {
    private final boolean ordersEnabled;
    private final boolean authenticationEnabled;
    public ApiConfigController(@Value("${app.orders.write-enabled:false}") boolean ordersEnabled,
                               @Value("${app.auth.enabled:false}") boolean authenticationEnabled) {
        this.ordersEnabled = ordersEnabled;
        this.authenticationEnabled = authenticationEnabled;
    }
    @GetMapping("/api/config")
    public Map<String, Boolean> config() {
        return Map.of("ordersEnabled", ordersEnabled, "authenticationEnabled", authenticationEnabled);
    }
}
