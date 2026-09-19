package com.nutritracker.dto;

import jakarta.validation.constraints.NotNull;

public record SuggestionRequest(
        @NotNull Long userId,
        String extraNotes
) {}
