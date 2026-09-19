package com.nutritracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detecta currency (BRL/USD/EUR) a partir de um IP. Prioridade:
 * 1) IP mapeado -> pais
 * 2) BR -> BRL, EU -> EUR, resto -> USD
 * Cache em memoria (ConcurrentHashMap) para nao chamar a API a cada request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyDetectionService {

    private static final Set<String> EUR_COUNTRIES = Set.of(
            "DE","FR","IT","ES","PT","NL","BE","IE","LU","AT","FI","GR","MT","CY",
            "EE","LV","LT","SI","SK","HR"
    );
    private static final Set<String> SUPPORTED = Set.of("BRL", "USD", "EUR");

    private final RestClient restClient;

    private final Map<String, String> ipToCountry = new ConcurrentHashMap<>();

    public String detectCurrency(String ip, String acceptLanguage) {
        String country = detectCountry(ip);
        if (country != null) {
            if ("BR".equalsIgnoreCase(country)) return "BRL";
            if (EUR_COUNTRIES.contains(country.toUpperCase())) return "EUR";
            return "USD";
        }
        // Fallback pelo Accept-Language
        if (acceptLanguage != null) {
            String lang = acceptLanguage.toLowerCase();
            if (lang.startsWith("pt")) return "BRL";
            if (lang.startsWith("en")) return "USD";
            if (lang.startsWith("fr") || lang.startsWith("de") || lang.startsWith("it")
                    || lang.startsWith("es") || lang.startsWith("nl")) return "EUR";
        }
        return "USD";
    }

    public boolean isSupported(String currency) {
        return currency != null && SUPPORTED.contains(currency.toUpperCase());
    }

    private String detectCountry(String ip) {
        if (ip == null || ip.isBlank() || ip.startsWith("127.") || ip.startsWith("192.168.")
                || ip.startsWith("10.") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return null; // IP local, sem lookup
        }
        String cached = ipToCountry.get(ip);
        if (cached != null) return cached.equals("-") ? null : cached;
        try {
            Map<?, ?> resp = restClient.get()
                    .uri("https://ipapi.co/{ip}/json/", ip)
                    .retrieve()
                    .body(Map.class);
            if (resp != null && resp.get("country_code") instanceof String cc && !cc.isBlank()) {
                ipToCountry.put(ip, cc);
                return cc;
            }
        } catch (Exception e) {
            log.debug("Falha ao detectar pais para IP {}: {}", ip, e.getMessage());
        }
        ipToCountry.put(ip, "-"); // marca como "tentado e falhou"
        return null;
    }
}
