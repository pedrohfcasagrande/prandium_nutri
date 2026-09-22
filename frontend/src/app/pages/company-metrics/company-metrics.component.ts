import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { CompanyMetricsOverview } from '../../models/models';
import { AlertBannerComponent } from '../../components/alert-banner.component';
import { SkeletonComponent } from '../../components/skeleton.component';
import { SparklineComponent } from '../../components/sparkline.component';

@Component({
  selector: 'app-company-metrics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatIconModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule,
    TranslateModule, AlertBannerComponent, SkeletonComponent, SparklineComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'companyMetrics.title' | translate }}</h1>
          <p>{{ 'companyMetrics.subtitle' | translate }}</p>
        </div>
      </div>

      <app-alert-banner scope="company"></app-alert-banner>

      @if (loading()) {
        <div class="kpi-grid">
          @for (i of [0,1,2,3]; track i) { <app-skeleton type="kpi"></app-skeleton> }
        </div>
        <div style="margin-top:24px">
          <app-skeleton type="card" height="240px"></app-skeleton>
        </div>
      }
      @if (!loading() && overview(); as o) {
        <div class="kpi-grid">
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'companyMetrics.nutrisActive' | translate }}</div>
              <div class="kpi-value">{{ o.nutritionistsActive | number }}</div>
              @if (o.nutritionistsPending) {
                <div class="kpi-sub">
                  {{ o.nutritionistsPending }} {{ 'companyMetrics.pending' | translate }}
                </div>
              }
            </div>
            <div class="kpi-icon" style="background:#dcfce7;color:#166534"><mat-icon>medical_services</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#047857" [seed]="(o.nutritionistsActive || 2) + 3"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'companyMetrics.clients' | translate }}</div>
              <div class="kpi-value">{{ o.clientsTotal | number }}</div>
            </div>
            <div class="kpi-icon" style="background:#dbeafe;color:#1e40af"><mat-icon>group</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#1e40af" [seed]="(o.clientsTotal || 4) + 7"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'companyMetrics.aiMonth' | translate }}</div>
              <div class="kpi-value">{{ o.aiSuggestionsMonth | number }}</div>
            </div>
            <div class="kpi-icon" style="background:#ede9fe;color:#6d28d9"><mat-icon>auto_awesome</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#7c3aed" [seed]="(o.aiSuggestionsMonth || 5) + 11"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'companyMetrics.meals30d' | translate }}</div>
              <div class="kpi-value">{{ o.mealsLoggedLast30d | number }}</div>
            </div>
            <div class="kpi-icon" style="background:#fef3c7;color:#92400e"><mat-icon>restaurant</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#d97706" [seed]="(o.mealsLoggedLast30d || 3) + 13"></app-sparkline>
          </mat-card>
        </div>

        <mat-card>
          <mat-card-header>
            <mat-card-title>{{ 'companyMetrics.nutritionists' | translate }}</mat-card-title>
          </mat-card-header>
          <mat-card-content class="table-wrap">
            <table mat-table [dataSource]="o.nutritionists">
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>{{ 'companyMetrics.nutri' | translate }}</th>
                <td mat-cell *matCellDef="let r">
                  <div><strong>{{ r.name }}</strong></div>
                  <div class="text-muted small">{{ r.email }}</div>
                </td>
              </ng-container>
              <ng-container matColumnDef="clients">
                <th mat-header-cell *matHeaderCellDef>{{ 'companyMetrics.activeClients' | translate }}</th>
                <td mat-cell *matCellDef="let r">{{ r.activeClients }}</td>
              </ng-container>
              <ng-container matColumnDef="ai">
                <th mat-header-cell *matHeaderCellDef>{{ 'companyMetrics.aiMonth' | translate }}</th>
                <td mat-cell *matCellDef="let r">{{ r.aiSuggestionsMonth }}</td>
              </ng-container>
              <ng-container matColumnDef="lastActivity">
                <th mat-header-cell *matHeaderCellDef>{{ 'companyMetrics.lastActivity' | translate }}</th>
                <td mat-cell *matCellDef="let r">
                  {{ r.lastActivityDate ? (r.lastActivityDate | date:'shortDate') : ('companyMetrics.never' | translate) }}
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="cols"></tr>
              <tr mat-row *matRowDef="let r; columns: cols"></tr>
            </table>
            @if (!o.nutritionists?.length) {
              <p class="text-muted no-data">{{ 'companyMetrics.empty' | translate }}</p>
            }
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .loading { display: flex; justify-content: center; padding: 60px; }
    .kpi-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px; margin-bottom: 24px;
    }
    .kpi {
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      gap: 10px; padding: 24px 20px; text-align: center;
    }
    .kpi-icon {
      width: 56px; height: 56px; border-radius: 14px;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
    .kpi-label { font-size: 0.85rem; color: var(--nt-text-muted); }
    .kpi-value { font-size: 1.75rem; font-weight: 700; }
    .kpi-sub { font-size: 0.8rem; color: var(--nt-text-muted); margin-top: 2px; }
    .table-wrap { overflow-x: auto; padding: 0 !important; }
    .table-wrap table { width: 100%; }
    .no-data { padding: 16px; text-align: center; }
    table td, table th { padding: 12px 16px !important; color: var(--nt-text) !important; border-color: var(--nt-border) !important; }
    table th { color: var(--nt-text-muted) !important; font-weight: 600 !important; }
    .small { font-size: 0.85rem; }
  `]
})
export class CompanyMetricsComponent implements OnInit {
  private api = inject(ApiService);
  loading = signal(true);
  overview = signal<CompanyMetricsOverview | null>(null);
  cols = ['name', 'clients', 'ai', 'lastActivity'];

  ngOnInit(): void {
    this.api.companyMetricsOverview().subscribe({
      next: o => { this.overview.set(o); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
