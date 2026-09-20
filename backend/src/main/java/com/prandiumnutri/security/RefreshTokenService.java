package com.prandiumnutri.security;

import com.prandiumnutri.domain.RefreshToken;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.RefreshTokenRepository;
import com.prandiumnutri.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 48;
    private static final SecureRandom RNG = new SecureRandom();

    private final RefreshTokenRepository refreshRepo;
    private final UserRepository userRepo;

    @Value("${prandiumnutri.jwt.refresh-expiration-ms}")
    private long refreshTtlMs;

    public record IssuedToken(String raw, RefreshToken persisted) {}

    @Transactional
    public IssuedToken issue(User user, String userAgent, String ip) {
        String raw = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(raw))
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusNanos(refreshTtlMs * 1_000_000))
                .userAgent(truncate(userAgent, 255))
                .ip(truncate(ip, 64))
                .build();
        entity = refreshRepo.save(entity);
        return new IssuedToken(raw, entity);
    }

    /**
     * Troca o refresh apresentado por um novo par (rotação). Detecta reuse:
     * se o token já foi revogado, invalida toda a família (revoga todos os
     * refreshes ativos do usuário) — sinal de que alguém copiou o token.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public RefreshResult rotate(String presentedRaw, String userAgent, String ip) {
        RefreshToken current = refreshRepo.findByTokenHash(hash(presentedRaw))
                .orElseThrow(() -> new BadCredentialsException("refresh token invalido"));

        if (current.getRevokedAt() != null) {
            // reuse detectado — revoga tudo do usuario
            refreshRepo.revokeAllForUser(current.getUserId(), LocalDateTime.now());
            throw new BadCredentialsException("refresh token reutilizado — sessao invalidada");
        }
        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("refresh token expirado");
        }

        User user = userRepo.findById(current.getUserId())
                .orElseThrow(() -> new BadCredentialsException("usuario nao encontrado"));
        if (!user.isActive()) {
            throw new BadCredentialsException("usuario inativo");
        }

        IssuedToken next = issue(user, userAgent, ip);
        current.setRevokedAt(LocalDateTime.now());
        current.setReplacedBy(next.persisted().getId());
        refreshRepo.save(current);

        return new RefreshResult(user, next.raw());
    }

    @Transactional
    public void revoke(String presentedRaw) {
        refreshRepo.findByTokenHash(hash(presentedRaw)).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.setRevokedAt(LocalDateTime.now());
                refreshRepo.save(t);
            }
        });
    }

    public record RefreshResult(User user, String rawRefreshToken) {}

    // ===================== helpers =====================

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
