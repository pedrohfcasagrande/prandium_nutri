package com.prandiumnutri.controller;

import com.prandiumnutri.dto.MealDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<MealDto> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return mealService.findByUserAndDate(currentUser.id(), date);
    }

    @PostMapping
    public MealDto create(@Valid @RequestBody MealDto dto) {
        MealDto forCurrent = new MealDto(dto.id(), currentUser.id(), dto.date(), dto.time(), dto.type(),
                dto.items(), null, null, null, null);
        return mealService.save(forCurrent);
    }

    @PutMapping("/{id}")
    public MealDto update(@PathVariable Long id, @Valid @RequestBody MealDto dto) {
        MealDto merged = new MealDto(id, currentUser.id(), dto.date(), dto.time(), dto.type(),
                dto.items(), null, null, null, null);
        return mealService.save(merged);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mealService.delete(id);
    }
}
