package com.nutritracker.controller;

import com.nutritracker.dto.FoodDto;
import com.nutritracker.service.FoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public List<FoodDto> search(@RequestParam(required = false) String q) {
        return foodService.search(q);
    }

    @PostMapping
    public FoodDto create(@Valid @RequestBody FoodDto dto) {
        return foodService.save(dto);
    }
}
