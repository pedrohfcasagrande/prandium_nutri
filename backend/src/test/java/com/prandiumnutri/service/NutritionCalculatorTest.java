package com.prandiumnutri.service;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.Sex;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.domain.ActivityLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NutritionCalculatorTest {

    private final NutritionCalculator calc = new NutritionCalculator();

    @Test
    void mifflin_male_matchesReference() {
        // Homem, 30 anos, 80 kg, 180 cm -> 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        assertThat(calc.mifflin(Sex.MALE, 80, 180, 30)).isCloseTo(1780.0, within(0.5));
    }

    @Test
    void mifflin_female_matchesReference() {
        // Mulher, 30 anos, 65 kg, 165 cm -> 10*65 + 6.25*165 - 5*30 - 161 = 650 + 1031.25 - 150 - 161 = 1370.25
        assertThat(calc.mifflin(Sex.FEMALE, 65, 165, 30)).isCloseTo(1370.25, within(0.5));
    }

    @Test
    void harrisBenedict_male_matchesReference() {
        // Homem, 30 anos, 80 kg, 180 cm -> 88.362 + 13.397*80 + 4.799*180 - 5.677*30
        double expected = 88.362 + 13.397 * 80 + 4.799 * 180 - 5.677 * 30;
        assertThat(calc.harrisBenedict(Sex.MALE, 80, 180, 30)).isCloseTo(expected, within(0.5));
    }

    @Test
    void katchMcArdle_usesLeanMass() {
        // 80 kg com 15% gordura -> lean = 68, BMR = 370 + 21.6*68 = 1838.8
        assertThat(calc.katchMcArdle(80, 15)).isCloseTo(1838.8, within(0.5));
    }

    @Test
    void katchMcArdle_returnsNanWhenPctInvalid() {
        assertThat(calc.katchMcArdle(80, 0)).isNaN();
        assertThat(calc.katchMcArdle(80, 100)).isNaN();
    }

    @Test
    void derive_completesFullPrescription() {
        User client = User.builder()
                .name("Test").sex(Sex.MALE)
                .birthDate(LocalDate.now().minusYears(30))
                .weightKg(80.0).heightCm(180.0)
                .activityLevel(ActivityLevel.MODERATE) // fator 1.55
                .build();
        ClientAssessment a = ClientAssessment.builder()
                .weightKg(80.0).heightCm(180)
                .tmbFormula("MIFFLIN")
                .proteinGPerKg(2.0)
                .fatPct(0.30)
                .calorieDeltaPct(-0.20) // deficit 20%
                .build();

        var rx = calc.derive(a, client);
        assertThat(rx).isNotNull();
        // Mifflin masc 80/180/30 = 1780; TDEE = 1780*1.55 = 2759; -20% = 2207
        assertThat(rx.calories()).isCloseTo(2207, within(2.0));
        assertThat(rx.proteinG()).isEqualTo(160.0); // 80 * 2.0
        assertThat(rx.fatG()).isGreaterThan(60); // 30% de 2207 / 9 ~ 73.5g
        assertThat(rx.carbsG()).isGreaterThan(150);
        assertThat(rx.waterMl()).isEqualTo(2800.0); // 80 * 35
        assertThat(rx.formulaUsed()).isEqualTo("MIFFLIN");
    }

    @Test
    void derive_katchFallsBackToMifflinWhenBodyFatMissing() {
        User client = User.builder()
                .sex(Sex.MALE).birthDate(LocalDate.now().minusYears(30))
                .weightKg(80.0).heightCm(180.0).activityLevel(ActivityLevel.SEDENTARY)
                .build();
        ClientAssessment a = ClientAssessment.builder()
                .weightKg(80.0).heightCm(180)
                .tmbFormula("KATCH_MCARDLE")
                .build();
        var rx = calc.derive(a, client);
        assertThat(rx).isNotNull();
        assertThat(rx.formulaUsed()).isEqualTo("MIFFLIN"); // fallback
    }

    @Test
    void derive_returnsNullWhenInputIncomplete() {
        User client = User.builder().name("x").build();
        ClientAssessment a = ClientAssessment.builder().build();
        assertThat(calc.derive(a, client)).isNull();
    }
}
