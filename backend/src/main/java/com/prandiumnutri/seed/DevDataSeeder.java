package com.prandiumnutri.seed;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.service.NutritionistPracticeService;
import com.prandiumnutri.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seed de dev — popula banco vazio com 2 empresas, 4 nutris e 10 clientes,
 * incluindo subscriptions, practices, vinculos NCL e CN.
 *
 * Roda depois do {@link DataSeeder} (admin base). Idempotente:
 * so cria se {@code users.count() < 3} (evita duplicar em banco populado).
 *
 * Senha de todos os usuarios: {@code admin@123}. So usar em dev.
 */
@Component
@Profile("dev")
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final CompanyMemberRepository memberRepo;
    private final CompanyNutritionistRepository cnRepo;
    private final NutritionistClientLinkRepository nclRepo;
    private final NutritionistPracticeService practiceService;
    private final SubscriptionService subscriptionService;
    private final PasswordEncoder passwordEncoder;

    private static final String DEV_PASSWORD = "admin@123";
    private long cpfCounter = 10000000000L;

    @Override
    public void run(String... args) {
        long userCount = userRepo.count();
        if (userCount >= 3) {
            log.info("DevDataSeeder: banco ja populado ({} usuarios); skip.", userCount);
            return;
        }
        log.info("DevDataSeeder: populando dados de desenvolvimento...");

        // --- 2 empresas + owners ---
        User marcos = createUser("Marcos Silva", "marcos@nutripro.com", Role.COMPANY, null);
        User rafael = createUser("Rafael Souza", "rafael@vidaativa.com", Role.COMPANY, null);
        subscriptionService.createFreeSubscription(PlanTarget.USER, marcos.getId(), "BRL"); // owner tambem tem user
        subscriptionService.createFreeSubscription(PlanTarget.USER, rafael.getId(), "BRL");

        Company nutripro = companyRepo.save(Company.builder()
                .owner(marcos).name("NutriPro Saude Ltda").tradeName("NutriPro")
                .taxId("12345678000190").countryCode("BR")
                .contactEmail("contato@nutripro.com").contactPhone("+55 11 9999-0001")
                .active(true).build());
        Company vidaAtiva = companyRepo.save(Company.builder()
                .owner(rafael).name("Vida Ativa Consultoria").tradeName("Vida Ativa")
                .taxId("98765432000110").countryCode("BR")
                .contactEmail("contato@vidaativa.com").contactPhone("+55 21 9999-0002")
                .active(true).build());
        subscriptionService.createFreeSubscription(PlanTarget.COMPANY, nutripro.getId(), "BRL");
        subscriptionService.createFreeSubscription(PlanTarget.COMPANY, vidaAtiva.getId(), "BRL");

        // Ana como CompanyMember admin da NutriPro (owner ja e Marcos)
        User ana = createUser("Ana Costa", "ana@nutripro.com", Role.COMPANY, null);
        subscriptionService.createFreeSubscription(PlanTarget.USER, ana.getId(), "BRL");
        memberRepo.save(CompanyMember.builder()
                .company(nutripro).user(ana).invitedByEmail(ana.getEmail())
                .inviteToken(UUID.randomUUID().toString()).roleInCompany(CompanyMemberRole.ADMIN)
                .status(LinkStatus.ACTIVE).invitedAt(LocalDateTime.now())
                .acceptedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusYears(1))
                .build());

        // --- 4 nutris (2 na NutriPro, 1 na Vida Ativa, 1 independente) ---
        User camila = createUser("Dra. Camila Rocha", "camila@nutripro.com", Role.NUTRITIONIST, "CRN3 12345");
        User bruno = createUser("Dr. Bruno Almeida", "bruno@nutripro.com", Role.NUTRITIONIST, "CRN3 23456");
        User larissa = createUser("Dra. Larissa Ferreira", "larissa@vidaativa.com", Role.NUTRITIONIST, "CRN4 34567");
        User julia = createUser("Dra. Julia Mendes", "julia@indep.com", Role.NUTRITIONIST, "CRN3 45678");
        for (User n : List.of(camila, bruno, larissa, julia)) {
            subscriptionService.createFreeSubscription(PlanTarget.NUTRITIONIST, n.getId(), "BRL");
            practiceService.ensureParticular(n);
        }

        // Vinculo empresa <-> nutri
        linkNutriToCompany(nutripro, camila);
        linkNutriToCompany(nutripro, bruno);
        linkNutriToCompany(vidaAtiva, larissa);
        practiceService.ensureForCompany(camila, nutripro);
        practiceService.ensureForCompany(bruno, nutripro);
        practiceService.ensureForCompany(larissa, vidaAtiva);

        // --- 10 clientes ---
        User joao = createUser("Joao Silva", "joao@cliente.com", Role.USER, null);
        User maria = createUser("Maria Oliveira", "maria@cliente.com", Role.USER, null);
        User pedro = createUser("Pedro Santos", "pedro@cliente.com", Role.USER, null);
        User carla = createUser("Carla Lima", "carla@cliente.com", Role.USER, null);
        User lucas = createUser("Lucas Pereira", "lucas@cliente.com", Role.USER, null);
        User fernanda = createUser("Fernanda Costa", "fernanda@cliente.com", Role.USER, null);
        User roberto = createUser("Roberto Alves", "roberto@cliente.com", Role.USER, null);
        User beatriz = createUser("Beatriz Melo", "beatriz@cliente.com", Role.USER, null);
        User gabriel = createUser("Gabriel Nunes", "gabriel@cliente.com", Role.USER, null);
        User isabela = createUser("Isabela Rodrigues", "isabela@cliente.com", Role.USER, null);
        for (User c : List.of(joao, maria, pedro, carla, lucas, fernanda, roberto, beatriz, gabriel, isabela)) {
            subscriptionService.createFreeSubscription(PlanTarget.USER, c.getId(), "BRL");
        }

        // Vinculos nutri <-> cliente (2 por nutri; ultimos 2 clientes sem vinculo)
        linkClientToNutri(camila, joao);
        linkClientToNutri(camila, maria);
        linkClientToNutri(bruno, pedro);
        linkClientToNutri(bruno, carla);
        linkClientToNutri(larissa, lucas);
        linkClientToNutri(larissa, fernanda);
        linkClientToNutri(julia, roberto);
        linkClientToNutri(julia, beatriz);

        log.info("DevDataSeeder: seed completo (2 empresas, 4 nutris, 10 clientes, senha admin@123)");
    }

    private User createUser(String name, String email, Role role, String crn) {
        return userRepo.save(User.builder()
                .name(name).email(email)
                .cpf(String.format("%011d", cpfCounter++))
                .crn(crn)
                .passwordHash(passwordEncoder.encode(DEV_PASSWORD))
                .role(role).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .build());
    }

    private void linkNutriToCompany(Company c, User nutri) {
        cnRepo.save(CompanyNutritionist.builder()
                .company(c).nutritionist(nutri)
                .invitedByEmail(nutri.getEmail())
                .inviteToken(UUID.randomUUID() + UUID.randomUUID().toString().replace("-", ""))
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).acceptedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build());
    }

    private void linkClientToNutri(User nutri, User client) {
        var practice = practiceService.ensureParticular(nutri);
        nclRepo.save(NutritionistClientLink.builder()
                .nutritionist(nutri).practice(practice).client(client)
                .invitedByEmail(client.getEmail())
                .inviteToken(UUID.randomUUID() + UUID.randomUUID().toString().replace("-", ""))
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now()).acceptedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .initiatedByClient(false).build());
        subscriptionService.sponsorClientToPlus(client.getId(), practice);
    }
}
