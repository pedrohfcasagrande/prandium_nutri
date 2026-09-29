package com.prandiumnutri.service;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.DailySummaryDto;
import com.prandiumnutri.dto.MealDto;
import com.prandiumnutri.repository.ClientAssessmentRepository;
import com.prandiumnutri.repository.WaterIntakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserService userService;
    private final MealService mealService;
    private final NutritionCalculator calculator;
    private final ClientAssessmentRepository assessmentRepository;
    private final WaterIntakeRepository waterIntakeRepository;

    @Transactional(readOnly = true)
    public DailySummaryDto summary(Long userId, LocalDate date) {
        User user = userService.getOrThrow(userId);
        List<MealDto> meals = mealService.findByUserAndDate(userId, date);

        double cal = meals.stream().mapToDouble(MealDto::totalCalories).sum();
        double prot = meals.stream().mapToDouble(MealDto::totalProtein).sum();
        double carb = meals.stream().mapToDouble(MealDto::totalCarbs).sum();
        double fat = meals.stream().mapToDouble(MealDto::totalFat).sum();

        // Fonte 1 (prioridade): prescricao ativa na ficha mais recente do cliente.
        Optional<ClientAssessment> ficha = assessmentRepository
                .findByClientIdOrderByUpdatedAtDesc(userId).stream()
                .filter(a -> a.getTargetCalories() != null)
                .findFirst();

        Double calGoal = null, protGoal = null, carbsGoal = null, fatGoal = null, waterGoal = null;
        String source = null;

        if (ficha.isPresent()) {
            ClientAssessment a = ficha.get();
            calGoal   = a.getTargetCalories();
            protGoal  = a.getTargetProteinG();
            carbsGoal = a.getTargetCarbsG();
            fatGoal   = a.getTargetFatG();
            waterGoal = a.getTargetWaterMl();
            source = "PRESCRIPTION";
        } else {
            // Fonte 2: calculo derivado do HealthProfile do proprio user (comportamento antigo).
            boolean healthReady = user.getBirthDate() != null && user.getWeightKg() != null
                    && user.getHeightCm() != null && user.getSex() != null
                    && user.getActivityLevel() != null && user.getGoal() != null;
            if (healthReady) {
                calGoal = round(calculator.calorieGoal(user));
                protGoal = round(calculator.proteinGoal(user));
                source = "SELF";
                // carbs/fat/water nao vem do calculo legado — deixa null pro frontend nao exibir
            }
        }

        Double calRemain = calGoal != null ? round(calGoal - cal) : null;
        Double protRemain = protGoal != null ? round(protGoal - prot) : null;

        long waterMl = waterIntakeRepository.sumMlByUserAndDate(userId, date);
        Integer waterConsumed = waterMl > 0 ? (int) waterMl : null;

        return new DailySummaryDto(date,
                calGoal, protGoal, carbsGoal, fatGoal, waterGoal,
                round(cal), round(prot), round(carb), round(fat),
                calRemain, protRemain,
                waterConsumed,
                source,
                meals);
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
