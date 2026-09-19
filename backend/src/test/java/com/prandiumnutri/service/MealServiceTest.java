package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.MealDto;
import com.prandiumnutri.dto.MealItemDto;
import com.prandiumnutri.repository.AuditLogRepository;
import com.prandiumnutri.repository.FoodRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MealServiceTest {

    @Autowired MealService mealService;
    @Autowired UserRepository userRepo;
    @Autowired FoodRepository foodRepo;
    @Autowired AuditLogRepository auditRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void save_newMeal_firesMealLoggedAudit() {
        User user = userRepo.save(User.builder()
                .name("Eater").email("eater-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
        Food banana = foodRepo.save(Food.builder()
                .name("Banana meal test").caloriesPer100g(89.0).proteinPer100g(1.1)
                .carbsPer100g(23.0).fatPer100g(0.3).build());

        MealDto dto = new MealDto(null, user.getId(), LocalDate.now(), LocalTime.NOON, MealType.LUNCH,
                List.of(new MealItemDto(null, banana.getId(), banana.getName(), 100.0, 0.0, 0.0, 0.0, 0.0)),
                null, null, null, null);
        MealDto saved = mealService.save(dto);
        assertThat(saved.id()).isNotNull();

        // Audit e async - aguarda
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        Awaitility.await().atMost(Duration.ofSeconds(2)).until(() ->
                auditRepo.countByActionAndActorSince(AuditAction.MEAL_LOGGED, user.getId(), since) >= 1);
    }
}
