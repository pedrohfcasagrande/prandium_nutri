import { Component, OnInit, effect, inject, signal } from '@angular/core';
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
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PracticeService } from '../../services/practice.service';
import { ClientAssessment, ClientLink } from '../../models/models';

@Component({
  selector: 'app-nutri-assessment',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule,
    MatExpansionModule, MatSnackBarModule, TranslateModule
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
            </div>
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

          <!-- 5. Objetivos -->
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

        <div class="footer">
          @if (d.updatedAt) {
            <span class="text-muted small">
              {{ 'assessment.lastUpdate' | translate }}: {{ d.updatedAt | date:'short' }}
            </span>
          }
          <span class="spacer"></span>
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
  saving = signal(false);

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
        } else {
          this.draft.set(this.blank());
          this.loadClientName();
        }
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('assessment.loadErr'),
                                    'OK', { duration: 3000 })
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
