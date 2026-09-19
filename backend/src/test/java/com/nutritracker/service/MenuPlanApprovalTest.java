package com.nutritracker.service;

import com.nutritracker.domain.*;
import com.nutritracker.dto.MenuPlanDto;
import com.nutritracker.repository.*;
import com.nutritracker.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MenuPlanApprovalTest {

    @Autowired MenuPlanService menuPlanService;
    @Autowired NotificationService notificationService;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistClientLinkRepository linkRepo;
    @Autowired MenuPlanRepository menuPlanRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void submit_movesToPendingAndNotifiesNutri() {
        User nutri = createUser(Role.NUTRITIONIST);
        User client = createUser(Role.USER);
        linkActive(nutri, client);

        MenuPlanDto plan = createPlanFor(client);
        MenuPlanDto submitted = menuPlanService.submit(client.getId(), plan.id());

        assertThat(submitted.status()).isEqualTo(MenuPlanStatus.PENDING);
        assertThat(submitted.submittedToNutriId()).isEqualTo(nutri.getId());
        assertThat(notificationService.unreadCount(nutri.getId())).isEqualTo(1);
    }

    @Test
    void submit_failsWithoutActiveNutri() {
        User client = createUser(Role.USER);
        MenuPlanDto plan = createPlanFor(client);
        assertThatThrownBy(() -> menuPlanService.submit(client.getId(), plan.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nutricionista");
    }

    @Test
    void approve_setsStatusApprovedAndNotifiesClient() {
        User nutri = createUser(Role.NUTRITIONIST);
        User client = createUser(Role.USER);
        linkActive(nutri, client);
        MenuPlanDto plan = createPlanFor(client);
        menuPlanService.submit(client.getId(), plan.id());

        MenuPlanDto approved = menuPlanService.approve(nutri, plan.id());
        assertThat(approved.status()).isEqualTo(MenuPlanStatus.APPROVED);
        assertThat(notificationService.unreadCount(client.getId())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void reject_recordsReasonAndNotifies() {
        User nutri = createUser(Role.NUTRITIONIST);
        User client = createUser(Role.USER);
        linkActive(nutri, client);
        MenuPlanDto plan = createPlanFor(client);
        menuPlanService.submit(client.getId(), plan.id());

        MenuPlanDto rejected = menuPlanService.reject(nutri, plan.id(), "faltam vegetais");
        assertThat(rejected.status()).isEqualTo(MenuPlanStatus.REJECTED);
        assertThat(rejected.reviewNote()).isEqualTo("faltam vegetais");
    }

    @Test
    void approve_deniesWhenSubmittedToDifferentNutri() {
        User nutri1 = createUser(Role.NUTRITIONIST);
        User nutri2 = createUser(Role.NUTRITIONIST);
        User client = createUser(Role.USER);
        linkActive(nutri1, client);
        MenuPlanDto plan = createPlanFor(client);
        menuPlanService.submit(client.getId(), plan.id());

        assertThatThrownBy(() -> menuPlanService.approve(nutri2, plan.id()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editApprovedPlan_whenUserHasNutri_resetsToDraft() {
        User nutri = createUser(Role.NUTRITIONIST);
        User client = createUser(Role.USER);
        linkActive(nutri, client);
        MenuPlanDto plan = createPlanFor(client);
        // vira APPROVED via approve
        menuPlanService.submit(client.getId(), plan.id());
        menuPlanService.approve(nutri, plan.id());
        assertThat(menuPlanService.list(client.getId()).get(0).status()).isEqualTo(MenuPlanStatus.APPROVED);

        // usuario edita conteudo -> deve voltar para DRAFT + inativo
        MenuPlanDto edited = menuPlanService.save(client.getId(), new MenuPlanDto(
                plan.id(), plan.name(), MenuPlanSource.MANUAL, "conteudo novo e diferente",
                null, null, false, null,
                null, null, null, null, null, null, null, null));
        assertThat(edited.status()).isEqualTo(MenuPlanStatus.DRAFT);
        assertThat(edited.active()).isFalse();
        assertThat(edited.submittedToNutriId()).isNull();
        assertThat(edited.reviewedAt()).isNull();
    }

    @Test
    void createPlan_whenUserHasNutri_startsAsDraft() {
        User nutri = createUser(Role.NUTRITIONIST);
        User client = createUser(Role.USER);
        linkActive(nutri, client);
        MenuPlanDto plan = createPlanFor(client);
        assertThat(plan.status()).isEqualTo(MenuPlanStatus.DRAFT);
    }

    @Test
    void createPlan_whenUserHasNoNutri_startsAsApproved() {
        User client = createUser(Role.USER);
        MenuPlanDto plan = createPlanFor(client);
        assertThat(plan.status()).isEqualTo(MenuPlanStatus.APPROVED);
    }

    @Test
    void setActive_deniedForPendingPlan() {
        User nutri = createUser(Role.NUTRITIONIST);
        User client = createUser(Role.USER);
        linkActive(nutri, client);
        MenuPlanDto plan = createPlanFor(client);
        menuPlanService.submit(client.getId(), plan.id());

        assertThatThrownBy(() -> menuPlanService.setActive(client.getId(), plan.id(), true))
                .isInstanceOf(IllegalStateException.class);
    }

    private User createUser(Role role) {
        return userRepo.save(User.builder()
                .name("U-" + role).email(role.name().toLowerCase() + "-" + System.nanoTime() + "@x.com")
                .role(role).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").build());
    }

    private void linkActive(User nutri, User client) {
        linkRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client)
                .invitedByEmail(client.getEmail()).inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(30))
                .build());
    }

    private MenuPlanDto createPlanFor(User user) {
        return menuPlanService.save(user.getId(), new MenuPlanDto(
                null, "Cardapio", MenuPlanSource.MANUAL, "conteudo do cardapio",
                null, null, false, null,
                null, null, null, null, null, null, null, null));
    }
}
