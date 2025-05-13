package com.brandongcobb.vegan.store.repo;

import com.brandongcobb.vegan.store.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> { }
