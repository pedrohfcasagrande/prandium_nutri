import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { AssessmentSelfFill, AssessmentSnapshot, ClientAssessment, Consultation, PrescriptionIssuance } from '../../models/models';
import { EmptyStateComponent } from '../../components/empty-state.component';
import { deriveMetrics } from '../../util/assessment-metrics';
import { AssessmentTrendChartComponent } from '../../components/assessment-trend-chart.component';

@Component({
  selector: 'app-my-assessment',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule, MatExpansionModule,
    MatSnackBarModule, TranslateModule, EmptyStateComponent, AssessmentTrendChartComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'myAssessment.title' | translate }}</h1>
          <p>{{ 'myAssessment.subtitle' | translate }}</p>
        </div>
      </div>

      @if (!assessment()) {
        <app-empty-state type="assessment"
                         [description]="'myAssessment.empty' | translate"></app-empty-state>
      } @else {
        @let a = assessment()!;

        <div class="meta card-surface">
          <mat-icon>medical_services</mat-icon>
          <div>
            <div><strong>{{ a.nutritionistName }}</strong></div>
            <div class="text-muted small">
              {{ 'myAssessment.updatedAt' | translate }}: {{ a.updatedAt | date:'short' }}
            </div>
          </div>
          <span class="spacer"></span>
          @if (a.targetCalories != null) {
            <button mat-flat-button color="primary" (click)="downloadPdf()" [disabled]="downloadingPdf()">
              <mat-icon>picture_as_pdf</mat-icon>
              {{ (downloadingPdf() ? 'assessment.pdf.downloading' : 'myAssessment.downloadPdf') | translate }}
            </button>
          }
        </div>

        <mat-card class="section" *ngIf="hasAnthropometry(a)">
          <div class="section-head">
            <mat-icon>straighten</mat-icon>
            <h3>{{ 'assessment.sections.anthropometry' | translate }}</h3>
          </div>
          <div class="kv-grid">
            @if (a.weightKg != null) { <div><span class="k">{{ 'assessment.f.weightKg' | translate }}</span><span class="v">{{ a.weightKg }} kg</span></div> }
            @if (a.heightCm != null) { <div><span class="k">{{ 'assessment.f.heightCm' | translate }}</span><span class="v">{{ a.heightCm }} cm</span></div> }
            @if (a.bodyFatPct != null) { <div><span class="k">{{ 'assessment.f.bodyFatPct' | translate }}</span><span class="v">{{ a.bodyFatPct }} %</span></div> }
            @if (a.waistCm != null) { <div><span class="k">{{ 'assessment.f.waistCm' | translate }}</span><span class="v">{{ a.waistCm }} cm</span></div> }
            @if (a.hipCm != null) { <div><span class="k">{{ 'assessment.f.hipCm' | translate }}</span><span class="v">{{ a.hipCm }} cm</span></div> }
            @if (a.neckCm != null) { <div><span class="k">{{ 'assessment.f.neckCm' | translate }}</span><span class="v">{{ a.neckCm }} cm</span></div> }
            @if (a.armCm != null) { <div><span class="k">{{ 'assessment.f.armCm' | translate }}</span><span class="v">{{ a.armCm }} cm</span></div> }
            @if (a.thighCm != null) { <div><span class="k">{{ 'assessment.f.thighCm' | translate }}</span><span class="v">{{ a.thighCm }} cm</span></div> }
            @if (a.calfCm != null) { <div><span class="k">{{ 'assessment.f.calfCm' | translate }}</span><span class="v">{{ a.calfCm }} cm</span></div> }
            @if (a.chestCm != null) { <div><span class="k">{{ 'assessment.f.chestCm' | translate }}</span><span class="v">{{ a.chestCm }} cm</span></div> }
            @if (a.forearmCm != null) { <div><span class="k">{{ 'assessment.f.forearmCm' | translate }}</span><span class="v">{{ a.forearmCm }} cm</span></div> }
            @if (a.abdomenCm != null) { <div><span class="k">{{ 'assessment.f.abdomenCm' | translate }}</span><span class="v">{{ a.abdomenCm }} cm</span></div> }
          </div>
          @if (derived(); as m) {
            <div class="derived-strip">
              @if (m.bmi != null) {
                <div class="pill"><span class="p-label">IMC</span><span class="p-val">{{ m.bmi }}</span>
                  <span class="p-note">{{ ('assessment.bmi.' + m.bmiCategory) | translate }}</span></div>
              }
              @if (m.whr != null) {
                <div class="pill" [class.risk-mod]="m.whrRisk === 'moderate'" [class.risk-hi]="m.whrRisk === 'high'">
                  <span class="p-label">RCQ</span><span class="p-val">{{ m.whr }}</span>
                  @if (m.whrRisk) { <span class="p-note">{{ ('assessment.whr.' + m.whrRisk) | translate }}</span> }
                </div>
              }
              @if (m.bodyFatPctCalc != null) {
                <div class="pill"><span class="p-label">% G (JP-7)</span><span class="p-val">{{ m.bodyFatPctCalc }}%</span></div>
              }
              @if (m.fatMassKg != null) {
                <div class="pill"><span class="p-label">MG</span><span class="p-val">{{ m.fatMassKg }} kg</span></div>
              }
              @if (m.leanMassKg != null) {
                <div class="pill"><span class="p-label">MM</span><span class="p-val">{{ m.leanMassKg }} kg</span></div>
              }
            </div>
          }
        </mat-card>

        <mat-card class="section" *ngIf="hasSkinfolds(a)">
          <div class="section-head">
            <mat-icon>compress</mat-icon>
            <h3>{{ 'assessment.sections.skinfolds' | translate }}</h3>
            @if (a.skinfoldProtocol) {
              <span class="text-muted small">{{ ('assessment.skinfoldProtocol.' + a.skinfoldProtocol) | translate }}</span>
            }
          </div>
          <div class="kv-grid">
            @if (a.sfTricipital != null) { <div><span class="k">{{ 'assessment.f.sfTricipital' | translate }}</span><span class="v">{{ a.sfTricipital }} mm</span></div> }
            @if (a.sfSubscapular != null) { <div><span class="k">{{ 'assessment.f.sfSubscapular' | translate }}</span><span class="v">{{ a.sfSubscapular }} mm</span></div> }
            @if (a.sfSuprailiac != null) { <div><span class="k">{{ 'assessment.f.sfSuprailiac' | translate }}</span><span class="v">{{ a.sfSuprailiac }} mm</span></div> }
            @if (a.sfAbdominal != null) { <div><span class="k">{{ 'assessment.f.sfAbdominal' | translate }}</span><span class="v">{{ a.sfAbdominal }} mm</span></div> }
            @if (a.sfThigh != null) { <div><span class="k">{{ 'assessment.f.sfThigh' | translate }}</span><span class="v">{{ a.sfThigh }} mm</span></div> }
            @if (a.sfChest != null) { <div><span class="k">{{ 'assessment.f.sfChest' | translate }}</span><span class="v">{{ a.sfChest }} mm</span></div> }
            @if (a.sfMidaxillary != null) { <div><span class="k">{{ 'assessment.f.sfMidaxillary' | translate }}</span><span class="v">{{ a.sfMidaxillary }} mm</span></div> }
          </div>
        </mat-card>

        <mat-card class="section" *ngIf="hasClinical(a)">
          <div class="section-head">
            <mat-icon>medical_information</mat-icon>
            <h3>{{ 'assessment.sections.clinical' | translate }}</h3>
          </div>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.chiefComplaint', value: a.chiefComplaint }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.medicalHistory', value: a.medicalHistory }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.medications', value: a.medications }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.preExistingConditions', value: a.preExistingConditions }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.allergies', value: a.allergies }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.familyHistory', value: a.familyHistory }"/>
        </mat-card>

        <mat-card class="section" *ngIf="hasDiet(a)">
          <div class="section-head">
            <mat-icon>restaurant</mat-icon>
            <h3>{{ 'assessment.sections.diet' | translate }}</h3>
          </div>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.dietaryRecall24h', value: a.dietaryRecall24h }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.mealFrequency', value: a.mealFrequency }"/>
          @if (a.waterIntakeLiters != null) {
            <div class="field"><span class="k">{{ 'assessment.f.waterIntakeLiters' | translate }}</span><span class="v">{{ a.waterIntakeLiters }} L/dia</span></div>
          }
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.alcoholFrequency', value: a.alcoholFrequency }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.foodPreferences', value: a.foodPreferences }"/>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.foodAversions', value: a.foodAversions }"/>
        </mat-card>

        <mat-card class="section" *ngIf="hasLifestyle(a)">
          <div class="section-head">
            <mat-icon>self_improvement</mat-icon>
            <h3>{{ 'assessment.sections.lifestyle' | translate }}</h3>
          </div>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.physicalActivityDetails', value: a.physicalActivityDetails }"/>
          @if (a.sleepHours != null) {
            <div class="field"><span class="k">{{ 'assessment.f.sleepHours' | translate }}</span><span class="v">{{ a.sleepHours }} h</span></div>
          }
          @if (a.stressLevel) {
            <div class="field"><span class="k">{{ 'assessment.f.stressLevel' | translate }}</span><span class="v">{{ ('assessment.stress.' + a.stressLevel) | translate }}</span></div>
          }
          <div class="field"><span class="k">{{ 'assessment.f.smoker' | translate }}</span><span class="v">{{ (a.smoker ? 'app.actions.yes' : 'app.actions.no') | translate }}</span></div>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.smokingDetails', value: a.smokingDetails }"/>
        </mat-card>

        <mat-card class="section" *ngIf="hasGoals(a)">
          <div class="section-head">
            <mat-icon>flag</mat-icon>
            <h3>{{ 'assessment.sections.goals' | translate }}</h3>
          </div>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.goalNotes', value: a.goalNotes }"/>
          @if (a.targetWeightKg != null) {
            <div class="field"><span class="k">{{ 'assessment.f.targetWeightKg' | translate }}</span><span class="v">{{ a.targetWeightKg }} kg</span></div>
          }
          @if (a.targetDate) {
            <div class="field"><span class="k">{{ 'assessment.f.targetDate' | translate }}</span><span class="v">{{ a.targetDate | date:'shortDate' }}</span></div>
          }
        </mat-card>

        <mat-card class="section" *ngIf="hasLabs(a)">
          <div class="section-head">
            <mat-icon>science</mat-icon>
            <h3>{{ 'assessment.sections.labs' | translate }}</h3>
            @if (a.labDate) {
              <span class="text-muted small">{{ a.labDate | date:'shortDate' }}</span>
            }
          </div>
          <div class="kv-grid">
            @if (a.glucoseFasting != null) { <div><span class="k">{{ 'assessment.f.glucoseFasting' | translate }}</span><span class="v">{{ a.glucoseFasting }} mg/dL</span></div> }
            @if (a.hba1c != null) { <div><span class="k">{{ 'assessment.f.hba1c' | translate }}</span><span class="v">{{ a.hba1c }} %</span></div> }
            @if (a.insulinFasting != null) { <div><span class="k">{{ 'assessment.f.insulinFasting' | translate }}</span><span class="v">{{ a.insulinFasting }}</span></div> }
            @if (a.totalCholesterol != null) { <div><span class="k">{{ 'assessment.f.totalCholesterol' | translate }}</span><span class="v">{{ a.totalCholesterol }} mg/dL</span></div> }
            @if (a.ldl != null) { <div><span class="k">{{ 'assessment.f.ldl' | translate }}</span><span class="v">{{ a.ldl }} mg/dL</span></div> }
            @if (a.hdl != null) { <div><span class="k">{{ 'assessment.f.hdl' | translate }}</span><span class="v">{{ a.hdl }} mg/dL</span></div> }
            @if (a.triglycerides != null) { <div><span class="k">{{ 'assessment.f.triglycerides' | translate }}</span><span class="v">{{ a.triglycerides }} mg/dL</span></div> }
            @if (a.tsh != null) { <div><span class="k">{{ 'assessment.f.tsh' | translate }}</span><span class="v">{{ a.tsh }} mUI/L</span></div> }
            @if (a.t4Free != null) { <div><span class="k">{{ 'assessment.f.t4Free' | translate }}</span><span class="v">{{ a.t4Free }} ng/dL</span></div> }
            @if (a.vitaminD != null) { <div><span class="k">{{ 'assessment.f.vitaminD' | translate }}</span><span class="v">{{ a.vitaminD }} ng/mL</span></div> }
            @if (a.vitaminB12 != null) { <div><span class="k">{{ 'assessment.f.vitaminB12' | translate }}</span><span class="v">{{ a.vitaminB12 }} pg/mL</span></div> }
            @if (a.iron != null) { <div><span class="k">{{ 'assessment.f.iron' | translate }}</span><span class="v">{{ a.iron }} μg/dL</span></div> }
            @if (a.ferritin != null) { <div><span class="k">{{ 'assessment.f.ferritin' | translate }}</span><span class="v">{{ a.ferritin }} ng/mL</span></div> }
            @if (a.crp != null) { <div><span class="k">{{ 'assessment.f.crp' | translate }}</span><span class="v">{{ a.crp }} mg/L</span></div> }
            @if (a.urea != null) { <div><span class="k">{{ 'assessment.f.urea' | translate }}</span><span class="v">{{ a.urea }} mg/dL</span></div> }
            @if (a.creatinine != null) { <div><span class="k">{{ 'assessment.f.creatinine' | translate }}</span><span class="v">{{ a.creatinine }} mg/dL</span></div> }
            @if (a.uricAcid != null) { <div><span class="k">{{ 'assessment.f.uricAcid' | translate }}</span><span class="v">{{ a.uricAcid }} mg/dL</span></div> }
          </div>
          <ng-container *ngTemplateOutlet="fieldT; context: { label: 'assessment.f.labNotes', value: a.labNotes }"/>
        </mat-card>

        @if (a.notes) {
          <mat-card class="section">
            <div class="section-head">
              <mat-icon>notes</mat-icon>
              <h3>{{ 'assessment.f.notes' | translate }}</h3>
            </div>
            <p class="notes-body">{{ a.notes }}</p>
          </mat-card>
        }

        <!-- Self-fill: cliente atualiza campos safe -->
        <mat-expansion-panel class="self-fill">
          <mat-expansion-panel-header>
            <mat-panel-title>
              <mat-icon>edit_note</mat-icon>
              {{ 'myAssessment.selfFill.title' | translate }}
            </mat-panel-title>
            <mat-panel-description>{{ 'myAssessment.selfFill.hint' | translate }}</mat-panel-description>
          </mat-expansion-panel-header>
          <div class="self-fill-form">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.foodPreferences' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="selfDraft.foodPreferences"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.foodAversions' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="selfDraft.foodAversions"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.dietaryRecall24h' | translate }}</mat-label>
              <textarea matInput rows="3" [(ngModel)]="selfDraft.dietaryRecall24h"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.mealFrequency' | translate }}</mat-label>
              <input matInput [(ngModel)]="selfDraft.mealFrequency">
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.waterIntakeLiters' | translate }}</mat-label>
              <input matInput type="number" step="0.1" [(ngModel)]="selfDraft.waterIntakeLiters">
              <span matSuffix>L</span>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.physicalActivityDetails' | translate }}</mat-label>
              <textarea matInput rows="3" [(ngModel)]="selfDraft.physicalActivityDetails"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.sleepHours' | translate }}</mat-label>
              <input matInput type="number" step="0.5" [(ngModel)]="selfDraft.sleepHours">
              <span matSuffix>h</span>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'assessment.f.goalNotes' | translate }}</mat-label>
              <textarea matInput rows="3" [(ngModel)]="selfDraft.goalNotes"></textarea>
            </mat-form-field>
            <div class="self-fill-actions">
              <button mat-flat-button color="primary" (click)="saveSelfFill()" [disabled]="savingSelf()">
                <mat-icon>save</mat-icon>
                {{ (savingSelf() ? 'myAssessment.selfFill.saving' : 'myAssessment.selfFill.save') | translate }}
              </button>
            </div>
          </div>
        </mat-expansion-panel>

        <!-- Gráfico de evolução (Fase 9B) -->
        <app-assessment-trend-chart
          [snapshots]="snapshots()"
          [current]="a"
          [birthDate]="a.clientBirthDate ?? null"
          [sex]="a.clientSex ?? null"></app-assessment-trend-chart>

        <!-- Fase 9E: consultas passadas -->
        @if (consultations().length > 0) {
          <mat-card class="section">
            <div class="section-head">
              <mat-icon>event_note</mat-icon>
              <h3>{{ 'myAssessment.consultations.title' | translate }}</h3>
              <span class="text-muted small">{{ consultations().length }}</span>
            </div>
            <ul class="cons-list">
              @for (c of consultations(); track c.id) {
                <li>
                  <span class="cons-type">{{ ('assessment.chart.type.' + c.type) | translate }}</span>
                  <span class="text-muted small">{{ c.consultationDate | date:'shortDate' }}</span>
                  @if (c.chiefComplaint) {
                    <span class="cons-complaint">· {{ c.chiefComplaint }}</span>
                  }
                </li>
              }
            </ul>
          </mat-card>
        }

        <!-- Timeline de snapshots -->
        @if (snapshots().length > 0) {
          <mat-card class="section snapshots-card">
            <div class="section-head">
              <mat-icon>history</mat-icon>
              <h3>{{ 'myAssessment.history.title' | translate }}</h3>
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
                  @if (s.payload?.weightKg != null) {
                    <span class="snap-metric">{{ s.payload.weightKg }} kg</span>
                  }
                </li>
              }
            </ul>
          </mat-card>
        }

        @if (issuances().length > 0) {
          <mat-card class="section issuances-card">
            <div class="section-head">
              <mat-icon>verified</mat-icon>
              <h3>{{ 'assessment.issuances.title' | translate }}</h3>
              <span class="text-muted small">{{ issuances().length }}</span>
            </div>
            <ul class="issuances-list">
              @for (i of issuances(); track i.id) {
                <li>
                  <span class="badge role-{{ i.requesterRole.toLowerCase() }}">
                    {{ ('assessment.issuances.by.' + i.requesterRole) | translate }}
                  </span>
                  <span>{{ i.issuedAt | date:'short' }}</span>
                  <span class="spacer"></span>
                  <code class="hash">{{ i.pdfSha256.slice(0, 12) }}…</code>
                </li>
              }
            </ul>
          </mat-card>
        }

        <ng-template #fieldT let-label="label" let-value="value">
          @if (value) {
            <div class="field">
              <span class="k">{{ label | translate }}</span>
              <span class="v pre">{{ value }}</span>
            </div>
          }
        </ng-template>
      }
    </div>
  `,
  styles: [`
    .meta { display: flex; align-items: center; gap: 12px; padding: 14px 18px !important;
      mat-icon { color: var(--nt-primary); font-size: 32px; width: 32px; height: 32px; }
    }
    .spacer { flex: 1; }
    .small { font-size: 0.85rem; }
    .section { padding: 20px !important; margin-bottom: 12px; }
    .section-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px;
      mat-icon { color: var(--nt-primary); }
      h3 { margin: 0; font-size: 1.05rem; }
    }
    .kv-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px;
      > div { display: flex; flex-direction: column; gap: 2px;
        background: var(--nt-surface-2); border-radius: 8px; padding: 8px 12px;
      }
    }
    .field { display: flex; flex-direction: column; gap: 2px; margin-bottom: 12px; }
    .k { font-size: 0.75rem; color: var(--nt-text-muted); text-transform: uppercase; letter-spacing: 0.03em; }
    .v { font-weight: 500; color: var(--nt-text); }
    .v.pre { white-space: pre-wrap; }
    .notes-body { white-space: pre-wrap; margin: 0; }

    .derived-strip { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
    .pill {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 6px 12px; background: var(--nt-primary-soft);
      border-radius: 999px; font-size: 0.85rem;
    }
    .pill.risk-mod { background: var(--nt-accent-soft); color: var(--nt-accent); }
    .pill.risk-hi  { background: var(--nt-danger-soft); color: var(--nt-danger); }
    .p-label { font-weight: 600; text-transform: uppercase; font-size: 0.7rem; letter-spacing: 0.04em; opacity: 0.75; }
    .p-val   { font-weight: 700; color: var(--nt-primary-dark); }
    .pill.risk-mod .p-val, .pill.risk-hi .p-val { color: inherit; }
    .p-note  { opacity: 0.75; font-size: 0.8rem; }

    .self-fill { margin-top: 16px; }
    .self-fill mat-panel-title { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); } }
    .self-fill-form { display: flex; flex-direction: column; gap: 12px; padding-top: 12px; }
    .self-fill-form mat-form-field { display: block; }
    .self-fill-actions { display: flex; justify-content: flex-end; padding-top: 4px; }

    .snapshots-card { margin-top: 16px; }
    .snapshots-list {
      list-style: none; margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: 6px;
      li {
        display: flex; align-items: center; gap: 10px;
        padding: 8px 12px; background: var(--nt-surface-2);
        border-radius: 10px; font-size: 0.9rem;
      }
    }
    .badge {
      background: var(--nt-primary-soft); color: var(--nt-primary-dark);
      padding: 2px 8px; border-radius: 999px;
      font-size: 0.75rem; font-weight: 600;
    }
    .snap-label { font-weight: 500; }
    .snap-metric {
      margin-left: auto;
      background: var(--nt-surface);
      padding: 2px 8px; border-radius: 8px;
      font-weight: 600; color: var(--nt-primary-dark);
    }

    .cons-list {
      list-style: none; margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: 6px;
      li {
        display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
        padding: 8px 12px; background: var(--nt-surface-2);
        border-radius: 10px; font-size: 0.9rem;
      }
    }
    .cons-type {
      background: var(--nt-primary-soft); color: var(--nt-primary-dark);
      padding: 2px 8px; border-radius: 999px;
      font-size: 0.75rem; font-weight: 600;
    }
    .cons-complaint { color: var(--nt-text); }
    .issuances-card { }
    .issuances-list { list-style: none; margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: 4px;
      li { display: flex; align-items: center; gap: 8px; padding: 6px 10px;
        background: var(--nt-surface-2); border-radius: 8px; font-size: 0.85rem; }
    }
    .badge.role-nutritionist { background: var(--nt-primary-soft); color: var(--nt-primary-dark);
      padding: 2px 8px; border-radius: 999px; font-size: 0.72rem; font-weight: 700; }
    .badge.role-client { background: var(--nt-accent-soft); color: var(--nt-accent);
      padding: 2px 8px; border-radius: 999px; font-size: 0.72rem; font-weight: 700; }
    .hash { font-family: 'JetBrains Mono', monospace; font-size: 0.75rem;
      background: var(--nt-surface); padding: 2px 6px; border-radius: 4px;
      color: var(--nt-text-muted); }
    .spacer { flex: 1; }
  `]
})
export class MyAssessmentComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  assessment = signal<ClientAssessment | null>(null);
  snapshots = signal<AssessmentSnapshot[]>([]);
  consultations = signal<Consultation[]>([]);
  issuances = signal<PrescriptionIssuance[]>([]);
  selfDraft: AssessmentSelfFill = {};
  savingSelf = signal(false);
  downloadingPdf = signal(false);
  derived = computed(() => {
    const a = this.assessment();
    if (!a) return null;
    return deriveMetrics(a, a.clientBirthDate, a.clientSex as any);
  });

  ngOnInit() {
    this.load();
  }

  private load() {
    this.api.myAssessment().subscribe(a => {
      this.assessment.set(a);
      if (a) {
        this.selfDraft = {
          foodPreferences: a.foodPreferences ?? null,
          foodAversions: a.foodAversions ?? null,
          dietaryRecall24h: a.dietaryRecall24h ?? null,
          mealFrequency: a.mealFrequency ?? null,
          waterIntakeLiters: a.waterIntakeLiters ?? null,
          physicalActivityDetails: a.physicalActivityDetails ?? null,
          sleepHours: a.sleepHours ?? null,
          goalNotes: a.goalNotes ?? null,
        };
        this.api.myAssessmentSnapshots().subscribe(s => this.snapshots.set(s));
        this.api.myConsultations().subscribe(c => this.consultations.set(c));
        this.api.myIssuances().subscribe(i => this.issuances.set(i));
      }
    });
  }

  saveSelfFill() {
    this.savingSelf.set(true);
    this.api.myAssessmentSelfFill(this.selfDraft).subscribe({
      next: updated => {
        this.savingSelf.set(false);
        this.assessment.set(updated);
        this.snack.open(this.translate.instant('myAssessment.selfFill.saved'), 'OK', { duration: 2500 });
      },
      error: err => {
        this.savingSelf.set(false);
        this.snack.open(err?.error?.error ?? this.translate.instant('myAssessment.selfFill.err'),
                        'OK', { duration: 3000 });
      }
    });
  }

  downloadPdf() {
    this.downloadingPdf.set(true);
    this.api.myPrescriptionPdf().subscribe({
      next: blob => {
        this.downloadingPdf.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'minha-prescricao.pdf';
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: err => {
        this.downloadingPdf.set(false);
        this.snack.open(err?.error?.error ?? this.translate.instant('assessment.pdf.err'),
                        'OK', { duration: 3000 });
      }
    });
  }

  hasAnthropometry(a: ClientAssessment): boolean {
    return [a.weightKg, a.heightCm, a.bodyFatPct, a.waistCm, a.hipCm, a.neckCm, a.armCm]
      .some(x => x != null);
  }
  hasClinical(a: ClientAssessment): boolean {
    return !!(a.chiefComplaint || a.medicalHistory || a.medications
           || a.preExistingConditions || a.allergies || a.familyHistory);
  }
  hasDiet(a: ClientAssessment): boolean {
    return !!(a.dietaryRecall24h || a.mealFrequency || a.waterIntakeLiters != null
           || a.alcoholFrequency || a.foodPreferences || a.foodAversions);
  }
  hasLifestyle(a: ClientAssessment): boolean {
    return !!(a.physicalActivityDetails || a.sleepHours != null || a.stressLevel
           || a.smoker || a.smokingDetails);
  }
  hasGoals(a: ClientAssessment): boolean {
    return !!(a.goalNotes || a.targetWeightKg != null || a.targetDate);
  }
  hasSkinfolds(a: ClientAssessment): boolean {
    return [a.sfTricipital, a.sfSubscapular, a.sfSuprailiac, a.sfAbdominal,
      a.sfThigh, a.sfChest, a.sfMidaxillary].some(x => x != null);
  }
  hasLabs(a: ClientAssessment): boolean {
    return [a.glucoseFasting, a.hba1c, a.insulinFasting, a.totalCholesterol, a.ldl, a.hdl,
      a.triglycerides, a.tsh, a.t4Free, a.vitaminD, a.vitaminB12, a.iron, a.ferritin,
      a.crp, a.urea, a.creatinine, a.uricAcid].some(x => x != null) || !!a.labNotes;
  }
}
