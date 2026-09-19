package com.prandiumnutri.service;

import com.prandiumnutri.domain.Sex;
import com.prandiumnutri.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class NutritionCalculator {

    public double bmr(User user) {
        int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        double base = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * age;
        return user.getSex() == Sex.MALE ? base + 5 : base - 161;
    }

    public double tdee(User user) {
        return bmr(user) * user.getActivityLevel().factor();
    }

    public double calorieGoal(User user) {
        return tdee(user) + user.getGoal().calorieDelta();
    }

    public double proteinGoal(User user) {
        return user.getWeightKg() * user.getGoal().proteinPerKg();
    }
}
