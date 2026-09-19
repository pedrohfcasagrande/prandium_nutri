package com.nutritracker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.nutritracker.domain.AuditAction;
import com.nutritracker.domain.AuthProvider;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Role;
import com.nutritracker.domain.User;
import com.nutritracker.dto.*;
import com.nutritracker.repository.UserRepository;
import com.nutritracker.security.GoogleTokenVerifier;
import com.nutritracker.security.JwtService;
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

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("E-mail ja cadastrado");
        }
        Role role = req.role() == Role.NUTRITIONIST ? Role.NUTRITIONIST : Role.USER;
        // Nutricionista nao precisa preencher profile de saude para usar
        boolean profileCompleted = role == Role.NUTRITIONIST;
        User user = User.builder()
                .name(req.name())
                .email(req.email())
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
        auditService.record(AuditAction.REGISTER, user.getId(), user.getRole());
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException(GENERIC_ERROR));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(GENERIC_ERROR);
        }
        if (!user.isActive()) {
            throw new BadCredentialsException(GENERIC_ERROR);
        }
        auditService.record(AuditAction.LOGIN, user.getId(), user.getRole());
        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest req) {
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
        return toAuthResponse(user);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest req) {
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Senha atual invalida");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generate(user),
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isProfileCompleted()
        );
    }
}
