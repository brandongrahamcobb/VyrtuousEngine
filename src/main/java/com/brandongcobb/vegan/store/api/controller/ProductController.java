package com.brandongcobb.vegan.store.api.controller;

import com.brandongcobb.vegan.store.api.dto.ProductResponse;
import com.brandongcobb.vegan.store.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final StoreService storeService;

    public ProductController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<ProductResponse> listProducts() {
        return storeService.listProducts().stream()
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getStock(),
                        p.getCategory().getId(),
                        p.getCategory().getName(),
                        p.getImageUrl()
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        var p = storeService.findProductById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getImageUrl()
        );
    }
}
