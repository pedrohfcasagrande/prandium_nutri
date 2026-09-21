package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompanyClientTransferTest {

    @Autowired CompanyService companyService;
    @Autowired CompanyRepository companyRepo;
    @Autowired CompanyNutritionistRepository cnRepo;
    @Autowired NutritionistClientLinkRepository nclRepo;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;
    @Autowired NotificationRepository notifRepo;
    @Autowired NutritionistPracticeRepository practiceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void revokeWithAction_revokeClients_endsAllClientLinks() {
        var ctx = seedCompanyWithTwoNutrisAndClients();
        long linkId = ctx.cnA.getId();

        companyService.revokeWithAction(ctx.company, linkId, "REVOKE_CLIENTS", null);

        // Nutri A vinculo revogado, clientes tambem
        assertThat(cnRepo.findById(linkId).orElseThrow().getStatus())
                .isNotEqualTo(LinkStatus.ACTIVE);
        assertThat(nclRepo.existsActiveLink(ctx.nutriA.getId(), ctx.client1.getId())).isFalse();
        assertThat(nclRepo.existsActiveLink(ctx.nutriA.getId(), ctx.client2.getId())).isFalse();
        // Nutri B nao afetado
        assertThat(nclRepo.existsActiveLink(ctx.nutriB.getId(), ctx.client3.getId())).isTrue();
    }

    @Test
    void revokeWithAction_transfer_movesClientsToTarget() {
        var ctx = seedCompanyWithTwoNutrisAndClients();

        companyService.revokeWithAction(ctx.company, ctx.cnA.getId(), "TRANSFER", ctx.nutriB.getId());

        // Cliente1 e cliente2 agora com nutriB
        assertThat(nclRepo.existsActiveLink(ctx.nutriA.getId(), ctx.client1.getId())).isFalse();
        assertThat(nclRepo.existsActiveLink(ctx.nutriB.getId(), ctx.client1.getId())).isTrue();
        assertThat(nclRepo.existsActiveLink(ctx.nutriB.getId(), ctx.client2.getId())).isTrue();
    }

    @Test
    void revokeWithAction_transfer_deniedWhenTargetNotInCompany() {
        var ctx = seedCompanyWithTwoNutrisAndClients();
        User outsider = createUser(Role.NUTRITIONIST);

        assertThatThrownBy(() -> companyService.revokeWithAction(
                ctx.company, ctx.cnA.getId(), "TRANSFER", outsider.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void transferClient_movesClientBetweenCompanyNutris() {
        var ctx = seedCompanyWithTwoNutrisAndClients();

        companyService.transferClient(ctx.company, ctx.client1.getId(), ctx.nutriB.getId());

        assertThat(nclRepo.existsActiveLink(ctx.nutriA.getId(), ctx.client1.getId())).isFalse();
        assertThat(nclRepo.existsActiveLink(ctx.nutriB.getId(), ctx.client1.getId())).isTrue();
    }

    @Test
    void revokeWithAction_transfer_notifiesNutriAndClientsAndTarget() {
        var ctx = seedCompanyWithTwoNutrisAndClients();

        companyService.revokeWithAction(ctx.company, ctx.cnA.getId(), "TRANSFER", ctx.nutriB.getId());

        // Nutri revogado recebe notificacao
        assertThat(notifRepo.countByUserIdAndReadAtIsNull(ctx.nutriA.getId())).isGreaterThan(0);
        // Clientes transferidos recebem notificacao
        assertThat(notifRepo.countByUserIdAndReadAtIsNull(ctx.client1.getId())).isGreaterThan(0);
        assertThat(notifRepo.countByUserIdAndReadAtIsNull(ctx.client2.getId())).isGreaterThan(0);
        // Nutri destino tambem
        assertThat(notifRepo.countByUserIdAndReadAtIsNull(ctx.nutriB.getId())).isGreaterThan(0);
    }

    @Test
    void inviteNutri_notifiesExistingNutri() {
        User owner = createUser(Role.COMPANY);
        Company c = companyRepo.save(Company.builder()
                .owner(owner).name("Co").contactEmail("co@x.com").build());
        User nutri = createUser(Role.NUTRITIONIST);

        companyService.inviteNutri(c, nutri.getEmail());

        assertThat(notifRepo.countByUserIdAndReadAtIsNull(nutri.getId())).isGreaterThan(0);
    }

    @Test
    void listClients_returnsAllClientsAcrossNutris() {
        var ctx = seedCompanyWithTwoNutrisAndClients();
        var list = companyService.listClients(ctx.company.getId());
        assertThat(list).hasSize(3);
        assertThat(list.stream().map(x -> x.clientId()))
                .containsExactlyInAnyOrder(ctx.client1.getId(), ctx.client2.getId(), ctx.client3.getId());
    }

    /** Fase 8: cliente do nutri via practice PARTICULAR nao pode aparecer para a empresa. */
    @Test
    void listClients_doesNotLeakParticularClientsToCompany() {
        var ctx = seedCompanyWithTwoNutrisAndClients();
        // Cria practice particular para nutriA e vincula um cliente particular
        var particular = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(ctx.nutriA).company(null)
                .acceptsNewClients(true).status(PracticeStatus.ACTIVE).build());
        User particularClient = createUser(Role.USER);
        linkNutriClient(ctx.nutriA, particularClient, particular);

        var list = companyService.listClients(ctx.company.getId());
        assertThat(list).hasSize(3); // apenas os 3 vinculados via practice da empresa
        assertThat(list.stream().map(x -> x.clientId()))
                .doesNotContain(particularClient.getId());
    }

    /** Fase 8: revogar nutri via empresa nao pode encerrar vinculos particulares desse nutri. */
    @Test
    void revokeWithAction_revokeClients_doesNotTouchParticularLinks() {
        var ctx = seedCompanyWithTwoNutrisAndClients();
        var particular = practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(ctx.nutriA).company(null)
                .acceptsNewClients(true).status(PracticeStatus.ACTIVE).build());
        User particularClient = createUser(Role.USER);
        linkNutriClient(ctx.nutriA, particularClient, particular);

        companyService.revokeWithAction(ctx.company, ctx.cnA.getId(), "REVOKE_CLIENTS", null);

        assertThat(nclRepo.existsActiveLink(ctx.nutriA.getId(), particularClient.getId()))
                .as("cliente particular nao deve ser afetado por acao da empresa")
                .isTrue();
    }

    // ---- helpers ----

    private record Ctx(Company company, User nutriA, User nutriB,
                       User client1, User client2, User client3,
                       CompanyNutritionist cnA, CompanyNutritionist cnB) {}

    private Ctx seedCompanyWithTwoNutrisAndClients() {
        User owner = createUser(Role.COMPANY);
        Company c = companyRepo.save(Company.builder()
                .owner(owner).name("Co").contactEmail("co@x.com").build());
        User nutriA = createUser(Role.NUTRITIONIST);
        User nutriB = createUser(Role.NUTRITIONIST);
        var cnA = linkCompanyNutri(c, nutriA);
        var cnB = linkCompanyNutri(c, nutriB);
        var practiceA = practiceForCompany(nutriA, c);
        var practiceB = practiceForCompany(nutriB, c);
        User client1 = createUser(Role.USER);
        User client2 = createUser(Role.USER);
        User client3 = createUser(Role.USER);
        linkNutriClient(nutriA, client1, practiceA);
        linkNutriClient(nutriA, client2, practiceA);
        linkNutriClient(nutriB, client3, practiceB);
        return new Ctx(c, nutriA, nutriB, client1, client2, client3, cnA, cnB);
    }

    private User createUser(Role role) {
        return userRepo.save(User.builder()
                .name("U-" + role).email(role.name().toLowerCase() + "-" + System.nanoTime() + "@x.com")
                .role(role).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x").cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    private CompanyNutritionist linkCompanyNutri(Company c, User nutri) {
        return cnRepo.save(CompanyNutritionist.builder()
                .company(c).nutritionist(nutri)
                .invitedByEmail(nutri.getEmail()).inviteToken("t-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).acceptedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());
    }

    private void linkNutriClient(User nutri, User client, NutritionistPractice practice) {
        nclRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).client(client).practice(practice)
                .invitedByEmail(client.getEmail()).inviteToken("nc-" + System.nanoTime())
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).acceptedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .initiatedByClient(false)
                .build());
    }

    private NutritionistPractice practiceForCompany(User nutri, Company company) {
        return practiceRepo.save(NutritionistPractice.builder()
                .nutritionist(nutri).company(company)
                .acceptsNewClients(true)
                .status(PracticeStatus.ACTIVE)
                .build());
    }
}
