package com.prandiumnutri.service;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.Sex;
import com.prandiumnutri.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

/**
 * Calcula gasto energetico, macros e agua.
 *
 * <p>Metodos legados (bmr/tdee/calorieGoal/proteinGoal) permanecem pra nao quebrar
 * DashboardService antigo. A Fase 9C acrescenta:
 * <ul>
 *   <li>Formulas alternativas (Harris-Benedict, Katch-McArdle);</li>
 *   <li>Prescricao completa a partir de {@link ClientAssessment} (respeita
 *       {@code proteinGPerKg}, {@code fatPct}, {@code calorieDeltaPct}, {@code activityFactor}
 *       quando setados; senao usa defaults sensatos ou o {@link User#getGoal()}).</li>
 * </ul>
 */
@Component
public class NutritionCalculator {

    // Formulas suportadas — string usada em ClientAssessment.tmbFormula
    public static final String F_MIFFLIN         = "MIFFLIN";
    public static final String F_HARRIS_BENEDICT = "HARRIS_BENEDICT";
    public static final String F_KATCH_MCARDLE   = "KATCH_MCARDLE";

    // Defaults quando o nutri nao explicitou preferencia
    private static final double DEFAULT_PROTEIN_G_PER_KG = 1.6;
    private static final double DEFAULT_FAT_PCT          = 0.28;
    private static final double DEFAULT_WATER_ML_PER_KG  = 35.0;

    // ---------- Metodos legados (mantidos por compat) ----------

    public double bmr(User user) {
        return mifflin(user.getSex(), user.getWeightKg(), user.getHeightCm(),
                age(user.getBirthDate()));
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

    // ---------- Fase 9C: formulas ----------

    /** Mifflin-St Jeor (padrao ouro na pratica clinica). */
    public double mifflin(Sex sex, double weightKg, double heightCm, int age) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return sex == Sex.MALE ? base + 5 : base - 161;
    }

    /** Harris-Benedict revisada (Roza & Shizgal, 1984). */
    public double harrisBenedict(Sex sex, double weightKg, double heightCm, int age) {
        return sex == Sex.MALE
                ? 88.362 + (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age)
                : 447.593 + (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age);
    }

    /**
     * Katch-McArdle — usa massa magra. So aplica se %gordura conhecido.
     * lean = weight * (1 - fatPct/100). Retorna NaN se input invalido pra caller decidir.
     */
    public double katchMcArdle(double weightKg, double bodyFatPct) {
        if (bodyFatPct <= 0 || bodyFatPct >= 100) return Double.NaN;
        double lean = weightKg * (1.0 - bodyFatPct / 100.0);
        return 370 + 21.6 * lean;
    }

    // ---------- Fase 9C: prescricao completa a partir da ficha ----------

    public record Prescription(
            double calories, double proteinG, double carbsG, double fatG, double waterMl,
            String formulaUsed, double bmr, double tdee
    ) {}

    /**
     * Calcula prescricao completa a partir da ficha + dados do usuario.
     * Prefere valores explicitos na ficha (proteinGPerKg, fatPct, calorieDeltaPct, activityFactor).
     * Requer weight + height + birthDate + sex — senao retorna null.
     */
    public Prescription derive(ClientAssessment a, User client) {
        Double weight = firstNonNull(a.getWeightKg(), client.getWeightKg());
        Double height = a.getHeightCm() != null
                ? Double.valueOf(a.getHeightCm())
                : client.getHeightCm();
        LocalDate birth = client.getBirthDate();
        Sex sex = client.getSex();
        if (weight == null || height == null || birth == null || sex == null) return null;

        int age = age(birth);
        String formula = a.getTmbFormula() != null ? a.getTmbFormula() : F_MIFFLIN;

        double bmr;
        switch (formula) {
            case F_HARRIS_BENEDICT -> bmr = harrisBenedict(sex, weight, height, age);
            case F_KATCH_MCARDLE -> {
                Double bf = a.getBodyFatPct();
                double km = bf != null ? katchMcArdle(weight, bf) : Double.NaN;
                if (Double.isNaN(km)) {
                    // fallback: sem %gordura, Katch nao aplica -> usa Mifflin
                    bmr = mifflin(sex, weight, height, age);
                    formula = F_MIFFLIN;
                } else {
                    bmr = km;
                }
            }
            default -> bmr = mifflin(sex, weight, height, age);
        }

        double factor = a.getActivityFactor() != null
                ? a.getActivityFactor()
                : (client.getActivityLevel() != null ? client.getActivityLevel().factor() : 1.4);
        double tdee = bmr * factor;

        double deltaPct = a.getCalorieDeltaPct() != null ? a.getCalorieDeltaPct() : 0.0;
        double calories = tdee * (1.0 + deltaPct);

        double proteinPerKg = a.getProteinGPerKg() != null ? a.getProteinGPerKg() : DEFAULT_PROTEIN_G_PER_KG;
        double proteinG = weight * proteinPerKg;

        double fatShare = a.getFatPct() != null ? a.getFatPct() : DEFAULT_FAT_PCT;
        double fatG = (calories * fatShare) / 9.0;

        double proteinKcal = proteinG * 4.0;
        double fatKcal = fatG * 9.0;
        double carbsKcal = Math.max(0, calories - proteinKcal - fatKcal);
        double carbsG = carbsKcal / 4.0;

        double waterMl = weight * DEFAULT_WATER_ML_PER_KG;

        return new Prescription(
                round(calories, 0), round(proteinG, 1), round(carbsG, 1), round(fatG, 1),
                round(waterMl, 0), formula, round(bmr, 0), round(tdee, 0)
        );
    }

    private int age(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private static Double firstNonNull(Double a, Double b) { return a != null ? a : b; }

    private static double round(double v, int decimals) {
        double m = Math.pow(10, decimals);
        return Math.round(v * m) / m;
    }
}
