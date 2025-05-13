package com.brandongcobb.vegan.store.repo;

import com.brandongcobb.vegan.store.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> { }
