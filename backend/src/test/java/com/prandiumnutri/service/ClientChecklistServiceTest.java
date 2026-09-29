package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.ClientChecklistDto;
import com.prandiumnutri.repository.ClientAssessmentRepository;
import com.prandiumnutri.repository.FoodRepository;
import com.prandiumnutri.repository.MealRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.ProgressEntryRepository;
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
class ClientChecklistServiceTest {

    @Autowired ClientChecklistService service;
    @Autowired UserRepository userRepo;
    @Autowired FoodRepository foodRepo;
    @Autowired MealRepository mealRepo;
    @Autowired ProgressEntryRepository progressRepo;
    @Autowired ClientAssessmentRepository assessmentRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    private User user() {
        return userRepo.save(User.builder()
                .name("New Client").email("newc-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x")
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    @Test
    void freshUser_allStepsIncomplete() {
        User u = user();
        ClientChecklistDto c = service.compute(u.getId());
        assertThat(c.profileFilled()).isFalse();
        assertThat(c.goalsPrescribed()).isFalse();
        assertThat(c.firstMealLogged()).isFalse();
        assertThat(c.firstProgressLogged()).isFalse();
        assertThat(c.completedSteps()).isZero();
        assertThat(c.totalSteps()).isEqualTo(4);
    }

    @Test
    void healthProfileFilled_marksFirstStep() {
        User u = user();
        u.setWeightKg(70.0);
        u.setHeightCm(170.0);
        u.setGoal(Goal.LOSE);
        userRepo.save(u);
        ClientChecklistDto c = service.compute(u.getId());
        assertThat(c.profileFilled()).isTrue();
        assertThat(c.completedSteps()).isEqualTo(1);
    }

    @Test
    void allStepsDone_gives4of4() {
        User nutri = userRepo.save(User.builder()
                .name("Doc").email("doc-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x")
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());

        User u = user();
        u.setWeightKg(80.0); u.setHeightCm(175.0); u.setGoal(Goal.MAINTAIN);
        userRepo.save(u);

        assessmentRepo.save(ClientAssessment.builder()
                .nutritionist(nutri).client(u)
                .assessmentDate(LocalDateTime.now())
                .targetCalories(2200.0).build());

        Food banana = foodRepo.save(Food.builder()
                .name("Check Banana " + System.nanoTime()).caloriesPer100g(89.0)
                .proteinPer100g(1.1).carbsPer100g(23.0).fatPer100g(0.3).build());
        Meal meal = Meal.builder()
                .user(u).date(LocalDate.now()).time(LocalTime.NOON).type(MealType.LUNCH).build();
        meal.getItems().add(MealItem.builder().meal(meal).food(banana).grams(100.0).build());
        mealRepo.save(meal);

        progressRepo.save(ProgressEntry.builder()
                .user(u).date(LocalDate.now()).weightKg(80.0).build());

        ClientChecklistDto c = service.compute(u.getId());
        assertThat(c.profileFilled()).isTrue();
        assertThat(c.goalsPrescribed()).isTrue();
        assertThat(c.firstMealLogged()).isTrue();
        assertThat(c.firstProgressLogged()).isTrue();
        assertThat(c.completedSteps()).isEqualTo(4);
    }
}
