// Espelho client-side do NutritionCalculator do backend (Fase 9C).
// Roda em tempo real enquanto o nutri ajusta parametros da prescricao,
// evitando ida ao servidor pra ver o preview. Backend mantem a autoridade
// no upsert e o dashboard.

import { ClientAssessment } from '../models/models';

export type Sex = 'M' | 'F' | 'MALE' | 'FEMALE';
export type TmbFormula = 'MIFFLIN' | 'HARRIS_BENEDICT' | 'KATCH_MCARDLE';

export const ACTIVITY_FACTORS: Record<string, number> = {
  SEDENTARY:    1.2,
  LIGHT:        1.375,
  MODERATE:     1.55,
  ACTIVE:       1.725,
  VERY_ACTIVE:  1.9,
};

const DEFAULT_PROTEIN_G_PER_KG = 1.6;
const DEFAULT_FAT_PCT          = 0.28;
const DEFAULT_WATER_ML_PER_KG  = 35;

export interface Prescription {
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  waterMl: number;
  bmr: number;
  tdee: number;
  formulaUsed: TmbFormula;
}

function calcAge(birthDate: string | null | undefined): number | null {
  if (!birthDate) return null;
  const d = new Date(birthDate);
  if (isNaN(d.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - d.getFullYear();
  const m = now.getMonth() - d.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < d.getDate())) age--;
  return age;
}

function isMale(sex: Sex | string | null | undefined): boolean {
  return sex === 'MALE' || sex === 'M';
}

export function mifflin(sex: Sex | string, weightKg: number, heightCm: number, age: number): number {
  const base = 10 * weightKg + 6.25 * heightCm - 5 * age;
  return isMale(sex) ? base + 5 : base - 161;
}

export function harrisBenedict(sex: Sex | string, weightKg: number, heightCm: number, age: number): number {
  return isMale(sex)
    ? 88.362 + 13.397 * weightKg + 4.799 * heightCm - 5.677 * age
    : 447.593 + 9.247 * weightKg + 3.098 * heightCm - 4.330 * age;
}

export function katchMcArdle(weightKg: number, bodyFatPct: number): number | null {
  if (bodyFatPct <= 0 || bodyFatPct >= 100) return null;
  const lean = weightKg * (1 - bodyFatPct / 100);
  return 370 + 21.6 * lean;
}

export function derive(
  a: ClientAssessment,
  clientBirthDate: string | null | undefined,
  clientSex: string | null | undefined,
  clientActivityFactor: number | null | undefined,
  clientDefaultWeightKg?: number | null,
  clientDefaultHeightCm?: number | null,
): Prescription | null {
  const weight = a.weightKg ?? clientDefaultWeightKg ?? null;
  const height = a.heightCm ?? clientDefaultHeightCm ?? null;
  const age = calcAge(clientBirthDate);
  if (!weight || !height || age == null || !clientSex) return null;

  let formula: TmbFormula = (a.tmbFormula as TmbFormula) || 'MIFFLIN';
  let bmr: number;
  switch (formula) {
    case 'HARRIS_BENEDICT':
      bmr = harrisBenedict(clientSex, weight, height, age); break;
    case 'KATCH_MCARDLE': {
      const km = a.bodyFatPct != null ? katchMcArdle(weight, a.bodyFatPct) : null;
      if (km == null) { bmr = mifflin(clientSex, weight, height, age); formula = 'MIFFLIN'; }
      else bmr = km;
      break;
    }
    default:
      bmr = mifflin(clientSex, weight, height, age);
  }

  const factor = a.activityFactor ?? clientActivityFactor ?? 1.4;
  const tdee = bmr * factor;

  const deltaPct = a.calorieDeltaPct ?? 0;
  const calories = tdee * (1 + deltaPct);

  const proteinPerKg = a.proteinGPerKg ?? DEFAULT_PROTEIN_G_PER_KG;
  const proteinG = weight * proteinPerKg;

  const fatShare = a.fatPct ?? DEFAULT_FAT_PCT;
  const fatG = (calories * fatShare) / 9;

  const carbsKcal = Math.max(0, calories - proteinG * 4 - fatG * 9);
  const carbsG = carbsKcal / 4;

  const waterMl = weight * DEFAULT_WATER_ML_PER_KG;

  return {
    calories: round(calories, 0),
    proteinG: round(proteinG, 1),
    carbsG:   round(carbsG, 1),
    fatG:     round(fatG, 1),
    waterMl:  round(waterMl, 0),
    bmr:      round(bmr, 0),
    tdee:     round(tdee, 0),
    formulaUsed: formula,
  };
}

function round(v: number, decimals: number): number {
  const m = Math.pow(10, decimals);
  return Math.round(v * m) / m;
}
