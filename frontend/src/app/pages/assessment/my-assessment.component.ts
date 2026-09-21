import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { ClientAssessment } from '../../models/models';

@Component({
  selector: 'app-my-assessment',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, TranslateModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'myAssessment.title' | translate }}</h1>
          <p>{{ 'myAssessment.subtitle' | translate }}</p>
        </div>
      </div>

      @if (!assessment()) {
        <div class="empty card-surface">
          <mat-icon>assignment</mat-icon>
          <p>{{ 'myAssessment.empty' | translate }}</p>
        </div>
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

        @if (a.notes) {
          <mat-card class="section">
            <div class="section-head">
              <mat-icon>notes</mat-icon>
              <h3>{{ 'assessment.f.notes' | translate }}</h3>
            </div>
            <p class="notes-body">{{ a.notes }}</p>
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
  `]
})
export class MyAssessmentComponent implements OnInit {
  private api = inject(ApiService);
  assessment = signal<ClientAssessment | null>(null);

  ngOnInit() {
    this.api.myAssessment().subscribe(a => this.assessment.set(a));
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
}
