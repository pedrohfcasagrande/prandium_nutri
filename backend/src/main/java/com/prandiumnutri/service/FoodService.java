package com.prandiumnutri.service;

import com.prandiumnutri.domain.Food;
import com.prandiumnutri.dto.FoodDto;
import com.prandiumnutri.repository.FoodRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodRepository foodRepository;

    @Transactional(readOnly = true)
    public List<FoodDto> search(String query) {
        List<Food> foods = (query == null || query.isBlank())
                ? foodRepository.findAll()
                : foodRepository.findByNameContainingIgnoreCase(query);
        return foods.stream().map(this::toDto).toList();
    }

    @Transactional
    public FoodDto save(FoodDto dto) {
        Food food;
        if (dto.id() != null) {
            food = foodRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Food not found"));
        } else {
            // Dedupe por nome (case-insensitive) para evitar duplicatas quando o mesmo
            // alimento IA e adicionado mais de uma vez
            var existing = foodRepository.findFirstByNameIgnoreCase(dto.name().trim());
            if (existing.isPresent()) return toDto(existing.get());
            food = new Food();
        }
        food.setName(dto.name().trim());
        food.setCategory(dto.category());
        food.setCaloriesPer100g(dto.caloriesPer100g());
        food.setProteinPer100g(dto.proteinPer100g());
        food.setCarbsPer100g(dto.carbsPer100g());
        food.setFatPer100g(dto.fatPer100g());
        return toDto(foodRepository.save(food));
    }

    public Food getOrThrow(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Food not found: " + id));
    }

    public FoodDto toDto(Food f) {
        return new FoodDto(f.getId(), f.getName(), f.getCategory(),
                f.getCaloriesPer100g(), f.getProteinPer100g(), f.getCarbsPer100g(), f.getFatPer100g());
    }
}
