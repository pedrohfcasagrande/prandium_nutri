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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SponsorshipTest {

    @Autowired NutritionistLinkService linkService;
    @Autowired NutritionistPracticeService practiceService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired UserRepository userRepo;
    @Autowired NutritionistClientLinkRepository nclRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void aceitarConvite_naoPromovePlus_quandoNutriParticularFree() {
        User nutri = createNutri();
        User client = createClient();
        // Nutri fica em NUTRI_FREE
        subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");
        subscriptionService.createFreeSubscription(PlanTarget.USER, client.getId(), "BRL");
        practiceService.ensureParticular(nutri);

        var link = linkService.invite(nutri, client.getEmail());
        linkService.acceptInvite(client, tokenOf(link.id()));

        var sub = subscriptionService.effectiveOf(PlanTarget.USER, client.getId()).orElseThrow();
        // Sponsor nao promove porque nutri esta em FREE
        assertThat(sub.getPlan().getCode()).isEqualTo("CLIENT_FREE");
        assertThat(sub.getSponsoredByPractice()).isNull();
    }

    @Test
    void aceitarConvite_ativaPlusPatrocinado_quandoNutriProParticular() {
        User nutri = createNutri();
        User client = createClient();
        upgradeToPro(nutri);
        subscriptionService.createFreeSubscription(PlanTarget.USER, client.getId(), "BRL");
        practiceService.ensureParticular(nutri);

        var link = linkService.invite(nutri, client.getEmail());
        linkService.acceptInvite(client, tokenOf(link.id()));

        var sub = subscriptionService.effectiveOf(PlanTarget.USER, client.getId()).orElseThrow();
        assertThat(sub.getPlan().getCode()).isEqualTo("CLIENT_PLUS");
        assertThat(sub.getSponsoredByPractice()).isNotNull();
    }

    @Test
    void revogarUltimoVinculo_voltaAoFree() {
        User nutri = createNutri();
        User client = createClient();
        upgradeToPro(nutri);
        subscriptionService.createFreeSubscription(PlanTarget.USER, client.getId(), "BRL");
        practiceService.ensureParticular(nutri);

        var linkDto = linkService.invite(nutri, client.getEmail());
        linkService.acceptInvite(client, tokenOf(linkDto.id()));
        assertThat(subscriptionService.effectiveOf(PlanTarget.USER, client.getId())
                .orElseThrow().getPlan().getCode()).isEqualTo("CLIENT_PLUS");

        linkService.revokeByClient(client, linkDto.id());
        var sub = subscriptionService.effectiveOf(PlanTarget.USER, client.getId()).orElseThrow();
        assertThat(sub.getPlan().getCode()).isEqualTo("CLIENT_FREE");
        assertThat(sub.getSponsoredByPractice()).isNull();
    }

    @Test
    void revogarComOutroNutriAtivo_mantemPlusViaSegundoVinculo() {
        User nutriA = createNutri();
        User nutriB = createNutri();
        User client = createClient();
        upgradeToPro(nutriA);
        upgradeToPro(nutriB);
        subscriptionService.createFreeSubscription(PlanTarget.USER, client.getId(), "BRL");
        var practiceA = practiceService.ensureParticular(nutriA);
        var practiceB = practiceService.ensureParticular(nutriB);

        var linkA = linkService.invite(nutriA, client.getEmail());
        linkService.acceptInvite(client, tokenOf(linkA.id()));
        var linkB = linkService.invite(nutriB, client.getEmail());
        linkService.acceptInvite(client, tokenOf(linkB.id()));

        linkService.revokeByNutri(nutriA, linkA.id());

        var subAfter = subscriptionService.effectiveOf(PlanTarget.USER, client.getId()).orElseThrow();
        assertThat(subAfter.getPlan().getCode()).isEqualTo("CLIENT_PLUS");
        assertThat(subAfter.getSponsoredByPractice()).isNotNull();
        assertThat(subAfter.getSponsoredByPractice().getId()).isEqualTo(practiceB.getId());
        assertThat(subAfter.getSponsoredByPractice().getId()).isNotEqualTo(practiceA.getId());
    }

    /** Ajuda: cria subscription NUTRI_PRO pro nutri (tier=PRO, "pago"). */
    private void upgradeToPro(User nutri) {
        var sub = subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, nutri.getId(), "BRL");
        var proPlan = planRepo.findById("NUTRI_PRO").orElseThrow();
        sub.setPlan(proPlan);
    }

    private String tokenOf(Long linkId) {
        return nclRepo.findById(linkId).orElseThrow().getInviteToken();
    }

    private User createNutri() {
        return userRepo.save(User.builder()
                .name("Nutri " + System.nanoTime())
                .email("nutri-" + System.nanoTime() + "@x.com")
                .passwordHash("$2a$10$x").role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).acceptsNewClients(true)
                .consentAcceptedAt(LocalDateTime.now())
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    private User createClient() {
        return userRepo.save(User.builder()
                .name("Cliente " + System.nanoTime())
                .email("cli-" + System.nanoTime() + "@x.com")
                .passwordHash("$2a$10$x").role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }
}
