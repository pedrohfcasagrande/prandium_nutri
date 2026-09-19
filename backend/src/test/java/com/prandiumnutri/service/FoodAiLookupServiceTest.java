package com.prandiumnutri.service;

import com.prandiumnutri.dto.FoodDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodAiLookupServiceTest {

    private final FoodAiLookupService service = new FoodAiLookupService(null);

    @Test
    void parseAndValidate_returnsWellFormedItems() {
        String json = """
                [
                  {"name":"Coca-Cola","category":"Bebidas","caloriesPer100g":42,"proteinPer100g":0,"carbsPer100g":10.6,"fatPer100g":0},
                  {"name":"Suco de laranja","category":"Bebidas","caloriesPer100g":45,"proteinPer100g":0.7,"carbsPer100g":10.4,"fatPer100g":0.2}
                ]
                """;
        var out = service.parseAndValidate(json);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).name()).isEqualTo("Coca-Cola");
        assertThat(out.get(0).aiSuggested()).isTrue();
        assertThat(out.get(0).id()).isNull();
    }

    @Test
    void parseAndValidate_stripsMarkdownFences() {
        String json = "```json\n[{\"name\":\"Agua\",\"caloriesPer100g\":0,\"proteinPer100g\":0,\"carbsPer100g\":0,\"fatPer100g\":0}]\n```";
        var out = service.parseAndValidate(json);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).name()).isEqualTo("Agua");
    }

    @Test
    void parseAndValidate_dropsItemsOutOfRange() {
        String json = """
                [
                  {"name":"Impossivel","caloriesPer100g":9999,"proteinPer100g":50,"carbsPer100g":10,"fatPer100g":10},
                  {"name":"OK","caloriesPer100g":100,"proteinPer100g":10,"carbsPer100g":20,"fatPer100g":2}
                ]
                """;
        var out = service.parseAndValidate(json);
        assertThat(out).extracting(FoodDto::name).containsExactly("OK");
    }

    @Test
    void parseAndValidate_returnsEmptyOnGarbage() {
        assertThat(service.parseAndValidate("nao e json")).isEmpty();
        assertThat(service.parseAndValidate(null)).isEmpty();
        assertThat(service.parseAndValidate("")).isEmpty();
    }

    @Test
    void parseAndValidate_skipsItemsWithoutName() {
        String json = "[{\"name\":\"\",\"caloriesPer100g\":10,\"proteinPer100g\":1,\"carbsPer100g\":1,\"fatPer100g\":1}]";
        assertThat(service.parseAndValidate(json)).isEmpty();
    }
}
