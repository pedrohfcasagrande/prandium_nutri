package com.prandiumnutri.service;

import com.prandiumnutri.domain.DailyFxRate;
import com.prandiumnutri.repository.DailyFxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fase 4B refino (2026-09-29): job diario que atualiza cotacoes de moedas
 * estrangeiras para BRL. Preenche gap deixado la em Setembro — o `FxRateService`
 * lia da tabela, mas nao havia quem escrevesse alem do seed manual.
 *
 * <p>Fonte default: {@code open.er-api.com} (gratis, sem chave, latency baixa).
 * Retorna JSON no formato {@code {"rates": {"BRL": 5.42, "EUR": 0.92, ...}}}
 * com {@code USD} como base. Convertemos X→BRL fazendo {@code rates.BRL / rates.X}.
 *
 * <p>Idempotente: se ja existe row para a moeda + rateDate=hoje, atualiza a taxa
 * em vez de inserir duplicata. Falha da API vira warning + no-op (proximo dia
 * tenta de novo). Sem retry — se cair 24h, o {@code FxRateService} usa FALLBACK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FxRateSyncService {

    /** Moedas que efetivamente aparecem em cobrancas (Fase 3A). */
    static final List<String> TRACKED = List.of("USD", "EUR");
    static final String SOURCE = "OPEN_ER_API";

    private final DailyFxRateRepository repository;
    private final RestClient restClient;

    @Value("${prandiumnutri.fx.base-url:https://open.er-api.com/v6}")
    private String baseUrl;

    @Value("${prandiumnutri.fx.enabled:true}")
    private boolean enabled;

    /**
     * Roda diariamente as 06:30 (horario do servidor). API atualiza uma vez
     * ao dia — nao ha ganho em rodar mais que 1x/dia.
     */
    @Scheduled(cron = "0 30 6 * * *")
    public void scheduledSync() {
        if (!enabled) {
            log.debug("Fx sync desabilitado (prandiumnutri.fx.enabled=false)");
            return;
        }
        syncNow();
    }

    /** Manual trigger — usado em teste + endpoint admin (caso necessario). */
    @Transactional
    public int syncNow() {
        Map<String, Double> rates = fetchUsdRates();
        if (rates == null) return 0;
        return applyRatesFromUsd(rates, LocalDate.now());
    }

    /**
     * Aplica um snapshot de cotacoes (base=USD) e persiste na data indicada.
     * Package-private pra permitir teste sem HTTP.
     */
    @Transactional
    int applyRatesFromUsd(Map<String, Double> ratesFromUsd, LocalDate date) {
        Double usdToBrl = ratesFromUsd.get("BRL");
        if (usdToBrl == null || usdToBrl <= 0) {
            log.warn("Fx API nao retornou BRL — pulando sync");
            return 0;
        }
        int updated = 0;
        for (String cur : TRACKED) {
            Double curPerUsd = ratesFromUsd.get(cur);
            if (curPerUsd == null || curPerUsd <= 0) {
                log.warn("Fx API nao retornou {} — pulando", cur);
                continue;
            }
            // X -> BRL: rates[cur] = unidades de cur por 1 USD; rates[BRL] = BRL por 1 USD.
            // => 1 cur = (rates.BRL / rates.cur) BRL.
            BigDecimal rate = BigDecimal.valueOf(usdToBrl)
                    .divide(BigDecimal.valueOf(curPerUsd), 6, RoundingMode.HALF_UP);
            upsert(cur, rate, date);
            updated++;
        }
        log.info("Fx sync concluido: {} moedas atualizadas ({})", updated, date);
        return updated;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> fetchUsdRates() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(baseUrl + "/latest/USD")
                    .retrieve()
                    .body(Map.class);
            if (response == null) return null;
            String result = (String) response.get("result");
            if (result != null && !"success".equalsIgnoreCase(result)) {
                log.warn("Fx API retornou result={}", result);
                return null;
            }
            Object rates = response.get("rates");
            if (!(rates instanceof Map<?, ?> map)) return null;
            // Copia so os valores numericos (protege contra tipos inesperados)
            java.util.HashMap<String, Double> out = new java.util.HashMap<>();
            for (var e : ((Map<String, Object>) map).entrySet()) {
                if (e.getValue() instanceof Number n) {
                    out.put(e.getKey(), n.doubleValue());
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("Falha ao buscar cotacoes: {}", e.getMessage());
            return null;
        }
    }

    private void upsert(String currency, BigDecimal rate, LocalDate date) {
        Optional<DailyFxRate> existing = repository
                .findFirstByCurrencyOrderByRateDateDesc(currency)
                .filter(r -> r.getRateDate().equals(date));
        if (existing.isPresent()) {
            DailyFxRate row = existing.get();
            row.setRateToBrl(rate);
            row.setSource(SOURCE);
            repository.save(row);
        } else {
            repository.save(DailyFxRate.builder()
                    .currency(currency)
                    .rateToBrl(rate)
                    .rateDate(date)
                    .source(SOURCE)
                    .build());
        }
    }
}
