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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FxRateSyncServiceTest {

    @Autowired FxRateSyncService syncService;
    @Autowired FxRateService fxRateService;
    @Autowired DailyFxRateRepository repository;

    /**
     * Snapshot realista da open.er-api em 2026-09: base USD.
     * 1 USD = 5.42 BRL; 1 USD = 0.92 EUR (=> 1 EUR ≈ 5.891 BRL).
     */
    private static Map<String, Double> sampleRates() {
        return Map.of(
                "USD", 1.0,
                "BRL", 5.42,
                "EUR", 0.92,
                "GBP", 0.78 // moeda nao rastreada — deve ser ignorada
        );
    }

    @Test
    void applyRates_persistsTrackedCurrenciesConvertedToBrl() {
        LocalDate today = LocalDate.now();
        int updated = syncService.applyRatesFromUsd(sampleRates(), today);

        assertThat(updated).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2L);

        DailyFxRate usd = repository.findFirstByCurrencyOrderByRateDateDesc("USD").orElseThrow();
        assertThat(usd.getRateToBrl()).isEqualByComparingTo(new BigDecimal("5.420000"));
        assertThat(usd.getSource()).isEqualTo("OPEN_ER_API");

        DailyFxRate eur = repository.findFirstByCurrencyOrderByRateDateDesc("EUR").orElseThrow();
        // 5.42 / 0.92 = 5.891304
        assertThat(eur.getRateToBrl().doubleValue()).isCloseTo(5.891304, within(0.0001));
    }

    @Test
    void applyRates_isIdempotentPerDay() {
        LocalDate today = LocalDate.now();
        syncService.applyRatesFromUsd(sampleRates(), today);
        // segunda chamada com taxa diferente atualiza o mesmo row, nao insere
        syncService.applyRatesFromUsd(Map.of(
                "USD", 1.0,
                "BRL", 5.99,
                "EUR", 0.90), today);

        assertThat(repository.count()).isEqualTo(2L);
        assertThat(repository.findFirstByCurrencyOrderByRateDateDesc("USD").orElseThrow()
                .getRateToBrl()).isEqualByComparingTo(new BigDecimal("5.990000"));
    }

    @Test
    void applyRates_skipsWhenBrlMissing() {
        int updated = syncService.applyRatesFromUsd(Map.of("USD", 1.0, "EUR", 0.9), LocalDate.now());
        assertThat(updated).isZero();
        assertThat(repository.count()).isZero();
    }

    @Test
    void applyRates_skipsCurrencyWhenRateMissing() {
        int updated = syncService.applyRatesFromUsd(Map.of("USD", 1.0, "BRL", 5.42), LocalDate.now());
        // EUR ausente — so USD entra
        assertThat(updated).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1L);
        assertThat(repository.findFirstByCurrencyOrderByRateDateDesc("USD")).isPresent();
        assertThat(repository.findFirstByCurrencyOrderByRateDateDesc("EUR")).isEmpty();
    }

    @Test
    void fxRateService_readsSyncedRow() {
        // integracao entre sync e read
        syncService.applyRatesFromUsd(sampleRates(), LocalDate.now());
        assertThat(fxRateService.toBrl("USD")).isEqualTo(5.42);
        assertThat(fxRateService.toBrl("EUR")).isCloseTo(5.891304, within(0.0001));
        // moeda nao sincronizada continua no fallback
        assertThat(fxRateService.toBrl("GBP")).isEqualTo(1.0); // GBP nao existe no FALLBACK -> 1.0
    }

    /** Sanity: TRACKED nao inclui BRL nem GBP, so USD/EUR. */
    @Test
    void trackedCurrenciesAreUsdAndEur() {
        assertThat(FxRateSyncService.TRACKED).containsExactly("USD", "EUR");
    }
}
