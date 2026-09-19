import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { HealthProfile } from '../../models/models';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';
import { DatePickerComponent } from '../../components/date-picker.component';

@Component({
  selector: 'app-profile-setup',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatDividerModule,
    MatSnackBarModule, TranslateModule, LanguageSwitcherComponent, DatePickerComponent
  ],
  template: `
    <div class="setup-shell">
      <div class="setup-topbar">
        <app-language-switcher></app-language-switcher>
      </div>
      <div class="setup-panel">
        <div class="hero">
          <div class="hero-icon"><mat-icon>waving_hand</mat-icon></div>
          <h1>{{ 'profileSetup.greeting' | translate:{ name: firstName() } }}</h1>
          <p>{{ 'profileSetup.subtitle' | translate }}</p>
        </div>

        <mat-card>
          <mat-card-header>
            <mat-card-title>{{ 'profileSetup.cardTitle' | translate }}</mat-card-title>
            <mat-card-subtitle>{{ 'profileSetup.cardSubtitle' | translate }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="grid">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'health.sex' | translate }}</mat-label>
                <mat-select [(ngModel)]="profile.sex">
                  <mat-option value="MALE">{{ 'health.male' | translate }}</mat-option>
                  <mat-option value="FEMALE">{{ 'health.female' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>

              <app-date-picker [(ngModel)]="profile.birthDate" name="birthDate"
                               [label]="'health.birthDate' | translate"></app-date-picker>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'health.weight' | translate }}</mat-label>
                <input matInput type="number" step="0.1" [(ngModel)]="profile.weightKg">
                <span matSuffix>&nbsp;kg</span>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'health.height' | translate }}</mat-label>
                <input matInput type="number" [(ngModel)]="profile.heightCm">
                <span matSuffix>&nbsp;cm</span>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'health.activity' | translate }}</mat-label>
                <mat-select [(ngModel)]="profile.activityLevel">
                  <mat-option value="SEDENTARY">{{ 'health.activityLevels.SEDENTARY' | translate }}</mat-option>
                  <mat-option value="LIGHT">{{ 'health.activityLevels.LIGHT' | translate }}</mat-option>
                  <mat-option value="MODERATE">{{ 'health.activityLevels.MODERATE' | translate }}</mat-option>
                  <mat-option value="ACTIVE">{{ 'health.activityLevels.ACTIVE' | translate }}</mat-option>
                  <mat-option value="VERY_ACTIVE">{{ 'health.activityLevels.VERY_ACTIVE' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'health.goal' | translate }}</mat-label>
                <mat-select [(ngModel)]="profile.goal">
                  <mat-option value="LOSE">{{ 'health.goals.LOSE' | translate }}</mat-option>
                  <mat-option value="MAINTAIN">{{ 'health.goals.MAINTAIN' | translate }}</mat-option>
                  <mat-option value="GAIN">{{ 'health.goals.GAIN' | translate }}</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full">
                <mat-label>{{ 'health.preferences' | translate }}</mat-label>
                <textarea matInput rows="2" [(ngModel)]="profile.preferences"
                          [placeholder]="'health.preferencesHint' | translate"></textarea>
              </mat-form-field>
            </div>

            <mat-divider class="section-divider"></mat-divider>

            <h3 class="section-title">
              <mat-icon>medical_information</mat-icon>
              {{ 'profile.healthConditionsCard' | translate }}
            </h3>
            <p class="section-sub">{{ 'profile.healthConditionsSubtitle' | translate }}</p>

            <div class="grid">
              <mat-form-field appearance="outline" class="full">
                <mat-label>{{ 'health.medications' | translate }}</mat-label>
                <textarea matInput rows="2" [(ngModel)]="profile.medications"
                          [placeholder]="'health.medicationsHint' | translate"></textarea>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full">
                <mat-label>{{ 'health.preExisting' | translate }}</mat-label>
                <textarea matInput rows="2" [(ngModel)]="profile.preExistingConditions"
                          [placeholder]="'health.preExistingHint' | translate"></textarea>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full">
                <mat-label>{{ 'health.allergies' | translate }}</mat-label>
                <textarea matInput rows="2" [(ngModel)]="profile.allergies"
                          [placeholder]="'health.allergiesHint' | translate"></textarea>
              </mat-form-field>

              <div class="full checkbox-row">
                <mat-checkbox [(ngModel)]="profile.hasBariatricSurgery" color="primary">
                  {{ 'health.bariatric' | translate }}
                </mat-checkbox>
              </div>
            </div>

            @if (error()) {
              <div class="alert-error" style="margin-top: 12px;">
                <mat-icon>error_outline</mat-icon> {{ error() }}
              </div>
            }
          </mat-card-content>
          <mat-card-actions align="end">
            <button mat-flat-button color="primary" (click)="save()" [disabled]="loading()">
              <mat-icon>arrow_forward</mat-icon> {{ 'profileSetup.submit' | translate }}
            </button>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .setup-shell {
      min-height: 100vh;
      background: var(--nt-hero-bg);
      padding: 40px 24px;
      display: flex; justify-content: center;
      position: relative;
    }
    .setup-topbar { position: absolute; top: 16px; right: 16px; display: flex; gap: 8px; }
    .setup-panel { max-width: 720px; width: 100%; }

    .hero { text-align: center; margin-bottom: 24px; }
    .hero-icon {
      width: 64px; height: 64px;
      background: var(--nt-accent);
      color: #fff;
      border-radius: 20px;
      display: inline-flex; align-items: center; justify-content: center;
      box-shadow: 0 10px 24px rgba(245, 158, 11, 0.35);
      mat-icon { font-size: 32px; width: 32px; height: 32px; }
    }
    .hero h1 { font-size: 1.75rem; margin: 12px 0 4px; font-weight: 700; letter-spacing: -0.02em; }
    .hero p { color: var(--nt-text-muted); margin: 0; }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }
    .full { grid-column: 1 / -1; }
    .checkbox-row { padding: 8px 4px; }

    .section-divider { margin: 24px 0 20px !important; }
    .section-title {
      display: flex; align-items: center; gap: 8px;
      margin: 0 0 4px; font-size: 1.05rem; font-weight: 600;
      color: var(--nt-primary-dark);
      mat-icon { color: var(--nt-primary); }
    }
    .section-sub {
      margin: 0 0 16px; color: var(--nt-text-muted); font-size: 0.85rem;
    }
  `]
})
export class ProfileSetupComponent {
  private api = inject(ApiService);
  auth = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  profile: HealthProfile = {
    sex: 'MALE', birthDate: '1990-01-01', weightKg: 70, heightCm: 170,
    activityLevel: 'MODERATE', goal: 'MAINTAIN', preferences: '',
    medications: '', hasBariatricSurgery: false,
    preExistingConditions: '', allergies: ''
  };
  loading = signal(false);
  error = signal<string | null>(null);

  firstName(): string {
    const n = this.auth.currentUser()?.name ?? '';
    return n.split(' ')[0] ?? '';
  }

  save() {
    this.error.set(null);
    if (!this.profile.sex || !this.profile.birthDate || !this.profile.weightKg
        || !this.profile.heightCm || !this.profile.activityLevel || !this.profile.goal) {
      this.error.set(this.translate.instant('profileSetup.errors.emptyFields')); return;
    }
    this.loading.set(true);
    this.api.saveHealthProfile(this.profile).subscribe({
      next: () => {
        this.auth.markProfileCompleted();
        this.snack.open(this.translate.instant('profileSetup.saved'), 'OK', { duration: 2000 });
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.error ?? this.translate.instant('profileSetup.errors.saveFail'));
      }
    });
  }
}
