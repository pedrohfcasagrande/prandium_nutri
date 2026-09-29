package com.prandiumnutri.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prandiumnutri.dto.PhotoAnalysisItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fase 10A: parser tolerante + validacao MIME.
 * Nao testa a chamada real ao Gemini (integration-test caro/instavel).
 */
class MealPhotoAnalysisServiceTest {

    private final MealPhotoAnalysisService service = new MealPhotoAnalysisService(
            RestClient.builder().build(), new ObjectMapper());

    @Test
    void parseItems_acceptsCleanJson() {
        String raw = """
                [
                  {"name": "Arroz branco", "portionGrams": 150, "calories": 200,
                   "protein": 4, "carbs": 43, "fat": 0.5, "confidence": 0.9},
                  {"name": "Feijao preto", "portionGrams": 100, "calories": 90,
                   "protein": 6, "carbs": 14, "fat": 0.3, "confidence": 0.85}
                ]
                """;
        List<PhotoAnalysisItemDto> items = service.parseItems(raw);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).name()).isEqualTo("Arroz branco");
        assertThat(items.get(0).portionGrams()).isEqualTo(150.0);
    }

    @Test
    void parseItems_stripsMarkdownFences() {
        String raw = "```json\n[{\"name\":\"X\",\"portionGrams\":100,\"calories\":100,\"protein\":5,\"carbs\":10,\"fat\":1}]\n```";
        List<PhotoAnalysisItemDto> items = service.parseItems(raw);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).name()).isEqualTo("X");
    }

    @Test
    void parseItems_filtersInvalidRanges() {
        String raw = """
                [
                  {"name": "Sanduiche", "portionGrams": 200, "calories": 400, "protein": 15, "carbs": 40, "fat": 12},
                  {"name": "Prato absurdo", "portionGrams": 20000, "calories": 3000, "protein": 5, "carbs": 5, "fat": 5},
                  {"name": "Kcal absurda", "portionGrams": 100, "calories": 99999, "protein": 5, "carbs": 5, "fat": 5},
                  {"name": "", "portionGrams": 100, "calories": 100, "protein": 5, "carbs": 5, "fat": 1}
                ]
                """;
        List<PhotoAnalysisItemDto> items = service.parseItems(raw);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).name()).isEqualTo("Sanduiche");
    }

    @Test
    void parseItems_returnsEmptyOnGarbage() {
        assertThat(service.parseItems("nao eh json")).isEmpty();
        assertThat(service.parseItems("")).isEmpty();
        assertThat(service.parseItems(null)).isEmpty();
    }

    @Test
    void analyze_rejectsUnsupportedMime() {
        assertThatThrownBy(() -> service.analyze("payload", "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MIME");
    }
}
