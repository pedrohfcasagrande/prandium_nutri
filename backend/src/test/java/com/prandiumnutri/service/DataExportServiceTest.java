package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataExportServiceTest {

    @Autowired DataExportService exportService;
    @Autowired UserRepository userRepo;
    @Autowired MealRepository mealRepo;
    @Autowired FoodRepository foodRepo;
    @Autowired ProgressEntryRepository progressRepo;
    @Autowired WaterIntakeService waterService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    private User makeUser(Role role) {
        return userRepo.save(User.builder()
                .name("Exp-" + role).email("exp-" + System.nanoTime() + "@x.com")
                .role(role).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x")
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    private Set<String> entries(byte[] zipBytes) {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) names.add(e.getName());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return names;
    }

    @Test
    void export_userWithMealsAndProgress_containsAllFiles() {
        User u = makeUser(Role.USER);
        Food banana = foodRepo.save(Food.builder()
                .name("Exp banana " + System.nanoTime()).caloriesPer100g(89.0)
                .proteinPer100g(1.1).carbsPer100g(23.0).fatPer100g(0.3).build());
        Meal meal = Meal.builder()
                .user(u).date(LocalDate.now()).time(LocalTime.NOON).type(MealType.LUNCH).build();
        meal.getItems().add(MealItem.builder().meal(meal).food(banana).grams(120.0).build());
        mealRepo.save(meal);
        progressRepo.save(ProgressEntry.builder().user(u).date(LocalDate.now()).weightKg(70.0).build());
        waterService.log(u.getId(), 250);

        byte[] zip = exportService.exportForUser(u.getId());
        Set<String> files = entries(zip);

        assertThat(files).contains(
                "README.md", "profile.json",
                "meals.json", "progress.json", "water_intakes.json",
                "assessments.json", "chat_sent.json",
                "notifications.json");
        // Nao deve haver arquivos de nutri/company pra este role
        assertThat(files).doesNotContain("practices.json", "company.json", "company_members.json");
    }

    @Test
    void export_nutritionist_containsNutriSpecificFiles() {
        User n = makeUser(Role.NUTRITIONIST);
        byte[] zip = exportService.exportForUser(n.getId());
        Set<String> files = entries(zip);

        assertThat(files).contains(
                "README.md", "profile.json",
                "practices.json", "client_links.json",
                "prescription_issuances.json", "chat_sent.json",
                "notifications.json");
        // Nao vaza dados clinicos de cliente pro nutri
        assertThat(files).doesNotContain("meals.json", "assessments.json", "progress.json");
    }

    @Test
    void export_profileJson_containsExpectedFields() {
        User u = makeUser(Role.USER);
        u.setWeightKg(72.5);
        u.setHeightCm(175.0);
        u.setGoal(Goal.MAINTAIN);
        userRepo.save(u);

        byte[] zip = exportService.exportForUser(u.getId());
        // Le o profile.json de dentro do zip
        String profileJson = readEntry(zip, "profile.json");
        assertThat(profileJson)
                .contains("\"weightKg\" : 72.5")
                .contains("\"heightCm\" : 175.0")
                .contains("\"goal\" : \"MAINTAIN\"")
                .contains("\"email\"");
    }

    private String readEntry(byte[] zipBytes, String name) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (e.getName().equals(name)) {
                    return new String(zip.readAllBytes());
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }
}
