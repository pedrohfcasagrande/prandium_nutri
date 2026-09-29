package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.AppointmentDto;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 9G-3: appointment ONLINE sem meetingUrl → gera Jitsi Meet automatico.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentMeetingUrlTest {

    @Autowired AppointmentService service;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistClientLinkRepository linkRepo;
    @Autowired NutritionistPracticeRepository practiceRepo;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void requestByClient_online_generatesJitsiUrlWhenMissing() {
        User nutri = createNutri();
        User client = createClient();
        activeLink(nutri, client);

        AppointmentDto ap = service.requestByClient(client, nutri.getId(),
                LocalDateTime.now().plusDays(1), 60,
                AppointmentLocation.ONLINE, null, null, "consulta");

        assertThat(ap.meetingUrl()).startsWith("https://meet.jit.si/PrandiumNutri-");
    }

    @Test
    void requestByClient_online_preservesUserProvidedUrl() {
        User nutri = createNutri();
        User client = createClient();
        activeLink(nutri, client);

        String manual = "https://zoom.us/j/9999999999";
        AppointmentDto ap = service.requestByClient(client, nutri.getId(),
                LocalDateTime.now().plusDays(1), 60,
                AppointmentLocation.ONLINE, null, manual, null);

        assertThat(ap.meetingUrl()).isEqualTo(manual);
    }

    @Test
    void requestByClient_inPerson_doesNotGenerateUrl() {
        User nutri = createNutri();
        User client = createClient();
        activeLink(nutri, client);

        AppointmentDto ap = service.requestByClient(client, nutri.getId(),
                LocalDateTime.now().plusDays(1), 60,
                AppointmentLocation.IN_PERSON, "Rua X, 123", null, null);

        assertThat(ap.meetingUrl()).isNull();
    }

    // ---------- helpers ----------

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

    private NutritionistClientLink activeLink(User nutri, User client) {
        NutritionistPractice practice = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(null)
                .acceptsNewClients(true)
                .status(PracticeStatus.ACTIVE).build());
        return linkRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client).practice(practice)
                .invitedByEmail(client.getEmail())
                .inviteToken(UUID.randomUUID().toString())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
    }

    private String cpf() {
        return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L));
    }
}
