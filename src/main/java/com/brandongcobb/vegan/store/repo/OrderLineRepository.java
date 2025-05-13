package com.brandongcobb.vegan.store.repo;

import com.brandongcobb.vegan.store.domain.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
}