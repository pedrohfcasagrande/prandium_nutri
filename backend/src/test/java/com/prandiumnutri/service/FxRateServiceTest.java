package com.prandiumnutri.service;

import com.prandiumnutri.domain.DailyFxRate;
import com.prandiumnutri.repository.DailyFxRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FxRateServiceTest {

    @Autowired FxRateService fxRateService;
    @Autowired DailyFxRateRepository repository;

    @Test
    void fallback_whenTableEmpty_returnsStaticRate() {
        // ddl create-drop no test profile deixa tabela vazia sem Flyway
        assertThat(fxRateService.toBrl("USD")).isEqualTo(5.5);
        assertThat(fxRateService.toBrl("EUR")).isEqualTo(6.0);
        assertThat(fxRateService.toBrl("BRL")).isEqualTo(1.0);
    }

    @Test
    void toBrl_usesMostRecentRateFromDb() {
        repository.save(DailyFxRate.builder()
                .currency("USD").rateToBrl(new BigDecimal("5.30")).rateDate(LocalDate.now().minusDays(1))
                .source("TEST").build());
        repository.save(DailyFxRate.builder()
                .currency("USD").rateToBrl(new BigDecimal("6.10")).rateDate(LocalDate.now())
                .source("TEST").build());

        assertThat(fxRateService.toBrl("USD")).isEqualTo(6.10);
    }

    @Test
    void toBrlCents_convertsCentsCorrectly() {
        repository.save(DailyFxRate.builder()
                .currency("EUR").rateToBrl(new BigDecimal("6.00")).rateDate(LocalDate.now())
                .source("TEST").build());
        // 1000 cents EUR * 6 = 6000 cents BRL
        assertThat(fxRateService.toBrlCents(1000, "EUR")).isEqualTo(6000L);
    }
}
