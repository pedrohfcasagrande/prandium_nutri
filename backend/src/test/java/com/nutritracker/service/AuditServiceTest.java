package com.nutritracker.service;

import com.nutritracker.domain.AuditAction;
import com.nutritracker.domain.Role;
import com.nutritracker.repository.AuditLogRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuditServiceTest {

    @Autowired AuditService auditService;
    @Autowired AuditLogRepository repository;

    @Test
    void recordsAsyncLoginEntry() {
        long before = repository.count();
        auditService.record(AuditAction.LOGIN, 42L, Role.USER);

        // Escrita e assincrona - aguarda ate 2s
        Awaitility.await().atMost(Duration.ofSeconds(2))
                .until(() -> repository.count() > before);

        var last = repository.findAll().stream()
                .filter(a -> a.getAction() == AuditAction.LOGIN && Long.valueOf(42L).equals(a.getActorUserId()))
                .findFirst().orElseThrow();
        assertThat(last.getActorRole()).isEqualTo(Role.USER);
        assertThat(last.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    void recordsCountByAction() {
        auditService.record(AuditAction.MEAL_LOGGED, 100L, Role.USER, "Meal", 1L);
        auditService.record(AuditAction.MEAL_LOGGED, 100L, Role.USER, "Meal", 2L);

        Awaitility.await().atMost(Duration.ofSeconds(2)).until(() ->
                repository.countByActionSince(LocalDateTime.now().minusMinutes(1)).stream()
                        .anyMatch(r -> r.getAction() == AuditAction.MEAL_LOGGED && r.getCount() >= 2));
    }
}
