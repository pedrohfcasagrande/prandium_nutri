import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
import { HealthProfile, User } from '../../models/models';
import { DatePickerComponent } from '../../components/date-picker.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatDividerModule,
    MatSnackBarModule, TranslateModule, DatePickerComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'profile.title' | translate }}</h1>
          <p>{{ 'profile.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-card>
        <mat-card-header>
          <div class="user-header">
            <div class="avatar"><mat-icon>person</mat-icon></div>
            <div>
              <mat-card-title>{{ user()?.name }}</mat-card-title>
              <mat-card-subtitle>{{ user()?.email }}</mat-card-subtitle>
            </div>
          </div>
        </mat-card-header>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ 'profile.healthCard' | translate }}</mat-card-title>
          <mat-card-subtitle>{{ 'profile.healthSubtitle' | translate }}</mat-card-subtitle>
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
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-flat-button color="primary" (click)="save()">
            <mat-icon>save</mat-icon> {{ 'app.actions.saveChanges' | translate }}
          </button>
        </mat-card-actions>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>
            <mat-icon>medical_information</mat-icon> {{ 'profile.healthConditionsCard' | translate }}
          </mat-card-title>
          <mat-card-subtitle>{{ 'profile.healthConditionsSubtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
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
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-flat-button color="primary" (click)="save()">
            <mat-icon>save</mat-icon> {{ 'app.actions.saveChanges' | translate }}
          </button>
        </mat-card-actions>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ 'profile.securityCard' | translate }}</mat-card-title>
          <mat-card-subtitle>{{ 'profile.securitySubtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="grid">
            <mat-form-field appearance="outline">
              <mat-label>{{ 'profile.currentPassword' | translate }}</mat-label>
              <input matInput type="password" [(ngModel)]="currentPassword">
              <mat-icon matPrefix>lock</mat-icon>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'profile.newPassword' | translate }}</mat-label>
              <input matInput type="password" [(ngModel)]="newPassword">
              <mat-icon matPrefix>lock_reset</mat-icon>
            </mat-form-field>
          </div>
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-flat-button color="primary" (click)="changePassword()">
            <mat-icon>key</mat-icon> {{ 'profile.changePasswordBtn' | translate }}
          </button>
        </mat-card-actions>
      </mat-card>

      <!-- LGPD: portabilidade de dados -->
      <mat-card>
        <mat-card-header>
          <mat-card-title>{{ 'profile.lgpdCard' | translate }}</mat-card-title>
          <mat-card-subtitle>{{ 'profile.lgpdSubtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p class="text-muted">{{ 'profile.lgpdHint' | translate }}</p>
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-stroked-button (click)="exportData()" [disabled]="exporting()">
            <mat-icon>{{ exporting() ? 'hourglass_empty' : 'download' }}</mat-icon>
            {{ (exporting() ? 'profile.lgpdExporting' : 'profile.lgpdDownload') | translate }}
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
    .full { grid-column: 1 / -1; }
    .checkbox-row { padding: 8px 4px; }
    .user-header { display: flex; gap: 16px; align-items: center; }
    .avatar {
      width: 56px; height: 56px;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 32px; width: 32px; height: 32px; }
    }
    mat-card-title mat-icon { color: var(--nt-primary); vertical-align: middle; margin-right: 4px; }
  `]
})
export class ProfileComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  user = signal<User | null>(null);
  profile: HealthProfile = {
    sex: 'MALE', birthDate: '1990-01-01', weightKg: 70, heightCm: 170,
    activityLevel: 'MODERATE', goal: 'MAINTAIN', preferences: '',
    medications: '', hasBariatricSurgery: false,
    preExistingConditions: '', allergies: ''
  };
  currentPassword = '';
  newPassword = '';
  exporting = signal<boolean>(false);

  ngOnInit(): void {
    this.api.me().subscribe(u => {
      this.user.set(u);
      if (u.sex) this.profile.sex = u.sex;
      if (u.birthDate) this.profile.birthDate = u.birthDate;
      if (u.weightKg) this.profile.weightKg = u.weightKg;
      if (u.heightCm) this.profile.heightCm = u.heightCm;
      if (u.activityLevel) this.profile.activityLevel = u.activityLevel;
      if (u.goal) this.profile.goal = u.goal;
      this.profile.preferences = u.preferences ?? '';
      this.profile.medications = u.medications ?? '';
      this.profile.hasBariatricSurgery = u.hasBariatricSurgery ?? false;
      this.profile.preExistingConditions = u.preExistingConditions ?? '';
      this.profile.allergies = u.allergies ?? '';
    });
  }

  save() {
    this.api.saveHealthProfile(this.profile).subscribe({
      next: () => this.snack.open(this.translate.instant('profile.saved'), 'OK', { duration: 2000 }),
      error: () => this.snack.open(this.translate.instant('profile.saveFail'), 'OK', { duration: 3000 })
    });
  }

  changePassword() {
    if (!this.currentPassword || !this.newPassword) {
      this.snack.open(this.translate.instant('profile.fillBothPasswords'), 'OK', { duration: 2500 }); return;
    }
    this.auth.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('profile.passwordChanged'), 'OK', { duration: 2000 });
        this.currentPassword = ''; this.newPassword = '';
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('profile.passwordChangeFail'),
                                   'OK', { duration: 3000 })
    });
  }

  exportData() {
    this.exporting.set(true);
    this.api.exportMyData().subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `prandium-nutri-export-${new Date().toISOString().slice(0, 10)}.zip`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.exporting.set(false);
        this.snack.open(this.translate.instant('profile.lgpdDownloaded'), 'OK', { duration: 2500 });
      },
      error: () => {
        this.exporting.set(false);
        this.snack.open(this.translate.instant('profile.lgpdError'), 'OK', { duration: 3000 });
      }
    });
  }
}
