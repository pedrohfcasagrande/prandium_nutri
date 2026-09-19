package com.nutritracker.service;

import com.nutritracker.domain.ProgressEntry;
import com.nutritracker.dto.ProgressEntryDto;
import com.nutritracker.dto.ProgressSummaryDto;
import com.nutritracker.repository.ProgressEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressEntryRepository repository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ProgressEntryDto> list(Long userId, Integer days) {
        List<ProgressEntry> entries = (days == null || days <= 0)
                ? repository.findByUserIdOrderByDateAsc(userId)
                : repository.findByUserIdAndDateGreaterThanEqualOrderByDateAsc(userId, LocalDate.now().minusDays(days));
        return entries.stream().map(this::toDto).toList();
    }

    @Transactional
    public ProgressEntryDto save(Long userId, ProgressEntryDto dto) {
        ProgressEntry entry = dto.id() != null
                ? repository.findById(dto.id()).orElseThrow(() -> new EntityNotFoundException("Progress not found"))
                : new ProgressEntry();
        entry.setUser(userService.getOrThrow(userId));
        entry.setDate(dto.date());
        entry.setWeightKg(dto.weightKg());
        entry.setBodyFatPercent(dto.bodyFatPercent());
        entry.setWaistCm(dto.waistCm());
        entry.setHipCm(dto.hipCm());
        entry.setArmCm(dto.armCm());
        entry.setChestCm(dto.chestCm());
        entry.setNote(dto.note());
        return toDto(repository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ProgressSummaryDto summary(Long userId, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        List<ProgressEntry> series = repository.findByUserIdAndDateGreaterThanEqualOrderByDateAsc(userId, since);

        ProgressEntry last = repository.findFirstByUserIdOrderByDateDesc(userId).orElse(null);
        Double current = last != null ? last.getWeightKg() : null;
        Double starting = series.isEmpty() ? null : series.get(0).getWeightKg();
        Double delta = (current != null && starting != null) ? round(current - starting) : null;

        List<ProgressSummaryDto.Point> points = series.stream()
                .map(e -> new ProgressSummaryDto.Point(e.getDate(), e.getWeightKg()))
                .toList();

        return new ProgressSummaryDto(current, starting, delta,
                last != null ? last.getDate() : null, series.size(), points);
    }

    private ProgressEntryDto toDto(ProgressEntry e) {
        return new ProgressEntryDto(e.getId(), e.getDate(), e.getWeightKg(),
                e.getBodyFatPercent(), e.getWaistCm(), e.getHipCm(),
                e.getArmCm(), e.getChestCm(), e.getNote());
    }

    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
