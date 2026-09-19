package com.nutritracker.domain;

public enum Goal {
    LOSE(-500, 2.0),
    MAINTAIN(0, 1.6),
    GAIN(300, 2.0);

    private final int calorieDelta;
    private final double proteinPerKg;

    Goal(int calorieDelta, double proteinPerKg) {
        this.calorieDelta = calorieDelta;
        this.proteinPerKg = proteinPerKg;
    }

    public int calorieDelta() {
        return calorieDelta;
    }

    public double proteinPerKg() {
        return proteinPerKg;
    }
}
