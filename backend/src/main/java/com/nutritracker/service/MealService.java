package com.nutritracker.service;

import com.nutritracker.domain.AuditAction;
import com.nutritracker.domain.Food;
import com.nutritracker.domain.Meal;
import com.nutritracker.domain.MealItem;
import com.nutritracker.dto.MealDto;
import com.nutritracker.dto.MealItemDto;
import com.nutritracker.repository.MealRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealService {

    private final MealRepository mealRepository;
    private final UserService userService;
    private final FoodService foodService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<MealDto> findByUserAndDate(Long userId, LocalDate date) {
        return mealRepository.findByUserIdAndDateOrderByTimeAsc(userId, date)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public MealDto save(MealDto dto) {
        boolean isNew = dto.id() == null;
        Meal meal = dto.id() != null
                ? mealRepository.findById(dto.id()).orElseThrow(() -> new EntityNotFoundException("Meal not found"))
                : new Meal();
        meal.setUser(userService.getOrThrow(dto.userId()));
        meal.setDate(dto.date());
        meal.setTime(dto.time());
        meal.setType(dto.type());

        meal.getItems().clear();
        List<MealItem> newItems = new ArrayList<>();
        for (MealItemDto itemDto : dto.items()) {
            Food food = foodService.getOrThrow(itemDto.foodId());
            newItems.add(MealItem.builder()
                    .meal(meal)
                    .food(food)
                    .grams(itemDto.grams())
                    .build());
        }
        meal.getItems().addAll(newItems);

        MealDto saved = toDto(mealRepository.save(meal));
        if (isNew) {
            auditService.record(AuditAction.MEAL_LOGGED, meal.getUser().getId(), meal.getUser().getRole(),
                    "Meal", saved.id());
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Meal meal = mealRepository.findById(id).orElse(null);
        mealRepository.deleteById(id);
        if (meal != null) {
            auditService.record(AuditAction.MEAL_DELETED, meal.getUser().getId(), meal.getUser().getRole(),
                    "Meal", id);
        }
    }

    public MealDto toDto(Meal meal) {
        double totalCal = 0, totalProt = 0, totalCarb = 0, totalFat = 0;
        List<MealItemDto> items = new ArrayList<>();
        for (MealItem it : meal.getItems()) {
            double factor = it.getGrams() / 100.0;
            double c = it.getFood().getCaloriesPer100g() * factor;
            double p = it.getFood().getProteinPer100g() * factor;
            double ca = it.getFood().getCarbsPer100g() * factor;
            double f = it.getFood().getFatPer100g() * factor;
            totalCal += c; totalProt += p; totalCarb += ca; totalFat += f;
            items.add(new MealItemDto(it.getId(), it.getFood().getId(), it.getFood().getName(),
                    it.getGrams(), round(c), round(p), round(ca), round(f)));
        }
        return new MealDto(meal.getId(), meal.getUser().getId(), meal.getDate(), meal.getTime(),
                meal.getType(), items, round(totalCal), round(totalProt), round(totalCarb), round(totalFat));
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
