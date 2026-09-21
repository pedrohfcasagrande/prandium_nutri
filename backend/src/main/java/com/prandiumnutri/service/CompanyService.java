package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.*;
import com.prandiumnutri.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository memberRepository;
    private final CompanyNutritionistRepository cnRepository;
    private final NutritionistClientLinkRepository nclRepository;
    private final UserRepository userRepository;
    private final MealRepository mealRepository;
    private final MailService mailService;
    private final FeatureGateService featureGate;
    private final NotificationService notificationService;
    private final NutritionistPracticeService practiceService;
    private final SubscriptionService subscriptionService;

    @Value("${prandiumnutri.invite.expiration-days:14}")
    private int expirationDays;

    @Value("${prandiumnutri.reset.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // --- Consulta ---

    /**
     * Resolve a empresa do usuario atual: seja como owner OU como membro ativo (admin).
     */
    @Transactional(readOnly = true)
    public Company getByOwner(Long userId) {
        // Primeiro tenta como owner direto
        var byOwner = companyRepository.findByOwnerId(userId);
        if (byOwner.isPresent()) return byOwner.get();
        // Se nao, procura via CompanyMember ACTIVE
        var members = memberRepository.findByUserIdAndStatus(userId, LinkStatus.ACTIVE);
        if (!members.isEmpty()) {
            return members.get(0).getCompany();
        }
        throw new EntityNotFoundException("Empresa nao encontrada");
    }

    @Transactional(readOnly = true)
    public CompanyDto toDto(Company c) {
        return new CompanyDto(c.getId(), c.getName(), c.getTradeName(), c.getTaxId(),
                c.getCountryCode(), c.getContactEmail(), c.getContactPhone(), c.isActive());
    }

    @Transactional
    public CompanyDto updateOwn(User owner, CompanyDto dto) {
        Company c = getByOwner(owner.getId());
        c.setName(dto.name());
        c.setTradeName(dto.tradeName());
        c.setTaxId(dto.taxId());
        c.setCountryCode(dto.countryCode().toUpperCase());
        c.setContactEmail(dto.contactEmail());
        c.setContactPhone(dto.contactPhone());
        return toDto(companyRepository.save(c));
    }

    // --- Convite Company -> Nutritionist ---

    @Transactional(readOnly = true)
    public List<CompanyNutriLinkDto> listNutritionists(Long companyId) {
        return listNutritionists(companyId, null, null);
    }

    /** Fase 6B: q busca em nome/email/cpf/crn; sort = 'name' | 'invitedAt' (default desc). */
    @Transactional(readOnly = true)
    public List<CompanyNutriLinkDto> listNutritionists(Long companyId, String q, String sort) {
        String needle = q == null ? null : q.trim().toLowerCase();
        String digits = q == null ? "" : q.replaceAll("\\D", "");
        var stream = cnRepository.findByCompanyIdOrderByInvitedAtDesc(companyId).stream();
        if (needle != null && !needle.isEmpty()) {
            stream = stream.filter(cn -> {
                User n = cn.getNutritionist();
                String email = n != null ? n.getEmail() : cn.getInvitedByEmail();
                if (n != null && n.getName() != null && n.getName().toLowerCase().contains(needle)) return true;
                if (email != null && email.toLowerCase().contains(needle)) return true;
                if (n != null && !digits.isEmpty() && n.getCpf() != null && n.getCpf().contains(digits)) return true;
                if (n != null && n.getCrn() != null && n.getCrn().toLowerCase().contains(needle)) return true;
                return false;
            });
        }
        java.util.Comparator<com.prandiumnutri.domain.CompanyNutritionist> comp = !"created".equalsIgnoreCase(sort)
                ? java.util.Comparator.comparing(cn -> {
                    var n = cn.getNutritionist();
                    return (n != null && n.getName() != null ? n.getName() : "").toLowerCase();
                  })
                : java.util.Comparator.comparing(
                        (com.prandiumnutri.domain.CompanyNutritionist cn) -> cn.getInvitedAt() == null ? LocalDateTime.MIN : cn.getInvitedAt()
                  ).reversed();
        return stream.sorted(comp).map(cn -> {
            User n = cn.getNutritionist();
            long clients = (n != null) ? nclRepository.countActiveOrPending(n.getId()) : 0;
            return new CompanyNutriLinkDto(
                    cn.getId(),
                    n != null ? n.getId() : null,
                    n != null ? n.getName() : "(pendente)",
                    n != null ? n.getEmail() : cn.getInvitedByEmail(),
                    n != null ? com.prandiumnutri.support.CpfUtil.mask(n.getCpf()) : null,
                    n != null ? n.getCrn() : null,
                    cn.getStatus(), cn.getInvitedAt(), cn.getAcceptedAt(),
                    clients
            );
        }).toList();
    }

    @Transactional
    public CompanyNutriLinkDto inviteNutri(Company company, String nutriEmail) {
        featureGate.assertCanAddNutritionist(company.getId());
        String email = nutriEmail.trim().toLowerCase();
        User existing = userRepository.findByEmail(email).orElse(null);

        if (existing != null && existing.getRole() != Role.NUTRITIONIST) {
            throw new IllegalArgumentException(
                    "O e-mail informado esta cadastrado, mas nao e um perfil de nutricionista");
        }
        if (existing != null && cnRepository.existsActive(company.getId(), existing.getId())) {
            throw new IllegalArgumentException("Este nutricionista ja esta vinculado a empresa");
        }

        String token = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        CompanyNutritionist cn = CompanyNutritionist.builder()
                .company(company)
                .nutritionist(existing)
                .invitedByEmail(email)
                .inviteToken(token)
                .status(LinkStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();
        cn = cnRepository.save(cn);

        String link = frontendUrl + "/invite?token=" + token + "&type=company";
        String body = """
                Ola,

                A empresa %s convidou voce para fazer parte da equipe de nutricionistas dela no Nutri Tracker.

                Para aceitar ou recusar o convite, clique no link (valido por %d dias):

                %s

                Se voce ainda nao tem cadastro como nutricionista, crie um antes de aceitar.

                Nutri Tracker
                """.formatted(company.getName(), expirationDays, link);
        mailService.send(email, "Convite de empresa - Nutri Tracker", body);

        if (existing != null) {
            notificationService.notify(
                    existing.getId(),
                    NotificationType.GENERIC,
                    "Convite de empresa",
                    company.getName() + " te convidou para fazer parte do time de nutricionistas."
                            + " Abra 'Convites de empresas' para aceitar ou recusar.",
                    "CompanyNutritionist", cn.getId());
        }

        return toLinkDto(cn);
    }

    /**
     * Revoga o vinculo empresa-nutri e escolhe o que fazer com os clientes daquele nutri.
     * - REVOKE_CLIENTS: encerra os vinculos nutri-cliente (clientes ficam sem nutri).
     * - TRANSFER: transfere os vinculos ativos para o targetNutriId (deve ser outro nutri ativo da mesma empresa).
     */
    @Transactional
    public void revokeWithAction(Company company, Long linkId, String action, Long targetNutriId) {
        CompanyNutritionist cn = cnRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculo nao encontrado"));
        if (!cn.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Vinculo nao pertence a esta empresa");
        }
        User nutri = cn.getNutritionist();

        cn.setStatus(LinkStatus.REVOKED_BY_NUTRI);
        cn.setRevokedAt(LocalDateTime.now());
        cnRepository.save(cn);

        if (nutri == null) return;

        // Fase 8 fix: captura vinculos DESTA empresa ANTES de mudar a practice.
        // `endLinkWithCompany` pode converter a practice em particular (company=null)
        // quando o nutri nao tem particular; se filtrassemos depois, nenhum vinculo
        // seria encontrado. Clientes particulares do nutri nao entram nessa lista.
        var activeClientLinks = nclRepository.findByNutritionistIdOrderByInvitedAtDesc(nutri.getId()).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                .filter(l -> l.getPractice() != null
                          && l.getPractice().getCompany() != null
                          && l.getPractice().getCompany().getId().equals(company.getId()))
                .toList();

        // Fase 5E: se o nutri nao tem particular, esta practice vira particular;
        // caso contrario, apenas suspende.
        boolean converted = practiceService.endLinkWithCompany(nutri.getId(), company.getId());

        notificationService.notify(
                nutri.getId(),
                NotificationType.GENERIC,
                "Vinculo com empresa encerrado",
                converted
                    ? ("Seu vinculo com " + company.getName() + " foi encerrado. Como voce nao tinha um perfil particular, "
                       + "criamos um automaticamente com base nesta atuacao — seus clientes continuam com voce.")
                    : ("Seu vinculo com " + company.getName() + " foi encerrado."),
                "CompanyNutritionist", cn.getId());

        if ("REVOKE_CLIENTS".equalsIgnoreCase(action)) {
            for (var l : activeClientLinks) {
                l.setStatus(LinkStatus.REVOKED_BY_NUTRI);
                l.setRevokedAt(LocalDateTime.now());
                nclRepository.save(l);
                if (l.getClient() != null) {
                    // Fase 5C: se essa practice era o sponsor, cliente volta a FREE (ou transfere se tem outro nutri)
                    maybeRevokeSponsorship(l);
                    notificationService.notify(
                            l.getClient().getId(),
                            NotificationType.GENERIC,
                            "Vinculo com nutricionista encerrado",
                            "Seu vinculo com " + nutri.getName() + " foi encerrado. Voce pode buscar outro nutricionista.",
                            "NutritionistClientLink", l.getId());
                }
            }
        } else if ("TRANSFER".equalsIgnoreCase(action)) {
            if (targetNutriId == null) {
                throw new IllegalArgumentException("targetNutriId obrigatorio para transferencia");
            }
            User target = userRepository.findById(targetNutriId)
                    .orElseThrow(() -> new EntityNotFoundException("Nutricionista alvo nao encontrado"));
            if (!companyHasNutri(company.getId(), target.getId())) {
                throw new AccessDeniedException("Nutricionista alvo nao pertence a esta empresa");
            }
            var targetPractice = practiceService.ensureForCompany(target, company);
            for (var l : activeClientLinks) {
                l.setStatus(LinkStatus.REVOKED_BY_NUTRI);
                l.setRevokedAt(LocalDateTime.now());
                nclRepository.save(l);
                User client = l.getClient();
                if (client == null) continue;
                if (nclRepository.existsActiveLink(target.getId(), client.getId())) continue;
                var newLink = nclRepository.save(NutritionistClientLink.builder()
                        .nutritionist(target)
                        .practice(targetPractice)
                        .client(client)
                        .invitedByEmail(client.getEmail().toLowerCase())
                        .inviteToken(UUID.randomUUID() + UUID.randomUUID().toString().replace("-", ""))
                        .status(LinkStatus.ACTIVE)
                        .invitedAt(LocalDateTime.now())
                        .acceptedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusYears(1))
                        .initiatedByClient(false)
                        .build());
                // Fase 5C: transfere sponsorship do cliente para a practice do nutri destino
                subscriptionService.sponsorClientToPlus(client.getId(), targetPractice);
                notificationService.notify(
                        client.getId(),
                        NotificationType.GENERIC,
                        "Novo nutricionista",
                        "Voce foi transferido de " + nutri.getName() + " para " + target.getName() + ".",
                        "NutritionistClientLink", newLink.getId());
                notificationService.notify(
                        target.getId(),
                        NotificationType.GENERIC,
                        "Novo cliente atribuido",
                        client.getName() + " foi transferido para voce por " + company.getName() + ".",
                        "NutritionistClientLink", newLink.getId());
            }
        }
    }

    /** Empresa transfere um cliente especifico de nutri (dentro da mesma empresa). */
    @Transactional
    public void transferClient(Company company, Long clientId, Long targetNutriId) {
        User target = userRepository.findById(targetNutriId)
                .orElseThrow(() -> new EntityNotFoundException("Nutricionista alvo nao encontrado"));
        if (!companyHasNutri(company.getId(), target.getId())) {
            throw new AccessDeniedException("Nutricionista alvo nao pertence a esta empresa");
        }

        List<Long> companyNutris = cnRepository.activeNutritionistIds(company.getId());
        for (Long nId : companyNutris) {
            // Fase 8 fix: so revoga vinculos via practice desta empresa; preserva
            // vinculo particular do cliente com o mesmo nutri se existir.
            var links = nclRepository.findByNutritionistIdOrderByInvitedAtDesc(nId).stream()
                    .filter(l -> l.getStatus() == LinkStatus.ACTIVE
                              && l.getClient() != null
                              && l.getClient().getId().equals(clientId))
                    .filter(l -> l.getPractice() != null
                              && l.getPractice().getCompany() != null
                              && l.getPractice().getCompany().getId().equals(company.getId()))
                    .toList();
            for (var l : links) {
                l.setStatus(LinkStatus.REVOKED_BY_NUTRI);
                l.setRevokedAt(LocalDateTime.now());
                nclRepository.save(l);
            }
        }

        if (nclRepository.existsActiveLink(target.getId(), clientId)) return;
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado"));
        var targetPractice = practiceService.ensureForCompany(target, company);
        nclRepository.save(NutritionistClientLink.builder()
                .nutritionist(target)
                .practice(targetPractice)
                .client(client)
                .invitedByEmail(client.getEmail().toLowerCase())
                .inviteToken(UUID.randomUUID() + UUID.randomUUID().toString().replace("-", ""))
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now())
                .acceptedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .initiatedByClient(false)
                .build());
        subscriptionService.sponsorClientToPlus(clientId, targetPractice);
    }

    @Transactional(readOnly = true)
    public List<CompanyClientDto> listClients(Long companyId) {
        return listClients(companyId, null, null);
    }

    /** Fase 6B: q busca em nome/email/cpf do cliente; sort = 'name' | 'invitedAt' (default desc). */
    @Transactional(readOnly = true)
    public List<CompanyClientDto> listClients(Long companyId, String q, String sort) {
        String needle = q == null ? null : q.trim().toLowerCase();
        String digits = q == null ? "" : q.replaceAll("\\D", "");
        List<Long> nutriIds = cnRepository.activeNutritionistIds(companyId);
        List<CompanyClientDto> out = new java.util.ArrayList<>();
        for (Long nId : nutriIds) {
            User nutri = userRepository.findById(nId).orElse(null);
            String nutriName = nutri != null ? nutri.getName() : ("Nutri #" + nId);
            // Fase 8 fix: mostra apenas clientes vinculados via practice DESTA empresa.
            // Sem esse filtro, clientes particulares do nutri (practice sem company)
            // vazariam para a listagem da empresa.
            var links = nclRepository.findByNutritionistIdOrderByInvitedAtDesc(nId).stream()
                    .filter(l -> l.getStatus() == LinkStatus.ACTIVE && l.getClient() != null)
                    .filter(l -> l.getPractice() != null
                              && l.getPractice().getCompany() != null
                              && l.getPractice().getCompany().getId().equals(companyId))
                    .toList();
            for (var l : links) {
                User client = l.getClient();
                if (needle != null && !needle.isEmpty()) {
                    boolean match = false;
                    if (client.getName() != null && client.getName().toLowerCase().contains(needle)) match = true;
                    else if (client.getEmail() != null && client.getEmail().toLowerCase().contains(needle)) match = true;
                    else if (!digits.isEmpty() && client.getCpf() != null && client.getCpf().contains(digits)) match = true;
                    if (!match) continue;
                }
                var lastMeal = mealRepository.lastMealDate(client.getId());
                out.add(new CompanyClientDto(l.getId(), client.getId(),
                        client.getName(), client.getEmail(),
                        com.prandiumnutri.support.CpfUtil.mask(client.getCpf()),
                        nId, nutriName, l.getStatus(), lastMeal));
            }
        }
        if (!"created".equalsIgnoreCase(sort)) {
            out.sort(java.util.Comparator.comparing(x -> x.clientName() == null ? "" : x.clientName().toLowerCase()));
        }
        // default: mantem a ordem natural (por nutri + invitedAt desc do repo)
        return out;
    }

    // --- Perspectiva do Nutricionista ---

    @Transactional(readOnly = true)
    public List<NutriCompanyDto> listCompaniesForNutri(Long nutritionistId) {
        return cnRepository.findByNutritionistIdOrderByInvitedAtDesc(nutritionistId).stream()
                .filter(cn -> cn.getStatus() == LinkStatus.ACTIVE)
                .map(cn -> new NutriCompanyDto(
                        cn.getId(), cn.getCompany().getId(), cn.getCompany().getName(),
                        cn.getStatus(), cn.getAcceptedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyInviteDto> pendingCompanyInvitesFor(User nutri) {
        List<CompanyNutritionist> byEmail = cnRepository.findByInvitedByEmailAndStatus(
                nutri.getEmail().toLowerCase(), LinkStatus.PENDING);
        List<CompanyNutritionist> byUser = cnRepository.findByNutritionistIdOrderByInvitedAtDesc(nutri.getId())
                .stream().filter(cn -> cn.getStatus() == LinkStatus.PENDING).toList();
        return java.util.stream.Stream.concat(byEmail.stream(), byUser.stream())
                .distinct()
                .filter(cn -> LocalDateTime.now().isBefore(cn.getExpiresAt()))
                .map(cn -> new CompanyInviteDto(cn.getId(), cn.getInviteToken(),
                        cn.getCompany().getId(), cn.getCompany().getName(),
                        cn.getInvitedAt(), cn.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void acceptCompanyInvite(User nutri, String token) {
        if (nutri.getRole() != Role.NUTRITIONIST) {
            throw new AccessDeniedException("Somente nutricionistas podem aceitar convites de empresa");
        }
        CompanyNutritionist cn = cnRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        validateInvite(cn, nutri);
        cn.setNutritionist(nutri);
        cn.setStatus(LinkStatus.ACTIVE);
        cn.setAcceptedAt(LocalDateTime.now());
        cnRepository.save(cn);
        // Fase 5A: cria/reativa a practice do nutri para esta empresa
        practiceService.ensureForCompany(nutri, cn.getCompany());
    }

    @Transactional
    public void rejectCompanyInvite(User nutri, String token) {
        CompanyNutritionist cn = cnRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        validateInvite(cn, nutri);
        cn.setStatus(LinkStatus.REJECTED);
        cn.setRevokedAt(LocalDateTime.now());
        cnRepository.save(cn);
    }

    @Transactional
    public void leaveCompany(User nutri, Long linkId) {
        CompanyNutritionist cn = cnRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculo nao encontrado"));
        if (cn.getNutritionist() == null || !cn.getNutritionist().getId().equals(nutri.getId())) {
            throw new AccessDeniedException("Voce nao pode revogar este vinculo");
        }
        cn.setStatus(LinkStatus.REVOKED_BY_NUTRI);
        cn.setRevokedAt(LocalDateTime.now());
        cnRepository.save(cn);
        // Fase 5E: se nutri nao tem particular, essa practice vira particular; senao, suspende.
        practiceService.endLinkWithCompany(nutri.getId(), cn.getCompany().getId());
    }

    // --- Overview do Company ---

    @Transactional(readOnly = true)
    public CompanyOverviewDto overview(Long companyId) {
        List<CompanyNutritionist> all = cnRepository.findByCompanyIdOrderByInvitedAtDesc(companyId);
        long active = all.stream().filter(c -> c.getStatus() == LinkStatus.ACTIVE).count();
        long pending = all.stream().filter(c -> c.getStatus() == LinkStatus.PENDING).count();
        // Fase 8 fix: conta apenas clientes vinculados via practice DESTA empresa
        long clients = all.stream()
                .filter(c -> c.getStatus() == LinkStatus.ACTIVE && c.getNutritionist() != null)
                .mapToLong(c -> nclRepository.countActiveOrPendingForCompany(
                        c.getNutritionist().getId(), companyId))
                .sum();
        return new CompanyOverviewDto(active, pending, clients);
    }

    // --- Helper para CurrentUser (impersonation) ---

    public boolean companyHasNutri(Long companyId, Long nutriId) {
        return cnRepository.existsActive(companyId, nutriId);
    }

    public boolean companyHasClientViaNutri(Long companyId, Long clientId) {
        // Fase 8 fix: so retorna true se o cliente esta vinculado ao nutri VIA
        // uma practice desta empresa. Clientes da practice particular do nutri
        // nao devem ser visiveis/impersonavel pela empresa.
        List<Long> nutriIds = cnRepository.activeNutritionistIds(companyId);
        for (Long nId : nutriIds) {
            boolean linkedViaCompany = nclRepository.findByNutritionistIdOrderByInvitedAtDesc(nId).stream()
                    .anyMatch(l -> l.getStatus() == LinkStatus.ACTIVE
                              && l.getClient() != null && l.getClient().getId().equals(clientId)
                              && l.getPractice() != null && l.getPractice().getCompany() != null
                              && l.getPractice().getCompany().getId().equals(companyId));
            if (linkedViaCompany) return true;
        }
        return false;
    }

    // --- privados ---

    private void validateInvite(CompanyNutritionist cn, User nutri) {
        if (cn.getStatus() != LinkStatus.PENDING) {
            throw new IllegalArgumentException("Convite nao esta mais pendente");
        }
        if (LocalDateTime.now().isAfter(cn.getExpiresAt())) {
            throw new IllegalArgumentException("Convite expirado");
        }
        boolean matchesEmail = cn.getInvitedByEmail().equalsIgnoreCase(nutri.getEmail());
        boolean matchesUser = cn.getNutritionist() != null
                && cn.getNutritionist().getId().equals(nutri.getId());
        if (!matchesEmail && !matchesUser) {
            throw new AccessDeniedException("Este convite nao e seu");
        }
    }

    private CompanyNutriLinkDto toLinkDto(CompanyNutritionist cn) {
        User n = cn.getNutritionist();
        long clients = (n != null) ? nclRepository.countActiveOrPending(n.getId()) : 0;
        return new CompanyNutriLinkDto(
                cn.getId(),
                n != null ? n.getId() : null,
                n != null ? n.getName() : "(pendente)",
                n != null ? n.getEmail() : cn.getInvitedByEmail(),
                cn.getStatus(), cn.getInvitedAt(), cn.getAcceptedAt(), clients);
    }

    /** Fase 5C: revoga sponsorship apos encerrar link nutri-cliente por acao da empresa. */
    private void maybeRevokeSponsorship(NutritionistClientLink link) {
        if (link.getClient() == null || link.getPractice() == null) return;
        Long clientId = link.getClient().getId();
        Long practiceId = link.getPractice().getId();
        subscriptionService.revokeSponsorship(clientId, practiceId, () ->
            nclRepository.findByClientIdOrderByInvitedAtDesc(clientId).stream()
                    .filter(l -> l.getStatus() == LinkStatus.ACTIVE
                              && !l.getId().equals(link.getId())
                              && l.getPractice() != null)
                    .map(NutritionistClientLink::getPractice)
                    .findFirst()
        );
    }
}
