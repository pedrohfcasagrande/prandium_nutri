package com.prandiumnutri.controller;

import com.prandiumnutri.dto.ProgressEntryDto;
import com.prandiumnutri.dto.ProgressSummaryDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<ProgressEntryDto> list(@RequestParam(required = false) Integer days) {
        return progressService.list(currentUser.id(), days);
    }

    @GetMapping("/summary")
    public ProgressSummaryDto summary(@RequestParam(defaultValue = "30") int days) {
        return progressService.summary(currentUser.id(), days);
    }

    @PostMapping
    public ProgressEntryDto create(@Valid @RequestBody ProgressEntryDto dto) {
        return progressService.save(currentUser.id(), dto);
    }

    @PutMapping("/{id}")
    public ProgressEntryDto update(@PathVariable Long id, @Valid @RequestBody ProgressEntryDto dto) {
        ProgressEntryDto merged = new ProgressEntryDto(id, dto.date(), dto.weightKg(),
                dto.bodyFatPercent(), dto.waistCm(), dto.hipCm(), dto.armCm(), dto.chestCm(), dto.note());
        return progressService.save(currentUser.id(), merged);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        progressService.delete(id);
    }
}
