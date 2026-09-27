// Calculos derivados de antropometria (Fase 9B).
// Ficam client-side pra evitar migracao/colunas redundantes e permitir
// recalculo instantaneo enquanto o nutri digita.

import { ClientAssessment } from '../models/models';

export type Sex = 'M' | 'F' | 'MALE' | 'FEMALE' | null | undefined;

export interface DerivedMetrics {
  bmi: number | null;              // Indice de Massa Corporal (peso/altura^2)
  bmiCategory: string | null;      // Baixo peso / Eutrofico / Sobrepeso / Obesidade I-III
  whr: number | null;              // Razao Cintura/Quadril
  whrRisk: 'low' | 'moderate' | 'high' | null;
  bodyFatPctCalc: number | null;   // % gordura via Jackson-Pollock 7 (quando dobras completas)
  fatMassKg: number | null;        // massa gorda (kg)
  leanMassKg: number | null;       // massa magra (kg)
}

export function calcAge(birthDate?: string | null): number | null {
  if (!birthDate) return null;
  const bd = new Date(birthDate);
  if (isNaN(bd.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - bd.getFullYear();
  const m = now.getMonth() - bd.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < bd.getDate())) age--;
  return age;
}

export function bmi(weightKg?: number | null, heightCm?: number | null): number | null {
  if (weightKg == null || heightCm == null || heightCm <= 0) return null;
  const h = heightCm / 100;
  return round(weightKg / (h * h), 1);
}

export function bmiCategory(bmiVal: number | null): string | null {
  if (bmiVal == null) return null;
  if (bmiVal < 18.5) return 'underweight';
  if (bmiVal < 25)   return 'normal';
  if (bmiVal < 30)   return 'overweight';
  if (bmiVal < 35)   return 'obesity1';
  if (bmiVal < 40)   return 'obesity2';
  return 'obesity3';
}

export function whr(waistCm?: number | null, hipCm?: number | null): number | null {
  if (waistCm == null || hipCm == null || hipCm <= 0) return null;
  return round(waistCm / hipCm, 2);
}

/**
 * Risco cardiovascular via WHR (OMS):
 * - Homens: >0.90 aumentado, >1.0 alto
 * - Mulheres: >0.80 aumentado, >0.85 alto
 */
export function whrRisk(whrVal: number | null, sex: Sex): 'low' | 'moderate' | 'high' | null {
  if (whrVal == null || !sex) return null;
  const male = sex === 'M' || sex === 'MALE';
  if (male) {
    if (whrVal > 1.0)  return 'high';
    if (whrVal > 0.90) return 'moderate';
    return 'low';
  }
  if (whrVal > 0.85) return 'high';
  if (whrVal > 0.80) return 'moderate';
  return 'low';
}

/**
 * % de gordura corporal via Jackson-Pollock 7 dobras.
 * Requer: tricipital + subescapular + suprailiaca + abdominal + coxa + peitoral + axilar media,
 * idade e sexo. Formula de Siri (1961) sobre densidade corporal.
 * Retorna null se qualquer input ausente.
 */
export function bodyFatJP7(
  a: ClientAssessment, age: number | null, sex: Sex
): number | null {
  const s = [a.sfTricipital, a.sfSubscapular, a.sfSuprailiac, a.sfAbdominal,
             a.sfThigh, a.sfChest, a.sfMidaxillary];
  if (age == null || !sex) return null;
  if (s.some(v => v == null)) return null;
  const sum = s.reduce((t, v) => t! + (v ?? 0), 0)!;
  const male = sex === 'M' || sex === 'MALE';
  const density = male
    ? 1.112 - 0.00043499 * sum + 0.00000055 * (sum * sum) - 0.00028826 * age
    : 1.097 - 0.00046971 * sum + 0.00000056 * (sum * sum) - 0.00012828 * age;
  // Siri: % gordura = (495 / densidade) - 450
  const pct = (495 / density) - 450;
  if (!isFinite(pct) || pct < 2 || pct > 60) return null; // sanity check
  return round(pct, 1);
}

export function fatMassKg(weightKg?: number | null, bodyFatPct?: number | null): number | null {
  if (weightKg == null || bodyFatPct == null) return null;
  return round(weightKg * (bodyFatPct / 100), 1);
}

export function leanMassKg(weightKg?: number | null, bodyFatPct?: number | null): number | null {
  if (weightKg == null || bodyFatPct == null) return null;
  return round(weightKg * (1 - bodyFatPct / 100), 1);
}

/** Consolida todos os derivados. Prefere %gordura calculada; senao usa o valor manual. */
export function deriveMetrics(a: ClientAssessment, birthDate?: string | null, sex?: Sex): DerivedMetrics {
  const age = calcAge(birthDate);
  const bmiVal = bmi(a.weightKg, a.heightCm);
  const whrVal = whr(a.waistCm, a.hipCm);
  const bfCalc = bodyFatJP7(a, age, sex);
  const bfEffective = bfCalc ?? a.bodyFatPct ?? null;
  return {
    bmi: bmiVal,
    bmiCategory: bmiCategory(bmiVal),
    whr: whrVal,
    whrRisk: whrRisk(whrVal, sex),
    bodyFatPctCalc: bfCalc,
    fatMassKg: fatMassKg(a.weightKg, bfEffective),
    leanMassKg: leanMassKg(a.weightKg, bfEffective),
  };
}

function round(v: number, decimals: number): number {
  const m = Math.pow(10, decimals);
  return Math.round(v * m) / m;
}
