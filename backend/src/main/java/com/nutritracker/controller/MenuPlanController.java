package com.nutritracker.controller;

import com.nutritracker.dto.MenuPlanDto;
import com.nutritracker.security.CurrentUser;
import com.nutritracker.service.MenuPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu-plans")
@RequiredArgsConstructor
public class MenuPlanController {

    private final MenuPlanService service;
    private final CurrentUser currentUser;

    @GetMapping
    public List<MenuPlanDto> list() {
        return service.list(currentUser.id());
    }

    @GetMapping("/active")
    public MenuPlanDto active() {
        return service.activeFor(currentUser.id());
    }

    @PostMapping
    public MenuPlanDto create(@Valid @RequestBody MenuPlanDto dto) {
        return service.save(currentUser.id(), dto);
    }

    @PutMapping("/{id}")
    public MenuPlanDto update(@PathVariable Long id, @Valid @RequestBody MenuPlanDto dto) {
        MenuPlanDto merged = new MenuPlanDto(id, dto.name(), dto.source(), dto.content(),
                dto.startDate(), dto.endDate(), dto.active(), null,
                null, null, null, null, null, null, null, null);
        return service.save(currentUser.id(), merged);
    }

    @PutMapping("/{id}/active")
    public MenuPlanDto setActive(@PathVariable Long id, @RequestParam boolean value) {
        return service.setActive(currentUser.id(), id, value);
    }

    @PostMapping("/{id}/submit")
    public MenuPlanDto submit(@PathVariable Long id) {
        return service.submit(currentUser.id(), id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(currentUser.id(), id);
    }
}
