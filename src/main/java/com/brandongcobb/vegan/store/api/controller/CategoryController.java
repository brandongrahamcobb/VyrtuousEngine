package com.brandongcobb.vegan.store.api.controller;

import com.brandongcobb.vegan.store.api.dto.CategoryResponse;
import com.brandongcobb.vegan.store.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final StoreService storeService;

    public CategoryController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return storeService.listCategories().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategory(@PathVariable Long id) {
        var c = storeService.findCategoryById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return new CategoryResponse(c.getId(), c.getName());
    }
}