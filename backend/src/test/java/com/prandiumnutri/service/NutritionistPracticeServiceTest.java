package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.CompanyRepository;
import com.prandiumnutri.repository.NutritionistPracticeRepository;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NutritionistPracticeServiceTest {

    @Autowired NutritionistPracticeService service;
    @Autowired NutritionistPracticeRepository repo;
    @Autowired UserRepository userRepo;
    @Autowired CompanyRepository companyRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void setup() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    @Test
    void ensureParticular_isIdempotent() {
        User nutri = createNutri("Dra A", "Foco em emagrecimento", "vegetariana");
        var p1 = service.ensureParticular(nutri);
        var p2 = service.ensureParticular(nutri);
        assertThat(p1.getId()).isEqualTo(p2.getId());
        assertThat(service.listByNutri(nutri.getId())).hasSize(1);
    }

    @Test
    void ensureParticular_copiaBioDoUser() {
        User nutri = createNutri("Dra B", "Bio original", "diabetes,hipertensao");
        var p = service.ensureParticular(nutri);
        assertThat(p.getBio()).isEqualTo("Bio original");
        assertThat(p.getSpecialties()).isEqualTo("diabetes,hipertensao");
        assertThat(p.isParticular()).isTrue();
        assertThat(p.getStatus()).isEqualTo(PracticeStatus.ACTIVE);
    }

    @Test
    void ensureForCompany_criaVinculadaEIdempotente() {
        User nutri = createNutri("Dra C", null, null);
        User owner = createOwner();
        Company co = companyRepo.save(Company.builder()
                .owner(owner).name("Co1").contactEmail("co1@x.com").build());

        var p1 = service.ensureForCompany(nutri, co);
        var p2 = service.ensureForCompany(nutri, co);
        assertThat(p1.getId()).isEqualTo(p2.getId());
        assertThat(p1.isParticular()).isFalse();
        assertThat(p1.getCompany().getId()).isEqualTo(co.getId());
    }

    @Test
    void suspendForCompany_marcaSuspended() {
        User nutri = createNutri("Dra D", null, null);
        User owner = createOwner();
        Company co = companyRepo.save(Company.builder()
                .owner(owner).name("Co2").contactEmail("co2@x.com").build());
        service.ensureForCompany(nutri, co);

        service.suspendForCompany(nutri.getId(), co.getId());

        var p = repo.findForCompany(nutri.getId(), co.getId()).orElseThrow();
        assertThat(p.getStatus()).isEqualTo(PracticeStatus.SUSPENDED);
    }

    @Test
    void ensureForCompany_reativaPracticeSuspensa() {
        User nutri = createNutri("Dra E", null, null);
        User owner = createOwner();
        Company co = companyRepo.save(Company.builder()
                .owner(owner).name("Co3").contactEmail("co3@x.com").build());
        service.ensureForCompany(nutri, co);
        service.suspendForCompany(nutri.getId(), co.getId());

        service.ensureForCompany(nutri, co);
        var p = repo.findForCompany(nutri.getId(), co.getId()).orElseThrow();
        assertThat(p.getStatus()).isEqualTo(PracticeStatus.ACTIVE);
    }

    @Test
    void nutri_podeTerParticularEVarias() {
        User nutri = createNutri("Dra F", null, null);
        Company a = companyRepo.save(Company.builder().owner(createOwner()).name("A").contactEmail("a@x.com").build());
        Company b = companyRepo.save(Company.builder().owner(createOwner()).name("B").contactEmail("b@x.com").build());

        service.ensureParticular(nutri);
        service.ensureForCompany(nutri, a);
        service.ensureForCompany(nutri, b);

        var list = service.listByNutri(nutri.getId());
        assertThat(list).hasSize(3);
        // Particular vem primeiro
        assertThat(list.get(0).isParticular()).isTrue();
    }

    private User createNutri(String name, String bio, String specialties) {
        return userRepo.save(User.builder()
                .name(name).email("nutri-" + System.nanoTime() + "@x.com")
                .passwordHash("$2a$10$x").role(Role.NUTRITIONIST).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .bio(bio).specialties(specialties).build());
    }

    @Test
    void endLinkWithCompany_semParticular_converteVinculadaEmParticular() {
        User nutri = createNutri("Sem Particular", null, null);
        // Apaga o particular gerado (nosso ensureParticular nao foi chamado)
        // Cria apenas a vinculada
        Company co = companyRepo.save(Company.builder()
                .owner(createOwner()).name("Co").contactEmail("c@x.com").build());
        var vinculada = service.ensureForCompany(nutri, co);
        assertThat(vinculada.isParticular()).isFalse();
        // Assert precondition: nutri sem particular
        assertThat(repo.findParticular(nutri.getId())).isEmpty();

        boolean converted = service.endLinkWithCompany(nutri.getId(), co.getId());

        assertThat(converted).isTrue();
        var after = repo.findById(vinculada.getId()).orElseThrow();
        assertThat(after.getCompany()).isNull();
        assertThat(after.getStatus()).isEqualTo(PracticeStatus.ACTIVE);
        assertThat(repo.findParticular(nutri.getId())).isPresent();
    }

    @Test
    void endLinkWithCompany_comParticular_apenasSuspende() {
        User nutri = createNutri("Com Particular", null, null);
        service.ensureParticular(nutri);
        Company co = companyRepo.save(Company.builder()
                .owner(createOwner()).name("Co2").contactEmail("c2@x.com").build());
        var vinculada = service.ensureForCompany(nutri, co);

        boolean converted = service.endLinkWithCompany(nutri.getId(), co.getId());

        assertThat(converted).isFalse();
        var after = repo.findById(vinculada.getId()).orElseThrow();
        assertThat(after.getCompany()).isNotNull();
        assertThat(after.getStatus()).isEqualTo(PracticeStatus.SUSPENDED);
    }

    @Test
    void availablePractices_incluiApenasActiveEOptIn() {
        User optIn = createNutri("Opt Sim", "Bio1", null);
        User optOut = createNutri("Opt Nao", null, null);
        var pSim = service.ensureParticular(optIn);
        pSim.setAcceptsNewClients(true);
        var pNao = service.ensureParticular(optOut);
        pNao.setAcceptsNewClients(false);

        // Practice suspensa nao aparece mesmo com opt-in
        User suspenso = createNutri("Suspenso", null, null);
        Company co = companyRepo.save(Company.builder()
                .owner(createOwner()).name("Susp").contactEmail("s@x.com").build());
        var pSusp = service.ensureForCompany(suspenso, co);
        pSusp.setAcceptsNewClients(true);
        pSusp.setStatus(PracticeStatus.SUSPENDED);

        var list = service.availablePractices();
        assertThat(list).extracting(p -> p.getNutritionist().getName())
                .contains("Opt Sim")
                .doesNotContain("Opt Nao", "Suspenso");
    }

    private User createOwner() {
        return userRepo.save(User.builder()
                .name("Owner").email("owner-" + System.nanoTime() + "@x.com")
                .passwordHash("$2a$10$x").role(Role.COMPANY).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now()).build());
    }
}
