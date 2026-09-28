package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.ConsultationDto;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsultationServiceTest {

    @Autowired ConsultationService service;
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
    void create_persistsBasicFields() {
        User nutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);

        ConsultationDto dto = new ConsultationDto(null, client.getId(), null, null, null, null,
                LocalDateTime.now(), ConsultationType.INITIAL,
                "Emagrecimento", "Cliente motivado", "Deficit calorico 20%",
                LocalDateTime.now().plusDays(30), null, null, null, null);

        ConsultationDto saved = service.create(nutri, client.getId(), null, dto, false);
        assertThat(saved.id()).isNotNull();
        assertThat(saved.type()).isEqualTo(ConsultationType.INITIAL);
        assertThat(saved.chiefComplaint()).isEqualTo("Emagrecimento");
    }

    @Test
    void create_withSnapshotFlagCreatesLinkedSnapshot() {
        User nutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);
        // Precisa ter uma ficha existente pra o snapshot ter algo pra copiar
        assessmentRepo.save(ClientAssessment.builder()
                .client(client).nutritionist(nutri)
                .assessmentDate(LocalDateTime.now())
                .weightKg(80.0).heightCm(180)
                .build());

        ConsultationDto dto = new ConsultationDto(null, client.getId(), null, null, null, null,
                LocalDateTime.now(), ConsultationType.RETURN,
                null, null, null, null, null, null, null, null);

        ConsultationDto saved = service.create(nutri, client.getId(), null, dto, true);
        assertThat(saved.snapshotId()).isNotNull();
        assertThat(saved.snapshotVersion()).isEqualTo(1);
    }

    @Test
    void list_returnsOnlyOwnConsultations() {
        User nutri = createNutri();
        User otherNutri = createNutri();
        User client = createClient();
        linkActive(nutri, client);
        linkActive(otherNutri, client);

        service.create(nutri, client.getId(), null,
                blankDto(client, "queixa A"), false);
        service.create(otherNutri, client.getId(), null,
                blankDto(client, "queixa B"), false);

        List<ConsultationDto> mine = service.listForNutri(nutri, client.getId(), null);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).chiefComplaint()).isEqualTo("queixa A");
    }

    @Test
    void update_deniedWhenOwnedByOtherNutri() {
        User nutri = createNutri();
        User other = createNutri();
        User client = createClient();
        linkActive(nutri, client);

        ConsultationDto created = service.create(nutri, client.getId(), null,
                blankDto(client, "algo"), false);

        assertThatThrownBy(() -> service.update(other, created.id(), blankDto(client, "hack")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listForClient_returnsCrossNutri() {
        User nutriA = createNutri();
        User nutriB = createNutri();
        User client = createClient();
        linkActive(nutriA, client);
        linkActive(nutriB, client);
        service.create(nutriA, client.getId(), null, blankDto(client, "A"), false);
        service.create(nutriB, client.getId(), null, blankDto(client, "B"), false);

        List<ConsultationDto> all = service.listForClient(client.getId());
        assertThat(all).hasSize(2);
    }

    private ConsultationDto blankDto(User client, String complaint) {
        return new ConsultationDto(null, client.getId(), null, null, null, null,
                LocalDateTime.now(), ConsultationType.RETURN,
                complaint, null, null, null, null, null, null, null);
    }

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

    private String cpf() { return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)); }

    private void linkActive(User nutri, User client) {
        linkRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client)
                .invitedByEmail(client.getEmail()).inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
                .build());
    }
}
