package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.NotificationType;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.NotificationRepository;
import com.prandiumnutri.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository repository;
    @Autowired UserRepository userRepository;

    @Test
    void notify_savesRowAndCountsUnread() {
        User u = createUser();
        notificationService.notify(u.getId(), NotificationType.GENERIC, "Ola", "corpo", null, null);
        assertThat(notificationService.unreadCount(u.getId())).isEqualTo(1);
        assertThat(notificationService.list(u.getId(), false, 10)).hasSize(1);
    }

    @Test
    void markRead_zeroesUnreadForCallerOnly() {
        User u = createUser();
        var n = notificationService.notify(u.getId(), NotificationType.GENERIC, "t", "b", null, null);
        notificationService.markRead(u.getId(), n.getId());
        assertThat(notificationService.unreadCount(u.getId())).isZero();
        // outro user nao afeta contagem
        User other = createUser();
        notificationService.notify(other.getId(), NotificationType.GENERIC, "x", "y", null, null);
        assertThat(notificationService.unreadCount(u.getId())).isZero();
        assertThat(notificationService.unreadCount(other.getId())).isEqualTo(1);
    }

    @Test
    void markRead_ignoresWhenOwnerDoesNotMatch() {
        User u = createUser();
        User attacker = createUser();
        var n = notificationService.notify(u.getId(), NotificationType.GENERIC, "t", "b", null, null);
        notificationService.markRead(attacker.getId(), n.getId());  // silencioso
        assertThat(notificationService.unreadCount(u.getId())).isEqualTo(1);
    }

    @Test
    void markAllRead_marksEveryUnreadForUser() {
        User u = createUser();
        for (int i = 0; i < 3; i++) {
            notificationService.notify(u.getId(), NotificationType.GENERIC, "t" + i, null, null, null);
        }
        int marked = notificationService.markAllRead(u.getId());
        assertThat(marked).isEqualTo(3);
        assertThat(notificationService.unreadCount(u.getId())).isZero();
    }

    private User createUser() {
        return userRepository.save(User.builder()
                .name("U").email("u-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }
}
