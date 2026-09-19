package com.nutritracker.service;

import com.nutritracker.domain.User;
import com.nutritracker.dto.DailySummaryDto;
import com.nutritracker.dto.MealDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserService userService;
    private final MealService mealService;
    private final NutritionCalculator calculator;

    @Transactional(readOnly = true)
    public DailySummaryDto summary(Long userId, LocalDate date) {
        User user = userService.getOrThrow(userId);
        List<MealDto> meals = mealService.findByUserAndDate(userId, date);

        double cal = meals.stream().mapToDouble(MealDto::totalCalories).sum();
        double prot = meals.stream().mapToDouble(MealDto::totalProtein).sum();
        double carb = meals.stream().mapToDouble(MealDto::totalCarbs).sum();
        double fat = meals.stream().mapToDouble(MealDto::totalFat).sum();

        boolean healthReady = user.getBirthDate() != null && user.getWeightKg() != null
                && user.getHeightCm() != null && user.getSex() != null
                && user.getActivityLevel() != null && user.getGoal() != null;
        double calGoal = healthReady ? round(calculator.calorieGoal(user)) : 0;
        double protGoal = healthReady ? round(calculator.proteinGoal(user)) : 0;

        return new DailySummaryDto(date, calGoal, protGoal,
                round(cal), round(prot), round(carb), round(fat),
                round(calGoal - cal), round(protGoal - prot), meals);
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
