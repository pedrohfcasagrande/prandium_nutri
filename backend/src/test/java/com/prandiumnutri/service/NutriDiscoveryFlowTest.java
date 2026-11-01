package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NutriDiscoveryFlowTest {

    @Autowired NutritionistLinkService linkService;
    @Autowired NotificationService notificationService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired SubscriptionRepository subscriptionRepo;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void requestLink_failsWhenClientOnFreePlan() {
        User client = createPlusClient();
        // Rebaixa para FREE
        var sub = subscriptionService.effectiveOf(PlanTarget.USER, client.getId()).orElseThrow();
        sub.setPlan(planRepo.findById("CLIENT_FREE").orElseThrow());
        subscriptionRepo.save(sub);

        User nutri = createNutriAccepting();

        assertThatThrownBy(() -> linkService.requestLink(client, nutri.getId(), null))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("findNutritionist");
    }

    @Test
    void requestLink_failsWhenNutriNotAcceptingNewClients() {
        User client = createPlusClient();
        User nutri = createUser(Role.NUTRITIONIST);   // acceptsNewClients=false por default

        assertThatThrownBy(() -> linkService.requestLink(client, nutri.getId(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aceita novos clientes");
    }

    @Test
    void requestLink_createsPendingAndNotifiesNutri() {
        User client = createPlusClient();
        User nutri = createNutriAccepting();

        var link = linkService.requestLink(client, nutri.getId(), "gostaria de ajuda");
        assertThat(link.status()).isEqualTo(LinkStatus.PENDING);
        assertThat(notificationService.unreadCount(nutri.getId())).isEqualTo(1);
    }

    @Test
    void acceptRequest_activatesAndNotifiesClient() {
        User client = createPlusClient();
        User nutri = createNutriAccepting();

        var link = linkService.requestLink(client, nutri.getId(), null);
        linkService.acceptRequest(nutri, link.id());

        assertThat(notificationService.unreadCount(client.getId())).isGreaterThanOrEqualTo(1);
        assertThat(linkService.hasActiveLink(nutri.getId(), client.getId())).isTrue();
    }

    @Test
    void rejectRequest_recordsNoteAndNotifiesClient() {
        User client = createPlusClient();
        User nutri = createNutriAccepting();

        var link = linkService.requestLink(client, nutri.getId(), null);
        linkService.rejectRequest(nutri, link.id(), "agenda cheia no momento");

        var requests = linkService.listClientRequests(nutri.getId());
        assertThat(requests).isEmpty();
        assertThat(notificationService.unreadCount(client.getId())).isGreaterThanOrEqualTo(1);
    }

    private User createUser(Role role) {
        return userRepo.save(User.builder()
                .name("U-" + role).email(role.name().toLowerCase() + "-" + System.nanoTime() + "@x.com")
                .role(role).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    private User createPlusClient() {
        User client = createUser(Role.USER);
        // Cria FREE (default) e depois muda o plan para PLUS
        var sub = subscriptionService.createFreeSubscription(PlanTarget.USER, client.getId(), "BRL");
        sub.setPlan(planRepo.findById("CLIENT_PLUS").orElseThrow());
        subscriptionRepo.save(sub);
        return client;
    }

    private User createNutriAccepting() {
        User nutri = createUser(Role.NUTRITIONIST);
        nutri.setAcceptsNewClients(true);
        return userRepo.save(nutri);
    }
}
