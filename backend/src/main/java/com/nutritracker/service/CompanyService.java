package com.nutritracker.service;

import com.nutritracker.domain.*;
import com.nutritracker.dto.*;
import com.nutritracker.repository.*;
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

    @Value("${nutritracker.invite.expiration-days:14}")
    private int expirationDays;

    @Value("${nutritracker.reset.frontend-url:http://localhost:4200}")
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
        return cnRepository.findByCompanyIdOrderByInvitedAtDesc(companyId).stream().map(cn -> {
            User n = cn.getNutritionist();
            long clients = (n != null) ? nclRepository.countActiveOrPending(n.getId()) : 0;
            return new CompanyNutriLinkDto(
                    cn.getId(),
                    n != null ? n.getId() : null,
                    n != null ? n.getName() : "(pendente)",
                    n != null ? n.getEmail() : cn.getInvitedByEmail(),
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

        return toLinkDto(cn);
    }

    @Transactional
    public void revokeByCompany(Company company, Long linkId) {
        CompanyNutritionist cn = cnRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculo nao encontrado"));
        if (!cn.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Vinculo nao pertence a esta empresa");
        }
        cn.setStatus(LinkStatus.REVOKED_BY_NUTRI); // nao ha status especifico REVOKED_BY_COMPANY; reusa
        cn.setRevokedAt(LocalDateTime.now());
        cnRepository.save(cn);
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

        var activeClientLinks = nclRepository.findByNutritionistIdOrderByInvitedAtDesc(nutri.getId()).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                .toList();

        if ("REVOKE_CLIENTS".equalsIgnoreCase(action)) {
            for (var l : activeClientLinks) {
                l.setStatus(LinkStatus.REVOKED_BY_NUTRI);
                l.setRevokedAt(LocalDateTime.now());
                nclRepository.save(l);
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
            for (var l : activeClientLinks) {
                l.setStatus(LinkStatus.REVOKED_BY_NUTRI);
                l.setRevokedAt(LocalDateTime.now());
                nclRepository.save(l);
                User client = l.getClient();
                if (client == null) continue;
                if (nclRepository.existsActiveLink(target.getId(), client.getId())) continue;
                nclRepository.save(NutritionistClientLink.builder()
                        .nutritionist(target)
                        .client(client)
                        .invitedByEmail(client.getEmail().toLowerCase())
                        .inviteToken(UUID.randomUUID() + UUID.randomUUID().toString().replace("-", ""))
                        .status(LinkStatus.ACTIVE)
                        .invitedAt(LocalDateTime.now())
                        .acceptedAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusYears(1))
                        .initiatedByClient(false)
                        .build());
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
            var links = nclRepository.findByNutritionistIdOrderByInvitedAtDesc(nId).stream()
                    .filter(l -> l.getStatus() == LinkStatus.ACTIVE
                              && l.getClient() != null
                              && l.getClient().getId().equals(clientId))
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
        nclRepository.save(NutritionistClientLink.builder()
                .nutritionist(target)
                .client(client)
                .invitedByEmail(client.getEmail().toLowerCase())
                .inviteToken(UUID.randomUUID() + UUID.randomUUID().toString().replace("-", ""))
                .status(LinkStatus.ACTIVE)
                .invitedAt(LocalDateTime.now())
                .acceptedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .initiatedByClient(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<CompanyClientDto> listClients(Long companyId) {
        List<Long> nutriIds = cnRepository.activeNutritionistIds(companyId);
        List<CompanyClientDto> out = new java.util.ArrayList<>();
        for (Long nId : nutriIds) {
            User nutri = userRepository.findById(nId).orElse(null);
            String nutriName = nutri != null ? nutri.getName() : ("Nutri #" + nId);
            var links = nclRepository.findByNutritionistIdOrderByInvitedAtDesc(nId).stream()
                    .filter(l -> l.getStatus() == LinkStatus.ACTIVE && l.getClient() != null)
                    .toList();
            for (var l : links) {
                User client = l.getClient();
                var lastMeal = mealRepository.lastMealDate(client.getId());
                out.add(new CompanyClientDto(l.getId(), client.getId(),
                        client.getName(), client.getEmail(),
                        nId, nutriName, l.getStatus(), lastMeal));
            }
        }
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
    }

    // --- Overview do Company ---

    @Transactional(readOnly = true)
    public CompanyOverviewDto overview(Long companyId) {
        List<CompanyNutritionist> all = cnRepository.findByCompanyIdOrderByInvitedAtDesc(companyId);
        long active = all.stream().filter(c -> c.getStatus() == LinkStatus.ACTIVE).count();
        long pending = all.stream().filter(c -> c.getStatus() == LinkStatus.PENDING).count();
        long clients = all.stream()
                .filter(c -> c.getStatus() == LinkStatus.ACTIVE && c.getNutritionist() != null)
                .mapToLong(c -> nclRepository.countActiveOrPending(c.getNutritionist().getId()))
                .sum();
        return new CompanyOverviewDto(active, pending, clients);
    }

    // --- Helper para CurrentUser (impersonation) ---

    public boolean companyHasNutri(Long companyId, Long nutriId) {
        return cnRepository.existsActive(companyId, nutriId);
    }

    public boolean companyHasClientViaNutri(Long companyId, Long clientId) {
        List<Long> nutriIds = cnRepository.activeNutritionistIds(companyId);
        for (Long nId : nutriIds) {
            if (nclRepository.existsActiveLink(nId, clientId)) return true;
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
}
