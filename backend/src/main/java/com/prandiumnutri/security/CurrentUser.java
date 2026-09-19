package com.prandiumnutri.security;

import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.CompanyMemberRepository;
import com.prandiumnutri.repository.CompanyNutritionistRepository;
import com.prandiumnutri.repository.CompanyRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    public static final String IMPERSONATE_HEADER = "X-Impersonate-User-Id";

    private final UserRepository userRepository;
    private final NutritionistClientLinkRepository linkRepository;
    private final CompanyRepository companyRepository;
    private final CompanyNutritionistRepository companyNutriRepository;
    private final CompanyMemberRepository companyMemberRepository;

    public User get() {
        User authenticated = getAuthenticated();
        String header = impersonationHeader();
        if (header == null) return authenticated;

        Long targetId;
        try { targetId = Long.parseLong(header); }
        catch (NumberFormatException e) { return authenticated; }

        return switch (authenticated.getRole()) {
            case ADMIN -> loadOrThrow(targetId);
            case NUTRITIONIST -> {
                if (!linkRepository.existsActiveLink(authenticated.getId(), targetId)) {
                    throw new AccessDeniedException("Sem vinculo ativo com este cliente");
                }
                yield loadOrThrow(targetId);
            }
            case COMPANY -> {
                Long companyId = resolveCompanyId(authenticated.getId());
                if (companyNutriRepository.existsActive(companyId, targetId)) {
                    yield loadOrThrow(targetId);
                }
                List<Long> nutriIds = companyNutriRepository.activeNutritionistIds(companyId);
                for (Long nId : nutriIds) {
                    if (linkRepository.existsActiveLink(nId, targetId)) {
                        yield loadOrThrow(targetId);
                    }
                }
                throw new AccessDeniedException("Este alvo nao esta sob sua empresa");
            }
            default -> authenticated;
        };
    }

    public Long id() { return get().getId(); }

    public User getAuthenticated() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AppUserDetails details) return details.getUser();
        throw new AccessDeniedException("Nao autenticado");
    }

    public Long getAuthenticatedId() { return getAuthenticated().getId(); }

    private User loadOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Impersonated user not found: " + id));
    }

    /** Resolve companyId a partir do userId: owner direto OU membro ACTIVE. */
    private Long resolveCompanyId(Long userId) {
        var byOwner = companyRepository.findByOwnerId(userId);
        if (byOwner.isPresent()) return byOwner.get().getId();
        var members = companyMemberRepository.findByUserIdAndStatus(userId, LinkStatus.ACTIVE);
        if (!members.isEmpty()) return members.get(0).getCompany().getId();
        throw new AccessDeniedException("Empresa nao encontrada para este usuario");
    }

    private String impersonationHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        String h = attrs.getRequest().getHeader(IMPERSONATE_HEADER);
        return (h == null || h.isBlank()) ? null : h;
    }
}
