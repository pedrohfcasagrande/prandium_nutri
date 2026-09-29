package com.prandiumnutri.controller;

import com.prandiumnutri.dto.MealDto;
import com.prandiumnutri.dto.PhotoAnalysisItemDto;
import com.prandiumnutri.security.CurrentUser;
import com.prandiumnutri.service.MealPhotoAnalysisService;
import com.prandiumnutri.service.MealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;
    private final MealPhotoAnalysisService photoAnalysisService;
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

    /**
     * Fase 10A: cliente envia foto do prato; IA (Gemini) identifica alimentos +
     * estima porcao e valores nutricionais. Retorna lista pra o cliente editar
     * antes de confirmar como itens da refeicao. Nao persiste imagem.
     */
    @PostMapping("/analyze-photo")
    public List<PhotoAnalysisItemDto> analyzePhoto(@RequestBody Map<String, String> body) {
        if (body == null) throw new IllegalArgumentException("Body vazio");
        String base64 = body.get("imageBase64");
        String mime = body.get("mimeType");
        if (base64 == null || base64.isBlank())
            throw new IllegalArgumentException("imageBase64 obrigatorio");
        // Aceita data URL completa ("data:image/jpeg;base64,...") ou apenas o payload.
        String payload = base64;
        int comma = base64.indexOf(',');
        if (base64.startsWith("data:") && comma > 0) {
            payload = base64.substring(comma + 1);
            if (mime == null || mime.isBlank()) {
                int semi = base64.indexOf(';');
                if (semi > 0) mime = base64.substring(5, semi);
            }
        }
        // Limite defensivo: ~7MB em base64 (~5MB de imagem)
        if (payload.length() > 7_500_000)
            throw new IllegalArgumentException("Imagem muito grande (max 5MB)");
        return photoAnalysisService.analyze(payload, mime);
    }
}
