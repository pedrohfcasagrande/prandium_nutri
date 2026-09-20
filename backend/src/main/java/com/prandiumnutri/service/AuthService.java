package com.prandiumnutri.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.prandiumnutri.domain.AuditAction;
import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.*;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.security.GoogleTokenVerifier;
import com.prandiumnutri.security.JwtService;
import com.prandiumnutri.security.RefreshTokenService;
import com.prandiumnutri.security.RefreshTokenService.RefreshResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GENERIC_ERROR = "Usuario ou senha invalidos";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final SubscriptionService subscriptionService;
    private final AuditService auditService;
    private final NutritionistPracticeService practiceService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest req, HttpServletRequest http) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("E-mail ja cadastrado");
        }
        String cpf = com.prandiumnutri.support.CpfUtil.normalize(req.cpf());
        if (cpf == null || cpf.length() != 11) {
            throw new IllegalArgumentException("CPF invalido");
        }
        if (userRepository.findByCpf(cpf).isPresent()) {
            throw new IllegalArgumentException("CPF ja cadastrado");
        }
        Role role = req.role() == Role.NUTRITIONIST ? Role.NUTRITIONIST : Role.USER;
        // Nutricionista nao precisa preencher profile de saude para usar
        boolean profileCompleted = role == Role.NUTRITIONIST;
        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .cpf(cpf)
                .crn(role == Role.NUTRITIONIST && req.crn() != null && !req.crn().isBlank() ? req.crn().trim() : null)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .provider(AuthProvider.LOCAL)
                .profileCompleted(profileCompleted)
                .active(true)
                .consentAcceptedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        // Cria FREE subscription automatica
        PlanTarget target = role == Role.NUTRITIONIST ? PlanTarget.NUTRITIONIST : PlanTarget.USER;
        subscriptionService.createFreeSubscription(target, user.getId(), "BRL");
        // Nutricionista sempre nasce com 1 perfil particular (Fase 5A)
        if (role == Role.NUTRITIONIST) {
            practiceService.ensureParticular(user);
        }
        auditService.record(AuditAction.REGISTER, user.getId(), user.getRole());
        return toAuthResponse(user, http);
    }

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest http) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException(GENERIC_ERROR));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(GENERIC_ERROR);
        }
        if (!user.isActive()) {
            throw new BadCredentialsException(GENERIC_ERROR);
        }
        auditService.record(AuditAction.LOGIN, user.getId(), user.getRole());
        return toAuthResponse(user, http);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest req, HttpServletRequest http) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(req.idToken());
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String sub = payload.getSubject();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            if (req.dataConsent() == null || !req.dataConsent()) {
                throw new IllegalArgumentException("Voce precisa aceitar a politica de coleta de dados no primeiro acesso");
            }
            user = User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .role(Role.USER)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(sub)
                    .profileCompleted(false)
                    .active(true)
                    .consentAcceptedAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
        } else if (user.getProvider() == AuthProvider.LOCAL && user.getProviderId() == null) {
            user.setProviderId(sub);
            user = userRepository.save(user);
        }
        auditService.record(AuditAction.LOGIN, user.getId(), user.getRole());
        return toAuthResponse(user, http);
    }

    public AuthResponse refresh(String presentedRefreshToken, HttpServletRequest http) {
        RefreshResult r = refreshTokenService.rotate(
                presentedRefreshToken,
                http != null ? http.getHeader("User-Agent") : null,
                http != null ? http.getRemoteAddr() : null);
        return new AuthResponse(
                jwtService.generate(r.user()),
                r.rawRefreshToken(),
                jwtService.getExpirationSeconds(),
                r.user().getId(),
                r.user().getEmail(),
                r.user().getName(),
                r.user().getRole(),
                r.user().isProfileCompleted()
        );
    }

    @Transactional
    public void logout(String presentedRefreshToken) {
        if (presentedRefreshToken != null && !presentedRefreshToken.isBlank()) {
            refreshTokenService.revoke(presentedRefreshToken);
        }
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest req) {
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Senha atual invalida");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user, HttpServletRequest http) {
        RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(
                user,
                http != null ? http.getHeader("User-Agent") : null,
                http != null ? http.getRemoteAddr() : null);
        return new AuthResponse(
                jwtService.generate(user),
                refresh.raw(),
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isProfileCompleted()
        );
    }
}
