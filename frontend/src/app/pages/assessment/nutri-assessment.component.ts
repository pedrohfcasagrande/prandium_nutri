import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PracticeService } from '../../services/practice.service';
import { AssessmentSnapshot, ClientAssessment, ClientLink, Consultation, ConsultationType } from '../../models/models';
import { deriveMetrics } from '../../util/assessment-metrics';
import { ACTIVITY_FACTORS, derive as derivePrescription } from '../../util/nutrition-calculator';
import { AssessmentTrendChartComponent } from '../../components/assessment-trend-chart.component';

@Component({
  selector: 'app-nutri-assessment',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule,
    MatExpansionModule, MatSnackBarModule, MatDialogModule, MatTooltipModule, TranslateModule,
    AssessmentTrendChartComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'assessment.title' | translate }}</h1>
          <p>{{ clientName() || ('assessment.subtitle' | translate) }}</p>
        </div>
        <button mat-stroked-button (click)="back()">
          <mat-icon>arrow_back</mat-icon>
          {{ 'assessment.back' | translate }}
        </button>
      </div>

      @if (draft(); as d) {
        <mat-accordion multi>
          <!-- 1. Antropometria -->
          <mat-expansion-panel [expanded]="true">
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>straighten</mat-icon>
                {{ 'assessment.sections.anthropometry' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.anthropometryHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <div class="grid">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.weightKg' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.weightKg"><span matSuffix>kg</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.heightCm' | translate }}</mat-label>
                <input matInput type="number" [(ngModel)]="d.heightCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.bodyFatPct' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.bodyFatPct"><span matSuffix>%</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.waistCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.waistCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.hipCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.hipCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.neckCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.neckCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.armCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.armCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.thighCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.thighCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.calfCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.calfCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.chestCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.chestCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.forearmCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.forearmCm"><span matSuffix>cm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.abdomenCm' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.abdomenCm"><span matSuffix>cm</span>
              </mat-form-field>
            </div>

            @if (derived(); as m) {
              <div class="derived-strip">
                @if (m.bmi != null) {
                  <div class="pill">
                    <span class="p-label">IMC</span>
                    <span class="p-val">{{ m.bmi }}</span>
                    <span class="p-note">{{ ('assessment.bmi.' + m.bmiCategory) | translate }}</span>
                  </div>
                }
                @if (m.whr != null) {
                  <div class="pill" [class.risk-mod]="m.whrRisk === 'moderate'"
                                    [class.risk-hi]="m.whrRisk === 'high'">
                    <span class="p-label">RCQ</span>
                    <span class="p-val">{{ m.whr }}</span>
                    @if (m.whrRisk) {
                      <span class="p-note">{{ ('assessment.whr.' + m.whrRisk) | translate }}</span>
                    }
                  </div>
                }
                @if (m.bodyFatPctCalc != null) {
                  <div class="pill">
                    <span class="p-label">% G (JP-7)</span>
                    <span class="p-val">{{ m.bodyFatPctCalc }}%</span>
                  </div>
                }
                @if (m.fatMassKg != null) {
                  <div class="pill">
                    <span class="p-label">MG</span>
                    <span class="p-val">{{ m.fatMassKg }} kg</span>
                  </div>
                }
                @if (m.leanMassKg != null) {
                  <div class="pill">
                    <span class="p-label">MM</span>
                    <span class="p-val">{{ m.leanMassKg }} kg</span>
                  </div>
                }
              </div>
            }
          </mat-expansion-panel>

          <!-- 1b. Dobras cutaneas (Jackson-Pollock) -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>compress</mat-icon>
                {{ 'assessment.sections.skinfolds' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.skinfoldsHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.skinfoldProtocol' | translate }}</mat-label>
              <mat-select [(ngModel)]="d.skinfoldProtocol">
                <mat-option [value]="null">-</mat-option>
                <mat-option value="JP_7">{{ 'assessment.skinfoldProtocol.JP_7' | translate }}</mat-option>
                <mat-option value="JP_3">{{ 'assessment.skinfoldProtocol.JP_3' | translate }}</mat-option>
                <mat-option value="DURNIN_4">{{ 'assessment.skinfoldProtocol.DURNIN_4' | translate }}</mat-option>
                <mat-option value="POLLOCK_3">{{ 'assessment.skinfoldProtocol.POLLOCK_3' | translate }}</mat-option>
                <mat-option value="MANUAL">{{ 'assessment.skinfoldProtocol.MANUAL' | translate }}</mat-option>
              </mat-select>
            </mat-form-field>
            <div class="grid">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sfTricipital' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.sfTricipital"><span matSuffix>mm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sfSubscapular' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.sfSubscapular"><span matSuffix>mm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sfSuprailiac' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.sfSuprailiac"><span matSuffix>mm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sfAbdominal' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.sfAbdominal"><span matSuffix>mm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sfThigh' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.sfThigh"><span matSuffix>mm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sfChest' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.sfChest"><span matSuffix>mm</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sfMidaxillary' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.sfMidaxillary"><span matSuffix>mm</span>
              </mat-form-field>
            </div>
            <p class="text-muted small">{{ 'assessment.skinfolds.formulaNote' | translate }}</p>
          </mat-expansion-panel>

          <!-- 2. Perfil clinico -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>medical_information</mat-icon>
                {{ 'assessment.sections.clinical' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.clinicalHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.chiefComplaint' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.chiefComplaint"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.medicalHistory' | translate }}</mat-label>
              <textarea matInput rows="3" [(ngModel)]="d.medicalHistory"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.medications' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.medications"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.preExistingConditions' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.preExistingConditions"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.allergies' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.allergies"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.familyHistory' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.familyHistory"></textarea>
            </mat-form-field>
          </mat-expansion-panel>

          <!-- 3. Habitos alimentares -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>restaurant</mat-icon>
                {{ 'assessment.sections.diet' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.dietHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.dietaryRecall24h' | translate }}</mat-label>
              <textarea matInput rows="4" [(ngModel)]="d.dietaryRecall24h"
                        [placeholder]="'assessment.f.dietaryRecall24hHint' | translate"></textarea>
            </mat-form-field>
            <div class="grid two">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.mealFrequency' | translate }}</mat-label>
                <input matInput [(ngModel)]="d.mealFrequency"
                       [placeholder]="'assessment.f.mealFrequencyHint' | translate">
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.waterIntakeLiters' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.waterIntakeLiters"><span matSuffix>L</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.alcoholFrequency' | translate }}</mat-label>
                <input matInput [(ngModel)]="d.alcoholFrequency">
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.foodPreferences' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.foodPreferences"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.foodAversions' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.foodAversions"></textarea>
            </mat-form-field>
          </mat-expansion-panel>

          <!-- 4. Estilo de vida -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>self_improvement</mat-icon>
                {{ 'assessment.sections.lifestyle' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.lifestyleHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.physicalActivityDetails' | translate }}</mat-label>
              <textarea matInput rows="3" [(ngModel)]="d.physicalActivityDetails"></textarea>
            </mat-form-field>
            <div class="grid two">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.sleepHours' | translate }}</mat-label>
                <input matInput type="number" step="0.5" [(ngModel)]="d.sleepHours"><span matSuffix>h</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.stressLevel' | translate }}</mat-label>
                <mat-select [(ngModel)]="d.stressLevel">
                  <mat-option [value]="null">-</mat-option>
                  <mat-option value="LOW">{{ 'assessment.stress.LOW' | translate }}</mat-option>
                  <mat-option value="MEDIUM">{{ 'assessment.stress.MEDIUM' | translate }}</mat-option>
                  <mat-option value="HIGH">{{ 'assessment.stress.HIGH' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            <mat-checkbox [(ngModel)]="d.smoker">{{ 'assessment.f.smoker' | translate }}</mat-checkbox>
            @if (d.smoker) {
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.smokingDetails' | translate }}</mat-label>
                <input matInput [(ngModel)]="d.smokingDetails">
              </mat-form-field>
            }
          </mat-expansion-panel>

          <!-- 5. Exames laboratoriais -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>science</mat-icon>
                {{ 'assessment.sections.labs' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.labsHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.labDate' | translate }}</mat-label>
              <input matInput type="date" [(ngModel)]="d.labDate">
            </mat-form-field>
            <h4 class="lab-group">{{ 'assessment.labs.glycemic' | translate }}</h4>
            <div class="grid">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.glucoseFasting' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.glucoseFasting"><span matSuffix>mg/dL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.hba1c' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.hba1c"><span matSuffix>%</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.insulinFasting' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.insulinFasting"><span matSuffix>μUI/mL</span>
              </mat-form-field>
            </div>
            <h4 class="lab-group">{{ 'assessment.labs.lipids' | translate }}</h4>
            <div class="grid">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.totalCholesterol' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.totalCholesterol"><span matSuffix>mg/dL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.ldl' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.ldl"><span matSuffix>mg/dL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.hdl' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.hdl"><span matSuffix>mg/dL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.triglycerides' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.triglycerides"><span matSuffix>mg/dL</span>
              </mat-form-field>
            </div>
            <h4 class="lab-group">{{ 'assessment.labs.thyroid' | translate }}</h4>
            <div class="grid">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.tsh' | translate }}</mat-label>
                <input matInput type="number" step="0.01" [(ngModel)]="d.tsh"><span matSuffix>mUI/L</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.t4Free' | translate }}</mat-label>
                <input matInput type="number" step="0.01" [(ngModel)]="d.t4Free"><span matSuffix>ng/dL</span>
              </mat-form-field>
            </div>
            <h4 class="lab-group">{{ 'assessment.labs.vitamins' | translate }}</h4>
            <div class="grid">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.vitaminD' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.vitaminD"><span matSuffix>ng/mL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.vitaminB12' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.vitaminB12"><span matSuffix>pg/mL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.iron' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.iron"><span matSuffix>μg/dL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.ferritin' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.ferritin"><span matSuffix>ng/mL</span>
              </mat-form-field>
            </div>
            <h4 class="lab-group">{{ 'assessment.labs.others' | translate }}</h4>
            <div class="grid">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.crp' | translate }}</mat-label>
                <input matInput type="number" step="0.01" [(ngModel)]="d.crp"><span matSuffix>mg/L</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.urea' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.urea"><span matSuffix>mg/dL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.creatinine' | translate }}</mat-label>
                <input matInput type="number" step="0.01" [(ngModel)]="d.creatinine"><span matSuffix>mg/dL</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.uricAcid' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.uricAcid"><span matSuffix>mg/dL</span>
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.labNotes' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="d.labNotes"></textarea>
            </mat-form-field>
          </mat-expansion-panel>

          <!-- 5c. Prescricao energetica (Fase 9C) -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>local_fire_department</mat-icon>
                {{ 'assessment.sections.prescription' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.prescriptionHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <div class="grid two">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.tmbFormula' | translate }}</mat-label>
                <mat-select [(ngModel)]="d.tmbFormula">
                  <mat-option value="MIFFLIN">{{ 'assessment.tmbFormula.MIFFLIN' | translate }}</mat-option>
                  <mat-option value="HARRIS_BENEDICT">{{ 'assessment.tmbFormula.HARRIS_BENEDICT' | translate }}</mat-option>
                  <mat-option value="KATCH_MCARDLE">{{ 'assessment.tmbFormula.KATCH_MCARDLE' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.activityFactor' | translate }}</mat-label>
                <mat-select [(ngModel)]="d.activityFactor">
                  <mat-option [value]="null">{{ 'assessment.activity.useProfile' | translate }}</mat-option>
                  <mat-option [value]="1.2">1.2 — {{ 'assessment.activity.SEDENTARY' | translate }}</mat-option>
                  <mat-option [value]="1.375">1.375 — {{ 'assessment.activity.LIGHT' | translate }}</mat-option>
                  <mat-option [value]="1.55">1.55 — {{ 'assessment.activity.MODERATE' | translate }}</mat-option>
                  <mat-option [value]="1.725">1.725 — {{ 'assessment.activity.ACTIVE' | translate }}</mat-option>
                  <mat-option [value]="1.9">1.9 — {{ 'assessment.activity.VERY_ACTIVE' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>
            </div>
            <div class="grid two">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.calorieDeltaPct' | translate }}</mat-label>
                <mat-select [(ngModel)]="d.calorieDeltaPct">
                  <mat-option [value]="-0.25">{{ 'assessment.calorieDelta.deficit25' | translate }}</mat-option>
                  <mat-option [value]="-0.20">{{ 'assessment.calorieDelta.deficit20' | translate }}</mat-option>
                  <mat-option [value]="-0.10">{{ 'assessment.calorieDelta.deficit10' | translate }}</mat-option>
                  <mat-option [value]="0">{{ 'assessment.calorieDelta.maintain' | translate }}</mat-option>
                  <mat-option [value]="0.10">{{ 'assessment.calorieDelta.surplus10' | translate }}</mat-option>
                  <mat-option [value]="0.15">{{ 'assessment.calorieDelta.surplus15' | translate }}</mat-option>
                  <mat-option [value]="0.20">{{ 'assessment.calorieDelta.surplus20' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.proteinGPerKg' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.proteinGPerKg"
                       [placeholder]="'assessment.proteinDefault' | translate">
                <span matSuffix>g/kg</span>
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.fatPct' | translate }}</mat-label>
              <input matInput type="number" step="0.01" min="0.15" max="0.45"
                     [ngModel]="d.fatPct" (ngModelChange)="d.fatPct = $event">
              <span matSuffix>({{ ((d.fatPct ?? 0.28) * 100) | number:'1.0-0' }} %)</span>
            </mat-form-field>

            <button mat-flat-button color="primary" type="button" (click)="recalculatePrescription()">
              <mat-icon>calculate</mat-icon>
              {{ 'assessment.prescription.recalc' | translate }}
            </button>

            @if (d.targetCalories != null) {
              <div class="rx-summary">
                <div class="rx-header">
                  <mat-icon>bolt</mat-icon>
                  <strong>{{ d.targetCalories | number:'1.0-0' }} kcal/dia</strong>
                  @if (d.prescribedAt) {
                    <span class="text-muted small">· {{ d.prescribedAt | date:'shortDate' }}</span>
                  }
                </div>
                <div class="rx-macros">
                  <span class="macro p"><b>P</b> {{ d.targetProteinG | number:'1.0-1' }} g</span>
                  <span class="macro c"><b>C</b> {{ d.targetCarbsG | number:'1.0-1' }} g</span>
                  <span class="macro f"><b>G</b> {{ d.targetFatG | number:'1.0-1' }} g</span>
                  <span class="macro w"><b>Água</b> {{ (d.targetWaterMl ?? 0) / 1000 | number:'1.1-1' }} L</span>
                </div>
              </div>
              <button mat-stroked-button type="button" (click)="downloadPdf()" [disabled]="downloadingPdf()">
                <mat-icon>picture_as_pdf</mat-icon>
                {{ (downloadingPdf() ? 'assessment.pdf.downloading' : 'assessment.pdf.download') | translate }}
              </button>
            }
          </mat-expansion-panel>

          <!-- 5d. Notas privadas do nutri (Fase 9E) -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>lock</mat-icon>
                {{ 'assessment.sections.nutriNotes' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.nutriNotesHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.nutriNotes' | translate }}</mat-label>
              <textarea matInput rows="5" [(ngModel)]="d.nutriNotes"
                        [placeholder]="'assessment.f.nutriNotesPlaceholder' | translate"></textarea>
              <mat-hint>{{ 'assessment.sections.nutriNotesHint' | translate }}</mat-hint>
            </mat-form-field>
          </mat-expansion-panel>

          <!-- 6. Objetivos -->
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon>flag</mat-icon>
                {{ 'assessment.sections.goals' | translate }}
              </mat-panel-title>
              <mat-panel-description>{{ 'assessment.sections.goalsHint' | translate }}</mat-panel-description>
            </mat-expansion-panel-header>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.goalNotes' | translate }}</mat-label>
              <textarea matInput rows="3" [(ngModel)]="d.goalNotes"></textarea>
            </mat-form-field>
            <div class="grid two">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.targetWeightKg' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="d.targetWeightKg"><span matSuffix>kg</span>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'assessment.f.targetDate' | translate }}</mat-label>
                <input matInput type="date" [(ngModel)]="d.targetDate">
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.notes' | translate }}</mat-label>
              <textarea matInput rows="3" [(ngModel)]="d.notes"
                        [placeholder]="'assessment.f.notesHint' | translate"></textarea>
            </mat-form-field>
          </mat-expansion-panel>
        </mat-accordion>

        <app-assessment-trend-chart
          [snapshots]="snapshots()"
          [current]="draft()"
          [birthDate]="clientBirthDate()"
          [sex]="clientSex()"></app-assessment-trend-chart>

        <!-- Fase 9E: prontuário — timeline unificada de consultas + snapshots -->
        <mat-card class="chart-card">
          <div class="chart-head">
            <mat-icon>history_edu</mat-icon>
            <h3>{{ 'assessment.chart.title' | translate }}</h3>
            <span class="spacer"></span>
            <button mat-flat-button color="primary" (click)="openNewConsultation()">
              <mat-icon>add</mat-icon>
              {{ 'assessment.chart.new' | translate }}
            </button>
          </div>
          @if (timeline().length === 0) {
            <p class="text-muted small chart-empty">{{ 'assessment.chart.empty' | translate }}</p>
          } @else {
            <ul class="chart-list">
              @for (ev of timeline(); track ev.key) {
                <li [class]="'ev-' + ev.kind">
                  <span class="ev-icon">
                    <mat-icon>{{ ev.icon }}</mat-icon>
                  </span>
                  <div class="ev-body">
                    <div class="ev-title">
                      <strong>{{ ev.title }}</strong>
                      <span class="text-muted small">· {{ ev.date | date:'short' }}</span>
                    </div>
                    @if (ev.subtitle) {
                      <div class="text-muted small">{{ ev.subtitle }}</div>
                    }
                  </div>
                </li>
              }
            </ul>
          }
        </mat-card>

        @if (snapshots().length > 0) {
          <mat-card class="snapshots-card">
            <div class="snapshots-header">
              <mat-icon>history</mat-icon>
              <h3>{{ 'assessment.snapshots.title' | translate }}</h3>
              <span class="text-muted small">
                {{ 'assessment.snapshots.count' | translate:{ n: snapshots().length } }}
              </span>
            </div>
            <ul class="snapshots-list">
              @for (s of snapshots(); track s.id) {
                <li>
                  <span class="badge">v{{ s.version }}</span>
                  <span class="snap-label">{{ s.label || ('assessment.snapshots.untitled' | translate) }}</span>
                  <span class="text-muted small">{{ s.createdAt | date:'short' }}</span>
                  <span class="text-muted small snap-author">· {{ s.createdByName }}</span>
                  @if (s.payload?.weightKg != null) {
                    <span class="snap-metric">{{ s.payload.weightKg }} kg</span>
                  }
                </li>
              }
            </ul>
          </mat-card>
        }

        <div class="footer">
          @if (d.updatedAt) {
            <span class="text-muted small">
              {{ 'assessment.lastUpdate' | translate }}: {{ d.updatedAt | date:'short' }}
            </span>
          }
          <span class="spacer"></span>
          @if (d.id) {
            <button mat-stroked-button (click)="takeSnapshot()" [disabled]="snapshotting()"
                    [matTooltip]="'assessment.snapshots.tooltip' | translate">
              <mat-icon>history_edu</mat-icon>
              {{ (snapshotting() ? 'assessment.snapshots.saving' : 'assessment.snapshots.new') | translate }}
            </button>
          }
          <button mat-flat-button color="primary" (click)="save()" [disabled]="saving()">
            <mat-icon>save</mat-icon>
            {{ (saving() ? 'assessment.saving' : 'assessment.save') | translate }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    mat-accordion { display: block; margin-top: 12px; }
    mat-expansion-panel { margin-bottom: 8px; }
    mat-expansion-panel-header mat-panel-title { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); } }
    mat-form-field { display: block; margin-bottom: 10px; }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 10px;
    }
    .grid.two { grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); }
    .footer {
      display: flex; align-items: center; gap: 12px;
      margin-top: 20px; padding: 16px;
      background: var(--nt-surface); border-radius: var(--nt-radius);
      box-shadow: var(--nt-shadow);
      position: sticky; bottom: 0;
    }
    .spacer { flex: 1; }
    .small { font-size: 0.85rem; }
    mat-checkbox { margin-bottom: 10px; display: block; }

    .lab-group {
      font-size: 0.78rem;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--nt-text-muted);
      margin: 14px 0 6px;
      font-weight: 600;
    }
    .lab-group:first-child { margin-top: 0; }

    .derived-strip {
      display: flex; flex-wrap: wrap; gap: 8px;
      margin-top: 14px;
    }
    .pill {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 6px 12px;
      background: var(--nt-primary-soft);
      border-radius: 999px;
      font-size: 0.85rem;
    }
    .pill.risk-mod { background: var(--nt-accent-soft); color: var(--nt-accent); }
    .pill.risk-hi  { background: var(--nt-danger-soft); color: var(--nt-danger); }
    .p-label { font-weight: 600; text-transform: uppercase; font-size: 0.7rem; letter-spacing: 0.04em; opacity: 0.75; }
    .p-val   { font-weight: 700; color: var(--nt-primary-dark); }
    .pill.risk-mod .p-val, .pill.risk-hi .p-val { color: inherit; }
    .p-note  { opacity: 0.75; font-size: 0.8rem; }

    .rx-summary {
      margin-top: 14px; padding: 14px 16px;
      background: var(--nt-primary-soft);
      border-radius: 12px;
      display: flex; flex-direction: column; gap: 10px;
    }
    .rx-header { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary-dark); }
      strong { font-size: 1.15rem; color: var(--nt-primary-dark); }
    }
    .rx-macros { display: flex; flex-wrap: wrap; gap: 8px; }
    .rx-macros .macro {
      padding: 4px 12px; border-radius: 999px;
      background: var(--nt-surface);
      font-size: 0.85rem;
      b { color: var(--nt-primary-dark); margin-right: 4px; }
    }
    .rx-macros .macro.p b { color: #2563eb; }
    .rx-macros .macro.c b { color: #d97706; }
    .rx-macros .macro.f b { color: #dc2626; }
    .rx-macros .macro.w b { color: #06b6d4; }

    .snapshots-card {
      margin-top: 20px;
      padding: 16px !important;
      display: flex; flex-direction: column; gap: 12px;
    }
    .snapshots-header {
      display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
      h3 { margin: 0; font-size: 1rem; }
      .small { margin-left: auto; }
    }
    .snapshots-list {
      list-style: none; margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: 6px;
      li {
        display: flex; align-items: center; gap: 10px;
        padding: 8px 12px;
        background: var(--nt-surface-2);
        border-radius: 10px;
        font-size: 0.9rem;
      }
    }
    .badge {
      background: var(--nt-primary-soft);
      color: var(--nt-primary-dark);
      padding: 2px 8px; border-radius: 999px;
      font-size: 0.75rem; font-weight: 600;
    }
    .snap-label { font-weight: 500; }
    .snap-author { }
    .snap-metric {
      margin-left: auto;
      background: var(--nt-surface);
      padding: 2px 8px; border-radius: 8px;
      font-weight: 600; color: var(--nt-primary-dark);
    }

    .chart-card {
      margin-top: 20px; padding: 16px !important;
      display: flex; flex-direction: column; gap: 14px;
    }
    .chart-head {
      display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
      h3 { margin: 0; font-size: 1.05rem; }
      .spacer { flex: 1; }
    }
    .chart-empty { padding: 24px 0; text-align: center; margin: 0; }
    .chart-list {
      list-style: none; margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: 4px;
      li {
        display: flex; align-items: center; gap: 12px;
        padding: 10px 12px;
        border-radius: 10px;
        border-left: 3px solid var(--nt-border);
      }
      li.ev-consultation { background: var(--nt-primary-soft); border-left-color: var(--nt-primary); }
      li.ev-snapshot     { background: var(--nt-surface-2); border-left-color: var(--nt-accent); }
    }
    .ev-icon {
      display: inline-flex; align-items: center; justify-content: center;
      width: 32px; height: 32px;
      background: var(--nt-surface); border-radius: 8px;
      mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--nt-primary-dark); }
    }
    .ev-body { flex: 1; min-width: 0; }
    .ev-title { display: flex; align-items: baseline; gap: 6px; flex-wrap: wrap; }
  `]
})
export class NutriAssessmentComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private practiceCtx = inject(PracticeService);

  clientId!: number;
  draft = signal<ClientAssessment | null>(null);
  clientName = signal<string | null>(null);
  clientBirthDate = signal<string | null>(null);
  clientSex = signal<string | null>(null);
  saving = signal(false);
  snapshots = signal<AssessmentSnapshot[]>([]);
  consultations = signal<Consultation[]>([]);
  snapshotting = signal(false);
  downloadingPdf = signal(false);
  timeline = computed(() => {
    // Une consultas + snapshots ordenados por data desc pra montar timeline visual
    const events: Array<{ key: string; kind: 'consultation' | 'snapshot'; date: string;
                          icon: string; title: string; subtitle?: string }> = [];
    for (const c of this.consultations()) {
      const typeLabel = this.translate.instant('assessment.chart.type.' + c.type);
      events.push({
        key: 'c-' + c.id,
        kind: 'consultation',
        date: c.consultationDate,
        icon: 'event_note',
        title: typeLabel,
        subtitle: c.chiefComplaint || undefined
      });
    }
    for (const s of this.snapshots()) {
      events.push({
        key: 's-' + s.id,
        kind: 'snapshot',
        date: s.createdAt,
        icon: 'photo_camera',
        title: s.label || `Snapshot v${s.version}`,
        subtitle: s.payload?.weightKg != null ? `${s.payload.weightKg} kg` : undefined
      });
    }
    return events.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  });
  derived = computed(() => {
    const d = this.draft();
    if (!d) return null;
    return deriveMetrics(d, this.clientBirthDate(), this.clientSex() as any);
  });

  private firstLoad = true;
  constructor() {
    effect(() => {
      this.practiceCtx.activePracticeId();
      if (this.firstLoad) { this.firstLoad = false; return; }
      this.load();
    }, { allowSignalWrites: true });
  }

  ngOnInit() {
    this.clientId = Number(this.route.snapshot.paramMap.get('clientId'));
    this.load();
  }

  private load() {
    this.api.nutriGetAssessment(this.clientId, this.practiceCtx.activePracticeId()).subscribe({
      next: existing => {
        if (existing) {
          this.draft.set(existing);
          this.clientName.set(existing.clientName ?? null);
          this.clientSex.set(existing.clientSex ?? null);
          this.clientBirthDate.set(existing.clientBirthDate ?? null);
          this.loadSnapshots();
        } else {
          this.draft.set(this.blank());
          this.snapshots.set([]);
          this.loadClientName();
        }
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('assessment.loadErr'),
                                    'OK', { duration: 3000 })
    });
  }

  private loadSnapshots() {
    this.api.nutriListAssessmentSnapshots(this.clientId, this.practiceCtx.activePracticeId())
      .subscribe({ next: list => this.snapshots.set(list) });
    this.api.nutriListConsultations(this.clientId, this.practiceCtx.activePracticeId())
      .subscribe({ next: list => this.consultations.set(list) });
  }

  /** Cria consulta rapida (mesmo dia, tipo padrao RETURN, com auto-snapshot). */
  openNewConsultation() {
    const complaint = window.prompt(
      this.translate.instant('assessment.chart.promptComplaint'),
      '');
    if (complaint === null) return;
    const dto: Consultation = {
      consultationDate: new Date().toISOString(),
      type: 'RETURN' as ConsultationType,
      chiefComplaint: complaint || null
    };
    this.api.nutriCreateConsultation(this.clientId, dto,
      { snapshot: true, practiceId: this.practiceCtx.activePracticeId() }).subscribe({
        next: () => {
          this.snack.open(this.translate.instant('assessment.chart.created'),
                          'OK', { duration: 2500 });
          this.loadSnapshots();
        },
        error: err => this.snack.open(err?.error?.error ??
                          this.translate.instant('assessment.chart.err'),
                          'OK', { duration: 3000 })
      });
  }

  recalculatePrescription() {
    const d = this.draft();
    if (!d) return;
    const rx = derivePrescription(d, this.clientBirthDate(), this.clientSex(), null);
    if (!rx) {
      this.snack.open(this.translate.instant('assessment.prescription.needData'),
                      'OK', { duration: 3500 });
      return;
    }
    d.targetCalories = rx.calories;
    d.targetProteinG = rx.proteinG;
    d.targetCarbsG = rx.carbsG;
    d.targetFatG = rx.fatG;
    d.targetWaterMl = rx.waterMl;
    d.prescribedAt = new Date().toISOString();
    this.draft.set({ ...d }); // trigger signal
  }

  downloadPdf() {
    this.downloadingPdf.set(true);
    this.api.nutriPrescriptionPdf(this.clientId, this.practiceCtx.activePracticeId()).subscribe({
      next: blob => {
        this.downloadingPdf.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `prescricao-${this.clientId}.pdf`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: () => {
        this.downloadingPdf.set(false);
        this.snack.open(this.translate.instant('assessment.pdf.err'), 'OK', { duration: 3000 });
      }
    });
  }

  takeSnapshot() {
    const label = window.prompt(this.translate.instant('assessment.snapshots.labelPrompt'),
                                 this.translate.instant('assessment.snapshots.defaultLabel'));
    if (label === null) return; // usuario cancelou
    this.snapshotting.set(true);
    this.api.nutriCreateAssessmentSnapshot(this.clientId, label || null,
                                             this.practiceCtx.activePracticeId()).subscribe({
      next: () => {
        this.snapshotting.set(false);
        this.snack.open(this.translate.instant('assessment.snapshots.saved'), 'OK', { duration: 2500 });
        this.loadSnapshots();
      },
      error: err => {
        this.snapshotting.set(false);
        this.snack.open(err?.error?.error ?? this.translate.instant('assessment.snapshots.err'),
                        'OK', { duration: 3000 });
      }
    });
  }

  private loadClientName() {
    this.api.nutriListClients(this.practiceCtx.activePracticeId(), '', 'name', 0, 500).subscribe(p => {
      const match = p.items.find((c: ClientLink) => c.clientId === this.clientId);
      if (match) this.clientName.set(match.clientName + ' · ' + match.clientEmail);
    });
  }

  private blank(): ClientAssessment {
    return {
      assessmentDate: new Date().toISOString(),
      smoker: false
    };
  }

  save() {
    const d = this.draft();
    if (!d) return;
    this.saving.set(true);
    this.api.nutriUpsertAssessment(this.clientId, d, this.practiceCtx.activePracticeId()).subscribe({
      next: saved => {
        this.saving.set(false);
        this.draft.set(saved);
        this.snack.open(this.translate.instant('assessment.saved'), 'OK', { duration: 2500 });
      },
      error: err => {
        this.saving.set(false);
        this.snack.open(err?.error?.error ?? this.translate.instant('assessment.saveErr'),
                        'OK', { duration: 3000 });
      }
    });
  }

  back() { this.router.navigateByUrl('/nutritionist/clients'); }
}
