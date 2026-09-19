package com.prandiumnutri.controller;

import com.prandiumnutri.dto.FoodDto;
import com.prandiumnutri.service.FoodAiLookupService;
import com.prandiumnutri.service.FoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final FoodAiLookupService aiLookupService;

    @GetMapping
    public List<FoodDto> search(@RequestParam(required = false) String q) {
        return foodService.search(q);
    }

    /** Consulta a IA por alimentos que nao existem no banco. Nao persiste. */
    @GetMapping("/ai-lookup")
    public List<FoodDto> aiLookup(@RequestParam String q) {
        return aiLookupService.lookup(q);
    }

    @PostMapping
    public FoodDto create(@Valid @RequestBody FoodDto dto) {
        return foodService.save(dto);
    }
}
