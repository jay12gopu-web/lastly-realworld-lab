package com.lastly.lab.web;

import com.lastly.lab.dto.ProductResponse;
import com.lastly.lab.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository products;
    public ProductController(ProductRepository products) { this.products = products; }

    @GetMapping
    public List<ProductResponse> list() {
        return products.findAll().stream().map(ProductResponse::from).toList();
    }
}
