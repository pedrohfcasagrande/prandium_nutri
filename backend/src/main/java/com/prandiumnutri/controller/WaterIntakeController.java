package com.prandiumnutri.controller;

import com.prandiumnutri.dto.WaterIntakeDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.WaterIntakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me/water")
@RequiredArgsConstructor
public class WaterIntakeController {

    private final WaterIntakeService service;
    private final CurrentUser currentUser;

    @GetMapping
    public List<WaterIntakeDto> list(@RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.listByDate(currentUser.id(), date == null ? LocalDate.now() : date);
    }

    @PostMapping
    public WaterIntakeDto log(@RequestBody Map<String, Object> body) {
        Object raw = body.get("amountMl");
        int amount = raw instanceof Number n ? n.intValue()
                : Integer.parseInt(raw == null ? "0" : raw.toString());
        return service.log(currentUser.id(), amount);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(currentUser.id(), id);
    }
}
