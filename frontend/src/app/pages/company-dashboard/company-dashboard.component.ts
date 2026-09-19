import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { Company, CompanyOverview } from '../../models/models';
import { CompanyEditDialogComponent } from './company-edit-dialog.component';

@Component({
  selector: 'app-company-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule, MatDialogModule, MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ company()?.name || 'Empresa' }}</h1>
          <p>{{ 'company.dashboard.subtitle' | translate }}</p>
        </div>
        <a mat-flat-button color="primary" routerLink="/company/nutritionists">
          <mat-icon>person_add</mat-icon> {{ 'company.dashboard.manageNutris' | translate }}
        </a>
      </div>

      <div class="grid-cards">
        <mat-card class="stat-card">
          <div class="stat-header">
            <div class="stat-icon nutris"><mat-icon>medical_services</mat-icon></div>
            <div>
              <div class="stat-label">{{ 'company.dashboard.nutrisActive' | translate }}</div>
              <div class="stat-value">{{ overview()?.nutritionistsActive ?? '—' }}</div>
            </div>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-header">
            <div class="stat-icon pending"><mat-icon>schedule</mat-icon></div>
            <div>
              <div class="stat-label">{{ 'company.dashboard.nutrisPending' | translate }}</div>
              <div class="stat-value">{{ overview()?.nutritionistsPending ?? '—' }}</div>
            </div>
          </div>
        </mat-card>

        <mat-card class="stat-card">
          <div class="stat-header">
            <div class="stat-icon clients"><mat-icon>groups</mat-icon></div>
            <div>
              <div class="stat-label">{{ 'company.dashboard.clientsTotal' | translate }}</div>
              <div class="stat-value">{{ overview()?.clientsTotal ?? '—' }}</div>
            </div>
          </div>
        </mat-card>
      </div>

      @if (company(); as c) {
        <mat-card class="info-card">
          <div class="info-header">
            <div class="info-header-icon"><mat-icon>corporate_fare</mat-icon></div>
            <div class="info-header-text">
              <div class="info-header-title">{{ 'company.dashboard.infoCard' | translate }}</div>
              <div class="info-header-sub">{{ c.name }}</div>
            </div>
            <button mat-stroked-button (click)="openEdit()">
              <mat-icon>edit</mat-icon>
              {{ 'company.dashboard.editBtn' | translate }}
            </button>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <mat-icon>badge</mat-icon>
              <div>
                <div class="info-label">{{ 'company.dashboard.tradeName' | translate }}</div>
                <div class="info-value">{{ c.tradeName || '—' }}</div>
              </div>
            </div>
            <div class="info-item">
              <mat-icon>tag</mat-icon>
              <div>
                <div class="info-label">{{ 'company.dashboard.taxId' | translate }}</div>
                <div class="info-value">{{ c.taxId || '—' }}</div>
              </div>
            </div>
            <div class="info-item">
              <mat-icon>public</mat-icon>
              <div>
                <div class="info-label">{{ 'company.dashboard.country' | translate }}</div>
                <div class="info-value">{{ c.countryCode }}</div>
              </div>
            </div>
            <div class="info-item">
              <mat-icon>mail</mat-icon>
              <div>
                <div class="info-label">{{ 'company.dashboard.email' | translate }}</div>
                <div class="info-value">{{ c.contactEmail }}</div>
              </div>
            </div>
            <div class="info-item">
              <mat-icon>call</mat-icon>
              <div>
                <div class="info-label">{{ 'company.dashboard.phone' | translate }}</div>
                <div class="info-value">{{ c.contactPhone || '—' }}</div>
              </div>
            </div>
          </div>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    mat-card-title { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
    }
    .stat-card { padding: 20px !important; display: flex; flex-direction: column; gap: 16px; }
    .stat-header { display: flex; gap: 14px; align-items: center; }
    .stat-icon {
      width: 48px; height: 48px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center; color: #fff;
      &.nutris { background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark)); }
      &.pending { background: linear-gradient(135deg, var(--nt-accent), #d97706); }
      &.clients { background: linear-gradient(135deg, #3b82f6, #2563eb); }
    }
    .stat-label { font-size: 0.85rem; color: var(--nt-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .stat-value { font-size: 2rem; font-weight: 700; letter-spacing: -0.02em; }

    .info-card { padding: 24px !important; display: flex; flex-direction: column; gap: 20px; }
    .info-header { display: flex; align-items: center; gap: 14px; }
    .info-header-text { flex: 1; min-width: 0; }
    .info-header-icon {
      width: 48px; height: 48px; border-radius: 12px;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 26px; width: 26px; height: 26px; }
    }
    .info-header-title { font-size: 1.15rem; font-weight: 700; letter-spacing: -0.01em; }
    .info-header-sub { font-size: 0.85rem; color: var(--nt-text-muted); }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 14px;
    }
    .info-item {
      display: flex; gap: 12px; align-items: flex-start;
      padding: 14px 16px;
      background: var(--nt-surface-2); border-radius: var(--nt-radius);
      mat-icon { color: var(--nt-primary); font-size: 22px; width: 22px; height: 22px; flex-shrink: 0; margin-top: 2px; }
    }
    .info-label {
      font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted); font-weight: 600; margin-bottom: 2px;
    }
    .info-value { font-size: 0.95rem; font-weight: 500; word-break: break-word; }
  `]
})
export class CompanyDashboardComponent implements OnInit {
  private api = inject(ApiService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  company = signal<Company | null>(null);
  overview = signal<CompanyOverview | null>(null);

  ngOnInit(): void {
    this.api.companyMe().subscribe(c => this.company.set(c));
    this.api.companyOverview().subscribe(o => this.overview.set(o));
  }

  openEdit() {
    const current = this.company();
    if (!current) return;
    const ref = this.dialog.open(CompanyEditDialogComponent, {
      data: current, autoFocus: false, restoreFocus: true, maxWidth: '640px', width: '100%'
    });
    ref.afterClosed().subscribe((updated: Company | null | undefined) => {
      if (!updated) return;
      this.api.companyUpdate(updated).subscribe({
        next: c => {
          this.company.set(c);
          this.snack.open(this.translate.instant('company.edit.saved'), 'OK', { duration: 2500 });
        },
        error: err => this.snack.open(err?.error?.error ?? this.translate.instant('company.edit.saveFail'),
                                       'OK', { duration: 3000 })
      });
    });
  }
}
