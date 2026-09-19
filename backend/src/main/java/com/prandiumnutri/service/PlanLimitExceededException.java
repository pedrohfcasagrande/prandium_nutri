package com.prandiumnutri.service;

import lombok.Getter;

@Getter
public class PlanLimitExceededException extends RuntimeException {
    private final String feature;
    private final int limit;
    private final int current;
    private final String planCode;

    public PlanLimitExceededException(String feature, int limit, int current, String planCode) {
        super("Limite do plano atingido para " + feature + " (" + current + "/" + limit + ")");
        this.feature = feature;
        this.limit = limit;
        this.current = current;
        this.planCode = planCode;
    }
}
