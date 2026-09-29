package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.ChatMessageDto;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.NutritionistPracticeRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.UserRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatServiceTest {

    @Autowired ChatService chatService;
    @Autowired NutritionistClientLinkRepository linkRepo;
    @Autowired NutritionistPracticeRepository practiceRepo;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void seedPlans() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    private User makeUser(Role role) {
        return userRepo.save(User.builder()
                .name("U-" + role + "-" + System.nanoTime())
                .email("chat-" + role + "-" + System.nanoTime() + "@x.com")
                .role(role).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x")
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
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

    @Test
    void send_persistsAndUpdatesUnread() {
        User nutri = makeUser(Role.NUTRITIONIST);
        User client = makeUser(Role.USER);
        NutritionistClientLink link = activeLink(nutri, client);

        ChatMessageDto dto = chatService.send(nutri, link.getId(), "Ola, como vai?");
        assertThat(dto.id()).isNotNull();
        assertThat(dto.senderId()).isEqualTo(nutri.getId());

        // client tem 1 nao-lido, nutri (sender) tem 0
        assertThat(chatService.unreadCount(client, link.getId())).isEqualTo(1);
        assertThat(chatService.unreadCount(nutri, link.getId())).isZero();
    }

    @Test
    void list_returnsInChronologicalOrder() {
        User nutri = makeUser(Role.NUTRITIONIST);
        User client = makeUser(Role.USER);
        NutritionistClientLink link = activeLink(nutri, client);

        chatService.send(nutri, link.getId(), "1");
        chatService.send(client, link.getId(), "2");
        chatService.send(nutri, link.getId(), "3");

        List<ChatMessageDto> list = chatService.list(client, link.getId());
        assertThat(list).extracting(ChatMessageDto::content).containsExactly("1", "2", "3");
    }

    @Test
    void markRead_marksOnlyOtherSideMessages() {
        User nutri = makeUser(Role.NUTRITIONIST);
        User client = makeUser(Role.USER);
        NutritionistClientLink link = activeLink(nutri, client);

        chatService.send(nutri, link.getId(), "a");
        chatService.send(nutri, link.getId(), "b");
        chatService.send(client, link.getId(), "c");

        int updated = chatService.markRead(client, link.getId());
        assertThat(updated).isEqualTo(2);
        assertThat(chatService.unreadCount(client, link.getId())).isZero();
        // nutri ainda tem "c" nao-lida
        assertThat(chatService.unreadCount(nutri, link.getId())).isEqualTo(1);
    }

    @Test
    void send_blocksOutsider() {
        User nutri = makeUser(Role.NUTRITIONIST);
        User client = makeUser(Role.USER);
        User outsider = makeUser(Role.USER);
        NutritionistClientLink link = activeLink(nutri, client);

        assertThatThrownBy(() -> chatService.send(outsider, link.getId(), "hi"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void send_blocksInactiveLink() {
        User nutri = makeUser(Role.NUTRITIONIST);
        User client = makeUser(Role.USER);
        NutritionistClientLink link = activeLink(nutri, client);
        link.setStatus(LinkStatus.PENDING);
        linkRepo.save(link);

        assertThatThrownBy(() -> chatService.send(nutri, link.getId(), "hi"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void send_rejectsBlankOrTooLong() {
        User nutri = makeUser(Role.NUTRITIONIST);
        User client = makeUser(Role.USER);
        NutritionistClientLink link = activeLink(nutri, client);

        assertThatThrownBy(() -> chatService.send(nutri, link.getId(), ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.send(nutri, link.getId(), "x".repeat(2001)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
