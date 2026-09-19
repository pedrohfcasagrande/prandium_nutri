package com.nutritracker.dto;

import java.time.LocalDate;
import java.util.List;

public record ProgressSummaryDto(
        Double currentWeight,
        Double startingWeight,
        Double weightDelta,
        LocalDate lastEntryDate,
        int entriesCount,
        List<Point> series
) {
    public record Point(LocalDate date, Double weightKg) {}
}
