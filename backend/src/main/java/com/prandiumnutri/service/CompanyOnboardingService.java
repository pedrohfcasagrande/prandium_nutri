package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.*;
import com.prandiumnutri.repository.*;
import com.prandiumnutri.security.JwtService;
import com.prandiumnutri.security.RefreshTokenService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyOnboardingService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository memberRepository;
    private final CompanyNutritionistRepository cnRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${prandiumnutri.invite.expiration-days:14}")
    private int expirationDays;

    @Value("${prandiumnutri.reset.frontend-url:http://localhost:4201}")
    private String frontendUrl;

    // ===================== ADMIN =====================

    @Transactional(readOnly = true)
    public List<AdminCompanyDto> listAll() {
        return listAll(null, null);
    }

    /** Fase 6B: q busca em name/tradeName/taxId/contactEmail; sort = 'name' | 'created' (default). */
    @Transactional(readOnly = true)
    public List<AdminCompanyDto> listAll(String q, String sort) {
        var stream = companyRepository.findAll().stream();
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase();
            String digits = q.replaceAll("\\D", "");
            stream = stream.filter(c ->
                (c.getName() != null && c.getName().toLowerCase().contains(needle))
             || (c.getTradeName() != null && c.getTradeName().toLowerCase().contains(needle))
             || (c.getContactEmail() != null && c.getContactEmail().toLowerCase().contains(needle))
             || (!digits.isEmpty() && c.getTaxId() != null && c.getTaxId().replaceAll("\\D", "").contains(digits))
            );
        }
        java.util.Comparator<Company> comp = !"created".equalsIgnoreCase(sort)
                ? java.util.Comparator.comparing(c -> c.getName() == null ? "" : c.getName().toLowerCase())
                : java.util.Comparator.comparing(
                        (Company c) -> c.getCreatedAt() == null ? java.time.LocalDateTime.MIN : c.getCreatedAt()
                  ).reversed();
        return stream.sorted(comp).map(this::toAdminDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminCompanyDto getById(Long id) {
        Company c = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa nao encontrada"));
        return toAdminDto(c);
    }

    @Transactional
    public AdminCompanyDto createByAdmin(User admin, CreateCompanyRequest req) {
        String ownerEmail = req.ownerEmail().trim().toLowerCase();

        Company company = Company.builder()
                .name(req.name())
                .tradeName(req.tradeName())
                .taxId(req.taxId())
                .countryCode(req.countryCode().toUpperCase())
                .contactEmail(req.contactEmail().trim().toLowerCase())
                .contactPhone(req.contactPhone())
                .active(true)
                .createdByAdmin(admin)
                .build();
        company = companyRepository.save(company);

        // Cria convite para OWNER
        createInvite(company, ownerEmail, CompanyMemberRole.OWNER);

        return toAdminDto(company);
    }

    @Transactional
    public AdminCompanyDto setActive(Long id, boolean active) {
        Company c = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa nao encontrada"));
        c.setActive(active);
        return toAdminDto(companyRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        companyRepository.deleteById(id);
    }

    /** Reenvia convite para o owner original (se ainda pendente). */
    @Transactional
    public void resendOwnerInvite(Long companyId) {
        Company c = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Empresa nao encontrada"));
        CompanyMember pendingOwner = memberRepository.findByCompanyIdOrderByInvitedAtDesc(companyId)
                .stream()
                .filter(m -> m.getRoleInCompany() == CompanyMemberRole.OWNER
                          && m.getStatus() == LinkStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nao ha convite de owner pendente"));

        pendingOwner.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        memberRepository.save(pendingOwner);
        sendInviteEmail(c, pendingOwner);
    }

    // ===================== COMPANY (owner/admin convida outros admins) =====================

    @Transactional
    public CompanyMemberDto inviteMember(Company company, InviteCompanyMemberRequest req) {
        String email = req.email().trim().toLowerCase();

        CompanyMember cm = createInvite(company, email, req.role());
        return toMemberDto(cm);
    }

    @Transactional(readOnly = true)
    public List<CompanyMemberDto> listMembers(Long companyId) {
        return memberRepository.findByCompanyIdOrderByInvitedAtDesc(companyId).stream()
                .map(this::toMemberDto).toList();
    }

    @Transactional
    public void revokeMember(Company company, Long memberId) {
        CompanyMember cm = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Membro nao encontrado"));
        if (!cm.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Membro nao pertence a esta empresa");
        }
        if (cm.getRoleInCompany() == CompanyMemberRole.OWNER
                && cm.getStatus() == LinkStatus.ACTIVE) {
            // Nao permite revogar o proprio owner
            throw new IllegalArgumentException("Nao e possivel revogar o owner. Transfira o owner primeiro.");
        }
        cm.setStatus(LinkStatus.REVOKED_BY_COMPANY);
        cm.setRevokedAt(LocalDateTime.now());
        memberRepository.save(cm);
    }

    // ===================== ONBOARDING (aceite via link) =====================

    @Transactional(readOnly = true)
    public OnboardingInfoDto lookupToken(String token) {
        CompanyMember cm = memberRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Convite invalido"));
        if (cm.getStatus() != LinkStatus.PENDING) {
            throw new IllegalArgumentException("Convite ja utilizado ou revogado");
        }
        if (LocalDateTime.now().isAfter(cm.getExpiresAt())) {
            throw new IllegalArgumentException("Convite expirado");
        }
        boolean userExists = userRepository.findByEmail(cm.getInvitedByEmail()).isPresent();
        return new OnboardingInfoDto(
                cm.getCompany().getId(),
                cm.getCompany().getName(),
                cm.getInvitedByEmail(),
                cm.getRoleInCompany(),
                userExists
        );
    }

    /**
     * Aceita o convite:
     * - Se ja existe user com esse email: promove pra COMPANY e vincula.
     * - Se nao existe: cria user novo com nome e senha informados.
     * Retorna JWT + info do user.
     */
    @Transactional
    public AuthResponse acceptOnboarding(String token, AcceptOnboardingRequest req, HttpServletRequest http) {
        CompanyMember cm = memberRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Convite invalido"));
        if (cm.getStatus() != LinkStatus.PENDING) {
            throw new IllegalArgumentException("Convite ja utilizado ou revogado");
        }
        if (LocalDateTime.now().isAfter(cm.getExpiresAt())) {
            throw new IllegalArgumentException("Convite expirado");
        }

        String email = cm.getInvitedByEmail();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .name(req.name())
                    .email(email)
                    .passwordHash(passwordEncoder.encode(req.password()))
                    .role(Role.COMPANY)
                    .provider(AuthProvider.LOCAL)
                    .profileCompleted(true)
                    .active(true)
                    .consentAcceptedAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
        } else {
            // Promove pra COMPANY se ainda nao for
            if (user.getRole() != Role.COMPANY && user.getRole() != Role.ADMIN) {
                user.setRole(Role.COMPANY);
                userRepository.save(user);
            }
        }

        cm.setUser(user);
        cm.setStatus(LinkStatus.ACTIVE);
        cm.setAcceptedAt(LocalDateTime.now());
        memberRepository.save(cm);

        // Se e o OWNER e a empresa ainda nao tem owner, seta
        if (cm.getRoleInCompany() == CompanyMemberRole.OWNER
                && cm.getCompany().getOwner() == null) {
            Company c = cm.getCompany();
            c.setOwner(user);
            companyRepository.save(c);
        }

        RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(
                user,
                http != null ? http.getHeader("User-Agent") : null,
                http != null ? http.getRemoteAddr() : null);
        return new AuthResponse(
                jwtService.generate(user),
                refresh.raw(),
                jwtService.getExpirationSeconds(),
                user.getId(), user.getEmail(), user.getName(),
                user.getRole(), user.isProfileCompleted()
        );
    }

    // ===================== helpers =====================

    private CompanyMember createInvite(Company company, String email, CompanyMemberRole role) {
        User existing = userRepository.findByEmail(email).orElse(null);

        if (existing != null) {
            // Ja existe user com este email: verifica se ja e membro
            boolean already = memberRepository.findByCompanyIdOrderByInvitedAtDesc(company.getId())
                    .stream()
                    .anyMatch(m -> m.getUser() != null
                            && m.getUser().getId().equals(existing.getId())
                            && (m.getStatus() == LinkStatus.ACTIVE || m.getStatus() == LinkStatus.PENDING));
            if (already) {
                throw new IllegalArgumentException("Este usuario ja e membro da empresa ou tem convite pendente");
            }
        }

        String token = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        CompanyMember cm = CompanyMember.builder()
                .company(company)
                .user(existing)
                .invitedByEmail(email)
                .inviteToken(token)
                .roleInCompany(role)
                .status(LinkStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();
        cm = memberRepository.save(cm);
        sendInviteEmail(company, cm);
        return cm;
    }

    private void sendInviteEmail(Company company, CompanyMember cm) {
        String link = frontendUrl + "/company-onboarding?token=" + cm.getInviteToken();
        String roleName = cm.getRoleInCompany() == CompanyMemberRole.OWNER ? "administrador principal" : "administrador";
        String body = """
                Ola,

                Voce foi convidado para ser %s da empresa "%s" no Nutri Tracker.

                Para aceitar o convite e ativar seu acesso, clique no link (valido por %d dias):

                %s

                Se voce ainda nao tem conta, o proprio link te guiara para criar uma.

                Nutri Tracker
                """.formatted(roleName, company.getName(), expirationDays, link);
        mailService.send(cm.getInvitedByEmail(),
                "Convite: " + company.getName() + " - Nutri Tracker", body);
    }

    private AdminCompanyDto toAdminDto(Company c) {
        List<CompanyMember> members = memberRepository.findByCompanyIdOrderByInvitedAtDesc(c.getId());
        long active = members.stream().filter(m -> m.getStatus() == LinkStatus.ACTIVE).count();
        long pending = members.stream().filter(m -> m.getStatus() == LinkStatus.PENDING).count();
        long nutris = cnRepository.findByCompanyIdOrderByInvitedAtDesc(c.getId()).stream()
                .filter(cn -> cn.getStatus() == LinkStatus.ACTIVE).count();

        User owner = c.getOwner();
        return new AdminCompanyDto(c.getId(), c.getName(), c.getTradeName(), c.getTaxId(),
                c.getCountryCode(), c.getContactEmail(), c.getContactPhone(),
                c.isActive(), c.getCreatedAt(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getName() : null,
                owner != null ? owner.getEmail() : null,
                active, pending, nutris);
    }

    private CompanyMemberDto toMemberDto(CompanyMember m) {
        User u = m.getUser();
        return new CompanyMemberDto(m.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getName() : "(pendente)",
                u != null ? u.getEmail() : m.getInvitedByEmail(),
                m.getRoleInCompany(), m.getStatus(),
                m.getInvitedAt(), m.getAcceptedAt());
    }
}
