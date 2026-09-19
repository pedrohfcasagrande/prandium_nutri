package com.prandiumnutri.repository;

import com.prandiumnutri.domain.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByNameContainingIgnoreCase(String name);
    Optional<Food> findFirstByNameIgnoreCase(String name);
}
