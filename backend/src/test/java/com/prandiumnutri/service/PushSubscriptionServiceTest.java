package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.PushSubscriptionDto;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.PushSubscriptionRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushSubscriptionServiceTest {

    @Autowired PushSubscriptionService service;
    @Autowired PushSubscriptionRepository repository;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    private User makeUser() {
        return userRepo.save(User.builder()
                .name("Push").email("p-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x")
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    private PushSubscriptionDto dto(String endpoint) {
        return new PushSubscriptionDto(endpoint, "pkey-" + endpoint, "auth-" + endpoint, "Chrome");
    }

    @Test
    void subscribe_persistsSubscription() {
        User u = makeUser();
        var saved = service.subscribe(u, dto("https://fcm.googleapis.com/x1"));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(u.getId());
        assertThat(service.listForUser(u.getId())).hasSize(1);
    }

    @Test
    void subscribe_isIdempotentByEndpoint() {
        User u1 = makeUser();
        User u2 = makeUser();
        var first = service.subscribe(u1, dto("https://fcm.googleapis.com/same"));
        var second = service.subscribe(u2, new PushSubscriptionDto(
                first.getEndpoint(), "newpkey", "newauth", "Firefox"));
        assertThat(second.getId()).isEqualTo(first.getId()); // mesmo row atualizado
        assertThat(second.getUser().getId()).isEqualTo(u2.getId());
        assertThat(second.getP256dh()).isEqualTo("newpkey");
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void subscribe_rejectsIncomplete() {
        User u = makeUser();
        assertThatThrownBy(() -> service.subscribe(u, new PushSubscriptionDto(null, "x", "y", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.subscribe(u, new PushSubscriptionDto("ep", null, "y", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsubscribe_removesRow() {
        User u = makeUser();
        var saved = service.subscribe(u, dto("https://fcm.googleapis.com/rm"));
        service.unsubscribe(saved.getEndpoint());
        assertThat(service.listForUser(u.getId())).isEmpty();
    }

    @Test
    void listForUser_returnsOnlyOwn() {
        User u1 = makeUser();
        User u2 = makeUser();
        service.subscribe(u1, dto("a"));
        service.subscribe(u1, dto("b"));
        service.subscribe(u2, dto("c"));
        assertThat(service.listForUser(u1.getId())).hasSize(2);
        assertThat(service.listForUser(u2.getId())).hasSize(1);
    }
}
