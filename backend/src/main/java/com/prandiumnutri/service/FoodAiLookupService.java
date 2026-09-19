package com.prandiumnutri.service;

import com.prandiumnutri.dto.FoodDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Consulta o Gemini para estimar valores nutricionais de alimentos que
 * ainda nao estao no banco. Usado como fallback do FoodService.search().
 * Nao persiste — quem persiste eh o FoodService.save() quando o usuario aceita.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FoodAiLookupService {

    private static final String SYSTEM_PROMPT = ""
            + "Voce e uma base de dados nutricional. Responda APENAS com JSON puro "
            + "(sem markdown, sem cercas ```). Nunca invente valores fora de faixas realistas. "
            + "Baseie-se em fontes oficiais (TACO, USDA, tabelas do fabricante quando aplicavel).";

    private static final double MAX_KCAL = 900.0;
    private static final double MAX_MACRO = 100.0;
    private static final int MAX_RESULTS = 5;

    private final RestClient restClient;

    @Value("${prandiumnutri.gemini.api-key}")
    private String geminiKey;

    @Value("${prandiumnutri.gemini.model}")
    private String geminiModel;

    @Value("${prandiumnutri.gemini.base-url}")
    private String geminiBaseUrl;

    public List<FoodDto> lookup(String query) {
        if (query == null || query.isBlank()) return List.of();
        if (geminiKey == null || geminiKey.isBlank()) {
            log.warn("GEMINI_API_KEY nao configurada; lookup IA indisponivel");
            return List.of();
        }

        String prompt = buildPrompt(query.trim());
        try {
            String text = callGemini(prompt);
            return parseAndValidate(text);
        } catch (RuntimeException e) {
            log.warn("Falha no lookup IA para '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private String buildPrompt(String query) {
        return """
                Retorne ate %d sugestoes de alimentos/bebidas correspondentes ao termo "%s".
                Para cada item, use valores por 100g (ou 100ml para bebidas).

                Formato de resposta (JSON, sem texto extra):
                [
                  {
                    "name": "Nome comum em portugues",
                    "category": "Categoria curta (ex: Bebidas, Cereais, Carnes)",
                    "caloriesPer100g": 0.0,
                    "proteinPer100g": 0.0,
                    "carbsPer100g": 0.0,
                    "fatPer100g": 0.0
                  }
                ]

                Regras:
                - Somente alimentos/bebidas comestiveis reais.
                - Valores em faixa realistas: kcal 0-%d, macros 0-%d g.
                - Se nao houver correspondencia razoavel, retorne [].
                """.formatted(MAX_RESULTS, query, (int) MAX_KCAL, (int) MAX_MACRO);
    }

    private String callGemini(String prompt) {
        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiKey;

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        Map<?, ?> response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Sem candidates: " + response);
        }
        Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        List<?> parts = (List<?>) content.get("parts");
        return (String) ((Map<?, ?>) parts.get(0)).get("text");
    }

    List<FoodDto> parseAndValidate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return List.of();
        String cleaned = rawJson.trim()
                .replaceAll("^```(?:json)?", "")
                .replaceAll("```$", "")
                .trim();
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<?> raw = mapper.readValue(cleaned, List.class);
            List<FoodDto> out = new ArrayList<>();
            for (Object item : raw) {
                if (!(item instanceof Map<?, ?> m)) continue;
                String name = str(m.get("name"));
                if (name == null || name.isBlank()) continue;
                Double kcal = num(m.get("caloriesPer100g"));
                Double protein = num(m.get("proteinPer100g"));
                Double carbs = num(m.get("carbsPer100g"));
                Double fat = num(m.get("fatPer100g"));
                if (!inRange(kcal, 0, MAX_KCAL)
                        || !inRange(protein, 0, MAX_MACRO)
                        || !inRange(carbs, 0, MAX_MACRO)
                        || !inRange(fat, 0, MAX_MACRO)) {
                    continue;
                }
                out.add(new FoodDto(null, name.trim(), str(m.get("category")),
                        kcal, protein, carbs, fat, true));
                if (out.size() >= MAX_RESULTS) break;
            }
            return out;
        } catch (Exception e) {
            log.warn("Nao foi possivel parsear resposta Gemini para lookup de alimentos: {}", e.getMessage());
            return List.of();
        }
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static Double num(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); }
        catch (NumberFormatException e) { return null; }
    }
    private static boolean inRange(Double v, double min, double max) {
        return v != null && v >= min && v <= max;
    }
}
