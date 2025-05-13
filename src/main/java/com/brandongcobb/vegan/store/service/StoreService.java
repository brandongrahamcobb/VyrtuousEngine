package com.brandongcobb.vegan.store.service;

import com.brandongcobb.vegan.store.domain.Category;
import com.brandongcobb.vegan.store.domain.Product;
import com.brandongcobb.vegan.store.repo.CategoryRepository;
import com.brandongcobb.vegan.store.repo.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StoreService {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    public StoreService(CategoryRepository categoryRepo,
                        ProductRepository productRepo) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
    }

    // —— Category operations —— //

    public List<Category> listCategories() {
        return categoryRepo.findAll();
    }

    public Optional<Category> findCategoryById(Long id) {
        return categoryRepo.findById(id);
    }

    public Category saveCategory(Category category) {
        return categoryRepo.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepo.deleteById(id);
    }

    // —— Product operations —— //

    public List<Product> listProducts() {
        return productRepo.findAll();
    }

    public Optional<Product> findProductById(Long id) {
        return productRepo.findById(id);
    }

    public Product saveProduct(Product product) {
        return productRepo.save(product);
    }

    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }
}
