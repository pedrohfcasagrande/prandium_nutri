package com.nutritracker.service;

import com.nutritracker.repository.DailyFxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FxRateService {

    /** Fallback usado se a tabela nao tiver a moeda. */
    private static final Map<String, Double> FALLBACK = Map.of(
            "BRL", 1.0,
            "USD", 5.5,
            "EUR", 6.0
    );

    private final DailyFxRateRepository repository;

    /** Taxa de conversao da moeda para BRL. Usa a cotacao mais recente do banco. */
    public double toBrl(String currency) {
        String cur = currency == null ? "BRL" : currency.toUpperCase();
        return repository.findFirstByCurrencyOrderByRateDateDesc(cur)
                .map(r -> r.getRateToBrl().doubleValue())
                .orElse(FALLBACK.getOrDefault(cur, 1.0));
    }

    /** Converte cents na moeda origem para cents em BRL. */
    public long toBrlCents(long amountCents, String currency) {
        return Math.round(amountCents * toBrl(currency));
    }
}
