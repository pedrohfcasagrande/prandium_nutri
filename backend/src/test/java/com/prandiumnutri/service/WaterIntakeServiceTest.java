package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuthProvider;
import com.prandiumnutri.domain.Role;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.WaterIntakeDto;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import com.prandiumnutri.repository.UserRepository;
import com.prandiumnutri.support.PlanTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WaterIntakeServiceTest {

    @Autowired WaterIntakeService service;
    @Autowired UserRepository userRepo;
    @Autowired PlanRepository planRepo;
    @Autowired PlanPriceRepository priceRepo;

    @BeforeEach
    void seedPlans() {
        if (planRepo.count() == 0) PlanTestFixtures.seedAll(planRepo, priceRepo);
    }

    private User user() {
        return userRepo.save(User.builder()
                .name("Drinker").email("drink-" + System.nanoTime() + "@x.com")
                .role(Role.USER).provider(AuthProvider.LOCAL)
                .profileCompleted(true).active(true).consentAcceptedAt(LocalDateTime.now())
                .passwordHash("$2a$10$x")
                .cpf(String.format("%011d", Math.abs(System.nanoTime() % 100000000000L)))
                .build());
    }

    @Test
    void log_persistsAndSumsForDay() {
        User u = user();
        service.log(u.getId(), 200);
        service.log(u.getId(), 300);
        assertThat(service.totalMl(u.getId(), LocalDate.now())).isEqualTo(500);
        List<WaterIntakeDto> list = service.listByDate(u.getId(), LocalDate.now());
        assertThat(list).hasSize(2).allSatisfy(d -> assertThat(d.amountMl()).isPositive());
    }

    @Test
    void log_rejectsInvalidAmount() {
        User u = user();
        assertThatThrownBy(() -> service.log(u.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.log(u.getId(), 5000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_removesOwnRow() {
        User u = user();
        WaterIntakeDto saved = service.log(u.getId(), 250);
        service.delete(u.getId(), saved.id());
        assertThat(service.totalMl(u.getId(), LocalDate.now())).isZero();
    }

    @Test
    void delete_blocksOtherUser() {
        User owner = user();
        User attacker = user();
        WaterIntakeDto saved = service.log(owner.getId(), 250);
        assertThatThrownBy(() -> service.delete(attacker.getId(), saved.id()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void totalMl_zeroWhenNothingLogged() {
        User u = user();
        assertThat(service.totalMl(u.getId(), LocalDate.now())).isZero();
    }
}
