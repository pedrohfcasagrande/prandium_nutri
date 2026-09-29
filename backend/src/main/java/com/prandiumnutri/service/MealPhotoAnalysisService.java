package com.prandiumnutri.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prandiumnutri.dto.PhotoAnalysisItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fase 10A: envia foto de prato pra Gemini (multimodal) e devolve lista de
 * alimentos estimados (nome + porcao em gramas + kcal + macros).
 *
 * Nao armazenamos a imagem — enviamos, recebemos analise, descartamos.
 * Simplifica LGPD (nada de fotos de comida no banco) e evita explodir storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealPhotoAnalysisService {

    private static final int MAX_ITEMS = 8;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private static final String SYSTEM_PROMPT = """
            Voce e um nutricionista assistente. Sua tarefa e analisar fotos de
            refeicoes e estimar cada componente visivel no prato: identificar
            alimentos, estimar peso servido (em gramas) e valores nutricionais
            da porcao mostrada. Nunca invente itens que nao aparecem na foto.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${prandiumnutri.gemini.api-key}")
    private String geminiKey;

    @Value("${prandiumnutri.gemini.model}")
    private String geminiModel;

    @Value("${prandiumnutri.gemini.base-url}")
    private String geminiBaseUrl;

    /**
     * Envia foto pra Gemini e devolve lista de alimentos identificados.
     * @param base64Data payload da imagem (sem prefixo data URL)
     * @param mimeType   ex.: image/jpeg
     */
    public List<PhotoAnalysisItemDto> analyze(String base64Data, String mimeType) {
        if (base64Data == null || base64Data.isBlank()) return List.of();
        if (mimeType == null || !ALLOWED_MIME.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("MIME nao suportado: " + mimeType
                    + " (aceitos: " + ALLOWED_MIME + ")");
        }
        if (geminiKey == null || geminiKey.isBlank()) {
            log.warn("Gemini key vazio — retornando []");
            return List.of();
        }

        String prompt = buildPrompt();
        Map<?, ?> response;
        try {
            response = call(prompt, base64Data, mimeType);
        } catch (Exception e) {
            log.warn("Falha ao chamar Gemini vision: {}", e.getMessage());
            return List.of();
        }
        String text = extractText(response);
        return parseItems(text);
    }

    private String buildPrompt() {
        return """
                Analise a foto e liste ate %d alimentos identificaveis no prato.
                Para cada um, estime a porcao em gramas visivel e os valores
                nutricionais da porcao (kcal, proteina, carbos, gordura).

                Retorne SO um JSON array, sem texto adicional:
                [
                  {
                    "name": "Nome do alimento em portugues",
                    "portionGrams": 150.0,
                    "calories": 200.0,
                    "protein": 5.0,
                    "carbs": 40.0,
                    "fat": 2.0,
                    "confidence": 0.85
                  }
                ]

                Regras:
                - Nao invente itens que nao estao visiveis.
                - portionGrams positivo, calories e macros >= 0.
                - confidence de 0 a 1 (o quao certo voce esta da identificacao).
                - Se a imagem nao tiver comida visivel, retorne [].
                - Nunca use markdown (```json), so o array puro.
                """.formatted(MAX_ITEMS);
    }

    private Map<?, ?> call(String prompt, String base64Data, String mimeType) {
        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiKey;
        Map<String, Object> imagePart = Map.of("inline_data",
                Map.of("mime_type", mimeType, "data", base64Data));
        Map<String, Object> textPart = Map.of("text", prompt);

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", List.of(Map.of("parts", List.of(imagePart, textPart))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json"
                )
        );

        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "[]";
        Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        if (content == null) return "[]";
        List<?> parts = (List<?>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "[]";
        Object t = ((Map<?, ?>) parts.get(0)).get("text");
        return t == null ? "[]" : t.toString();
    }

    /** Parser tolerante — aceita array JSON puro, remove cercas markdown, filtra invalidos. */
    List<PhotoAnalysisItemDto> parseItems(String raw) {
        if (raw == null) return List.of();
        String cleaned = stripMarkdownFences(raw).trim();
        if (cleaned.isEmpty()) return List.of();
        try {
            List<Map<String, Object>> list = objectMapper.readValue(cleaned,
                    new TypeReference<List<Map<String, Object>>>() {});
            return list.stream()
                    .map(this::toDto)
                    .filter(this::isValid)
                    .limit(MAX_ITEMS)
                    .toList();
        } catch (Exception e) {
            log.warn("Falha ao parsear analise IA: {}", e.getMessage());
            return List.of();
        }
    }

    private PhotoAnalysisItemDto toDto(Map<String, Object> m) {
        return new PhotoAnalysisItemDto(
                asString(m.get("name")),
                asDouble(m.get("portionGrams")),
                asDouble(m.get("calories")),
                asDouble(m.get("protein")),
                asDouble(m.get("carbs")),
                asDouble(m.get("fat")),
                asDouble(m.get("confidence"))
        );
    }

    private boolean isValid(PhotoAnalysisItemDto d) {
        if (d.name() == null || d.name().isBlank()) return false;
        // Filtra valores absurdos
        if (d.portionGrams() != null && (d.portionGrams() <= 0 || d.portionGrams() > 5000)) return false;
        if (d.calories() != null && (d.calories() < 0 || d.calories() > 5000)) return false;
        return true;
    }

    private static final Pattern FENCE = Pattern.compile("^```(?:json)?\\s*|\\s*```$");

    private String stripMarkdownFences(String s) {
        Matcher m = FENCE.matcher(s);
        return m.replaceAll("");
    }

    private String asString(Object o) { return o == null ? null : o.toString(); }

    private Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
