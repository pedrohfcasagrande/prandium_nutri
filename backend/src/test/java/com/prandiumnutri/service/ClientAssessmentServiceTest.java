package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.AssessmentSelfFillDto;
import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.support.PlanTestFixtures;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientAssessmentServiceTest {

    @Autowired ClientAssessmentService service;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistClientLinkRepository linkRepo;
    @Autowired ClientAssessmentRepository assessmentRepo;
    @Autowired AssessmentSnapshotRepository snapshotRepo;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void upsert_savesLabsAndSyncsUser() {
        User nutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);

        ClientAssessmentDto dto = baseDto(client)
                .withWeightKg(72.5)
                .withGlucoseFasting(95.0)
                .withHba1c(5.4)
                .withVitaminD(28.0)
                .withFoodPreferences("frango, arroz, brocolis")
                .raw();

        ClientAssessmentDto saved = service.upsert(nutri, client.getId(), null, dto);

        assertThat(saved.glucoseFasting()).isEqualTo(95.0);
        assertThat(saved.hba1c()).isEqualTo(5.4);
        assertThat(saved.vitaminD()).isEqualTo(28.0);
        // sync com o cadastro do usuario
        User reload = userRepo.findById(client.getId()).orElseThrow();
        assertThat(reload.getWeightKg()).isEqualTo(72.5);
        assertThat(reload.getPreferences()).contains("frango");
    }

    @Test
    void upsert_deniedWhenNutriDoesNotAttendClient() {
        User nutri = createNutri();
        User client = createClient();
        // Sem link

        assertThatThrownBy(() -> service.upsert(nutri, client.getId(), null,
                baseDto(client).raw()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void snapshot_incrementsVersionAndPersistsPayload() {
        User nutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);

        service.upsert(nutri, client.getId(), null,
                baseDto(client).withWeightKg(80.0).raw());

        var s1 = service.createSnapshot(nutri, client.getId(), null, "Inicial");
        assertThat(s1.version()).isEqualTo(1);
        assertThat(s1.label()).isEqualTo("Inicial");
        assertThat(s1.payload().weightKg()).isEqualTo(80.0);

        // Nova consulta com peso diferente
        service.upsert(nutri, client.getId(), null,
                baseDto(client).withWeightKg(78.5).raw());
        var s2 = service.createSnapshot(nutri, client.getId(), null, "Retorno 30d");
        assertThat(s2.version()).isEqualTo(2);
        assertThat(s2.payload().weightKg()).isEqualTo(78.5);
        // O snapshot antigo (v1) permanece com o peso antigo
        var all = service.listSnapshots(nutri.getId(), client.getId(), null);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).version()).isEqualTo(2); // desc
        assertThat(all.get(1).payload().weightKg()).isEqualTo(80.0);
    }

    @Test
    void snapshot_failsWhenNoAssessmentExists() {
        User nutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);

        assertThatThrownBy(() -> service.createSnapshot(nutri, client.getId(), null, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void selfFill_updatesSafeFieldsAndIgnoresClinicalOnes() {
        User nutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);

        // Nutri cria a ficha inicial com peso + labs
        service.upsert(nutri, client.getId(), null,
                baseDto(client)
                        .withWeightKg(75.0)
                        .withGlucoseFasting(90.0)
                        .withChiefComplaint("Emagrecimento")
                        .raw());

        // Cliente auto-preenche preferencias + habitos
        var self = new AssessmentSelfFillDto(
                "cafe: pao com queijo, cafe preto",
                "5 refeicoes/dia",
                2.5,
                "raramente",
                "banana, brocolis, salmao",
                "quiabo",
                "musculacao 4x semana",
                7.5,
                false,
                null,
                "manter foco em ganho de massa magra",
                null,
                null
        );
        ClientAssessmentDto after = service.selfFillForClient(client.getId(), self);

        // Campos safe atualizados
        assertThat(after.mealFrequency()).isEqualTo("5 refeicoes/dia");
        assertThat(after.foodPreferences()).isEqualTo("banana, brocolis, salmao");
        assertThat(after.physicalActivityDetails()).isEqualTo("musculacao 4x semana");
        assertThat(after.sleepHours()).isEqualTo(7.5);
        assertThat(after.goalNotes()).contains("massa magra");

        // Campos clinicos intocados
        assertThat(after.weightKg()).isEqualTo(75.0);
        assertThat(after.glucoseFasting()).isEqualTo(90.0);
        assertThat(after.chiefComplaint()).isEqualTo("Emagrecimento");
    }

    @Test
    void upsert_persistsSkinfoldsAndPerimeters() {
        User nutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);

        ClientAssessmentDto dto = baseDto(client)
                .withWeightKg(70.0)
                .withSfTricipital(10.0)
                .withSfAbdominal(15.0)
                .withThighCm(52.0)
                .withCalfCm(36.0)
                .withSkinfoldProtocol("JP_7")
                .raw();

        ClientAssessmentDto saved = service.upsert(nutri, client.getId(), null, dto);
        assertThat(saved.sfTricipital()).isEqualTo(10.0);
        assertThat(saved.sfAbdominal()).isEqualTo(15.0);
        assertThat(saved.thighCm()).isEqualTo(52.0);
        assertThat(saved.calfCm()).isEqualTo(36.0);
        assertThat(saved.skinfoldProtocol()).isEqualTo("JP_7");
    }

    @Test
    void selfFill_failsWhenNoAssessmentYet() {
        User client = createClient();
        assertThatThrownBy(() -> service.selfFillForClient(client.getId(),
                new AssessmentSelfFillDto(null, null, null, null, null, null,
                        null, null, null, null, null, null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------- helpers --------------------

    private User createNutri() {
        User u = userRepo.save(User.builder()
                .name("Nutri").email("n-" + System.nanoTime() + "@x.com")
                .role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, u.getId(), "BRL");
        return u;
    }

    private User createClient() {
        return userRepo.save(User.builder()
                .name("Client").email("c-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(cpf())
                .build());
    }

    private String cpf() {
        return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L));
    }

    private void linkActive(User nutri, User client) {
        linkRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client)
                .invitedByEmail(client.getEmail())
                .inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
    }

    private DtoBuilder baseDto(User client) {
        return new DtoBuilder(client);
    }

    /** Builder helper para deixar os testes legiveis sem construir 50+ campos. */
    private static class DtoBuilder {
        Long clientId;
        LocalDateTime assessmentDate = LocalDateTime.now();
        Double weightKg, bodyFatPct, waistCm, hipCm, neckCm, armCm;
        Integer heightCm;
        Double thighCm, calfCm, chestCm, forearmCm, abdomenCm;
        Double sfTricipital, sfSubscapular, sfSuprailiac, sfAbdominal, sfThigh, sfChest, sfMidaxillary;
        String skinfoldProtocol;
        String chiefComplaint, medicalHistory, medications, preExistingConditions, allergies, familyHistory;
        String dietaryRecall24h, mealFrequency, alcoholFrequency, foodPreferences, foodAversions;
        Double waterIntakeLiters, sleepHours;
        String physicalActivityDetails;
        StressLevel stressLevel;
        boolean smoker;
        String smokingDetails;
        String goalNotes;
        Double targetWeightKg;
        LocalDate targetDate, labDate;
        Double glucoseFasting, hba1c, insulinFasting, totalCholesterol, ldl, hdl, triglycerides;
        Double tsh, t4Free, vitaminD, vitaminB12, iron, ferritin, crp, urea, creatinine, uricAcid;
        String labNotes, notes;

        DtoBuilder(User client) { this.clientId = client.getId(); }
        DtoBuilder withWeightKg(double v) { this.weightKg = v; return this; }
        DtoBuilder withGlucoseFasting(double v) { this.glucoseFasting = v; return this; }
        DtoBuilder withHba1c(double v) { this.hba1c = v; return this; }
        DtoBuilder withVitaminD(double v) { this.vitaminD = v; return this; }
        DtoBuilder withFoodPreferences(String v) { this.foodPreferences = v; return this; }
        DtoBuilder withChiefComplaint(String v) { this.chiefComplaint = v; return this; }
        DtoBuilder withSfTricipital(double v) { this.sfTricipital = v; return this; }
        DtoBuilder withSfAbdominal(double v) { this.sfAbdominal = v; return this; }
        DtoBuilder withThighCm(double v) { this.thighCm = v; return this; }
        DtoBuilder withCalfCm(double v) { this.calfCm = v; return this; }
        DtoBuilder withSkinfoldProtocol(String v) { this.skinfoldProtocol = v; return this; }

        ClientAssessmentDto raw() {
            return new ClientAssessmentDto(
                    null, clientId, null, null, null, null, null, null, null, assessmentDate,
                    weightKg, heightCm, bodyFatPct, waistCm, hipCm, neckCm, armCm,
                    thighCm, calfCm, chestCm, forearmCm, abdomenCm,
                    sfTricipital, sfSubscapular, sfSuprailiac, sfAbdominal, sfThigh, sfChest, sfMidaxillary,
                    skinfoldProtocol,
                    chiefComplaint, medicalHistory, medications, preExistingConditions, allergies, familyHistory,
                    dietaryRecall24h, mealFrequency, waterIntakeLiters, alcoholFrequency, foodPreferences, foodAversions,
                    physicalActivityDetails, sleepHours, stressLevel, smoker, smokingDetails,
                    goalNotes, targetWeightKg, targetDate,
                    labDate, glucoseFasting, hba1c, insulinFasting,
                    totalCholesterol, ldl, hdl, triglycerides,
                    tsh, t4Free, vitaminD, vitaminB12, iron, ferritin, crp,
                    urea, creatinine, uricAcid, labNotes,
                    notes, null, null
            );
        }
    }
}
