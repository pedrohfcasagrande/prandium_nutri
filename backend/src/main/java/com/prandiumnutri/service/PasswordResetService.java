package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.PasswordResetToken;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.PasswordResetTokenRepository;
import com.prandiumnutri.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${prandiumnutri.reset.expiration-hours:1}")
    private int expirationHours;

    @Value("${prandiumnutri.reset.frontend-url:http://localhost:4201}")
    private String frontendUrl;

    /**
     * Sempre retorna sem erro para nao vazar se o email existe.
     * So envia email se o usuario existe E foi criado com senha local (nao SSO).
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Reset solicitado para email inexistente: {}", email);
            return;
        }
        User user = userOpt.get();
        if (user.getProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            log.info("Reset solicitado para usuario SSO (Google) - nao ha senha local: {}", email);
            return;
        }

        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(rawToken)
                .expiresAt(LocalDateTime.now().plusHours(expirationHours))
                .createdAt(LocalDateTime.now())
                .build();
        tokenRepository.save(token);

        String link = frontendUrl + "/reset-password?token=" + rawToken;
        String body = """
                Ola %s,

                Recebemos uma solicitacao para redefinir sua senha no Nutri Tracker.

                Para criar uma nova senha, clique no link abaixo (valido por %d hora(s)):

                %s

                Se voce nao fez essa solicitacao, ignore este email - sua senha permanecera a mesma.

                Nutri Tracker
                """.formatted(user.getName(), expirationHours, link);

        mailService.send(user.getEmail(), "Recuperacao de senha - Nutri Tracker", body);
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Token invalido"));
        if (!token.isValid()) {
            throw new IllegalArgumentException("Token expirado ou ja utilizado");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve ter ao menos 6 caracteres");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("Senha alterada para usuario {}", user.getEmail());
    }
}
