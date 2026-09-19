package com.prandiumnutri.seed;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seed apenas do usuario admin. Alimentos sao populados via Flyway (V002__seed_foods.sql).
 * O admin precisa ser criado em Java porque a senha precisa ser codificada em BCrypt em runtime.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${prandiumnutri.admin.email}")
    private String adminEmail;

    @Value("${prandiumnutri.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                u -> log.info("Admin ja existe: {}", adminEmail),
                () -> {
                    User admin = User.builder()
                            .email(adminEmail)
                            .name("Administrador")
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .role(Role.ADMIN)
                            .provider(AuthProvider.LOCAL)
                            .profileCompleted(true)
                            .active(true)
                            .consentAcceptedAt(LocalDateTime.now())
                            .build();
                    userRepository.save(admin);
                    log.info("Admin criado: {}", adminEmail);
                }
        );
    }
}
