package com.prandiumnutri.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WaterIntakeDto(
        Long id,
        LocalDate date,
        Integer amountMl,
        LocalDateTime loggedAt
) {}
