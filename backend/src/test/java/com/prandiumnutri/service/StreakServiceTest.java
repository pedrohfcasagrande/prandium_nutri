package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.StreakInfoDto;
import com.prandiumnutri.repository.FoodRepository;
import com.prandiumnutri.repository.MealRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StreakServiceTest {

    @Autowired StreakService streakService;
    @Autowired MealRepository mealRepo;
    @Autowired FoodRepository foodRepo;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    private Food food;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
        food = foodRepo.findAll().stream()
                .filter(f -> "Streak Banana".equals(f.getName()))
                .findFirst()
                .orElseGet(() -> foodRepo.save(Food.builder()
                        .name("Streak Banana").caloriesPer100g(89.0).proteinPer100g(1.1)
                        .carbsPer100g(23.0).fatPer100g(0.3).build()));
    }

    private User newUser() {
        return userRepo.save(User.builder()
                .name("Streaker").email("streak-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x")
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    private void logMealOn(User user, LocalDate date) {
        Meal meal = Meal.builder()
                .user(user).date(date).time(LocalTime.NOON).type(MealType.LUNCH).build();
        MealItem item = MealItem.builder().meal(meal).food(food).grams(100.0).build();
        meal.getItems().add(item);
        mealRepo.save(meal);
    }

    @Test
    void emptyHistory_returnsZeros() {
        User u = newUser();
        StreakInfoDto s = streakService.compute(u.getId());
        assertThat(s.currentStreak()).isZero();
        assertThat(s.longestStreak()).isZero();
        assertThat(s.badges()).isEmpty();
        assertThat(s.todayLogged()).isFalse();
    }

    @Test
    void threeConsecutiveDaysEndingToday_gives3StreakAnd3dBadge() {
        User u = newUser();
        LocalDate today = LocalDate.now();
        logMealOn(u, today.minusDays(2));
        logMealOn(u, today.minusDays(1));
        logMealOn(u, today);

        StreakInfoDto s = streakService.compute(u.getId());
        assertThat(s.currentStreak()).isEqualTo(3);
        assertThat(s.longestStreak()).isEqualTo(3);
        assertThat(s.todayLogged()).isTrue();
        assertThat(s.badges()).containsExactly("streak_3");
    }

    @Test
    void gapBreaksStreak_currentIsRecentRun() {
        User u = newUser();
        LocalDate today = LocalDate.now();
        // Passado: run de 7
        for (int i = 20; i >= 14; i--) logMealOn(u, today.minusDays(i));
        // Recente: run de 2 terminando ontem
        logMealOn(u, today.minusDays(2));
        logMealOn(u, today.minusDays(1));

        StreakInfoDto s = streakService.compute(u.getId());
        assertThat(s.currentStreak()).isEqualTo(2);
        assertThat(s.longestStreak()).isEqualTo(7);
        assertThat(s.badges()).containsExactly("streak_3", "streak_7");
        assertThat(s.todayLogged()).isFalse();
    }

    @Test
    void lastMealWasBeforeYesterday_currentIsZero_longestPreserved() {
        User u = newUser();
        LocalDate today = LocalDate.now();
        // 4 dias consecutivos terminando 5 dias atras
        for (int i = 8; i >= 5; i--) logMealOn(u, today.minusDays(i));

        StreakInfoDto s = streakService.compute(u.getId());
        assertThat(s.currentStreak()).isZero();
        assertThat(s.longestStreak()).isEqualTo(4);
        assertThat(s.badges()).containsExactly("streak_3");
    }
}
