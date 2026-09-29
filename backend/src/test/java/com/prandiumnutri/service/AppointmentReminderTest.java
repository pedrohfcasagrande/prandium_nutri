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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 9F — cobertura do job {@code sendReminders()}: janela 24h, dedup e status.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentReminderTest {

    @Autowired AppointmentService service;
    @Autowired AppointmentRepository repository;
    @Autowired UserRepository userRepo;
    @Autowired NotificationRepository notificationRepo;
    @Autowired SubscriptionService subscriptionService;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void sendReminders_deliversForConfirmedIn24hAndMarksTimestamp() {
        User nutri = createNutri();
        User client = createClient();
        Appointment ap = saveAppointment(nutri, client,
                LocalDateTime.now().plusHours(12), AppointmentStatus.CONFIRMED);

        service.sendReminders();

        Appointment reloaded = repository.findById(ap.getId()).orElseThrow();
        assertThat(reloaded.getReminderSentAt()).isNotNull();
        assertThat(notificationRepo.findByUserIdOrderByCreatedAtDesc(client.getId(), org.springframework.data.domain.PageRequest.of(0, 50)))
                .anyMatch(n -> n.getTitle().contains("Lembrete"));
        assertThat(notificationRepo.findByUserIdOrderByCreatedAtDesc(nutri.getId(), org.springframework.data.domain.PageRequest.of(0, 50)))
                .anyMatch(n -> n.getTitle().contains("Lembrete"));
    }

    @Test
    void sendReminders_skipsAlreadySent() {
        User nutri = createNutri();
        User client = createClient();
        Appointment ap = saveAppointment(nutri, client,
                LocalDateTime.now().plusHours(6), AppointmentStatus.CONFIRMED);
        ap.setReminderSentAt(LocalDateTime.now().minusHours(1));
        repository.save(ap);

        int beforeC = notificationRepo.findByUserIdOrderByCreatedAtDesc(client.getId(), org.springframework.data.domain.PageRequest.of(0, 50)).size();
        service.sendReminders();
        int afterC = notificationRepo.findByUserIdOrderByCreatedAtDesc(client.getId(), org.springframework.data.domain.PageRequest.of(0, 50)).size();

        assertThat(afterC).isEqualTo(beforeC);
    }

    @Test
    void sendReminders_ignoresNonConfirmedOrOutOfWindow() {
        User nutri = createNutri();
        User client = createClient();
        // REQUESTED, mesmo dentro da janela
        saveAppointment(nutri, client, LocalDateTime.now().plusHours(5), AppointmentStatus.REQUESTED);
        // CONFIRMED, mas fora da janela (5 dias adiante)
        saveAppointment(nutri, client, LocalDateTime.now().plusDays(5), AppointmentStatus.CONFIRMED);

        service.sendReminders();

        assertThat(notificationRepo.findByUserIdOrderByCreatedAtDesc(client.getId(), org.springframework.data.domain.PageRequest.of(0, 50)))
                .noneMatch(n -> n.getTitle().contains("Lembrete"));
    }

    // -------------------- helpers --------------------

    private Appointment saveAppointment(User nutri, User client, LocalDateTime when, AppointmentStatus status) {
        return repository.save(Appointment.builder()
                .client(client).nutritionist(nutri)
                .scheduledAt(when).durationMinutes(60)
                .location(AppointmentLocation.ONLINE)
                .status(status)
                .requestedBy(AppointmentActor.CLIENT).lastActionBy(AppointmentActor.CLIENT)
                .build());
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

    private String cpf() {
        return String.format("%011d", Math.abs(System.nanoTime() % 100000000000L));
    }
}
