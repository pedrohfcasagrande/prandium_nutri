import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import type { ChartConfiguration } from 'chart.js';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { AdminMetricsOverview } from '../../models/models';
import { AlertBannerComponent } from '../../components/alert-banner.component';
import { SkeletonComponent } from '../../components/skeleton.component';
import { SparklineComponent } from '../../components/sparkline.component';

@Component({
  selector: 'app-admin-metrics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatIconModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule,
    BaseChartDirective, TranslateModule, AlertBannerComponent, SkeletonComponent,
    SparklineComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'adminMetrics.title' | translate }}</h1>
          <p>{{ 'adminMetrics.subtitle' | translate }}</p>
        </div>
      </div>

      <app-alert-banner scope="admin"></app-alert-banner>

      @if (loading()) {
        <div class="kpi-grid">
          @for (i of [0,1,2,3]; track i) { <app-skeleton type="kpi"></app-skeleton> }
        </div>
        <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:20px;margin-top:24px">
          @for (i of [0,1]; track i) { <app-skeleton type="card" height="220px"></app-skeleton> }
        </div>
      }
      @if (!loading() && overview(); as o) {
        <div class="kpi-grid">
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'adminMetrics.mrr' | translate }}</div>
              <div class="kpi-value">R$ {{ (o.mrrBrlCents / 100) | number:'1.2-2' }}</div>
            </div>
            <div class="kpi-icon" style="background:#dbeafe;color:#1d4ed8"><mat-icon>attach_money</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#1d4ed8" [seed]="o.mrrBrlCents || 1"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'adminMetrics.activeSubs' | translate }}</div>
              <div class="kpi-value">{{ o.activeSubscriptions | number }}</div>
            </div>
            <div class="kpi-icon" style="background:#dcfce7;color:#166534"><mat-icon>subscriptions</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#047857" [seed]="(o.activeSubscriptions || 2) + 3"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'adminMetrics.mau' | translate }}</div>
              <div class="kpi-value">{{ o.mau | number }}</div>
              <div class="kpi-sub">DAU {{ o.dau }} · WAU {{ o.wau }}</div>
            </div>
            <div class="kpi-icon" style="background:#fef3c7;color:#92400e"><mat-icon>groups</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#d97706" [seed]="(o.mau || 5) + 7"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'adminMetrics.newUsers30d' | translate }}</div>
              <div class="kpi-value">{{ o.newUsers30d | number }}</div>
              <div class="kpi-sub">{{ o.totalUsers }} {{ 'adminMetrics.totalUsers' | translate }}</div>
            </div>
            <div class="kpi-icon" style="background:#ede9fe;color:#6d28d9"><mat-icon>person_add</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#7c3aed" [seed]="(o.newUsers30d || 4) + 11"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'adminMetrics.nutris' | translate }}</div>
              <div class="kpi-value">{{ o.totalNutritionists | number }}</div>
            </div>
            <div class="kpi-icon" style="background:#fce7f3;color:#be185d"><mat-icon>medical_services</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#be185d" [seed]="(o.totalNutritionists || 3) + 5"></app-sparkline>
          </mat-card>
          <mat-card class="kpi">
            <div>
              <div class="kpi-label">{{ 'adminMetrics.companies' | translate }}</div>
              <div class="kpi-value">{{ o.totalCompanies | number }}</div>
            </div>
            <div class="kpi-icon" style="background:#e0f2fe;color:#075985"><mat-icon>corporate_fare</mat-icon></div>
            <app-sparkline class="kpi-spark" style="color:#0369a1" [seed]="(o.totalCompanies || 6) + 13"></app-sparkline>
          </mat-card>
        </div>

        <div class="charts-grid">
          <mat-card>
            <mat-card-header>
              <mat-card-title>{{ 'adminMetrics.mrrByCurrency' | translate }}</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              @if (mrrByCurrencyData()) {
                <canvas baseChart [type]="'bar'" [data]="mrrByCurrencyData()!" [options]="barOptions"></canvas>
              } @else {
                <p class="text-muted">{{ 'adminMetrics.empty' | translate }}</p>
              }
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header>
              <mat-card-title>{{ 'adminMetrics.plansDistribution' | translate }}</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              @if (planDistData()) {
                <canvas baseChart [type]="'doughnut'" [data]="planDistData()!" [options]="pieOptions"></canvas>
              } @else {
                <p class="text-muted">{{ 'adminMetrics.empty' | translate }}</p>
              }
            </mat-card-content>
          </mat-card>
        </div>

        <div class="tables-grid">
          <mat-card>
            <mat-card-header><mat-card-title>{{ 'adminMetrics.topCompanies' | translate }}</mat-card-title></mat-card-header>
            <mat-card-content class="table-wrap">
              <table mat-table [dataSource]="o.topCompanies">
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>{{ 'adminMetrics.company' | translate }}</th>
                  <td mat-cell *matCellDef="let r">{{ r.name }}</td>
                </ng-container>
                <ng-container matColumnDef="amount">
                  <th mat-header-cell *matHeaderCellDef>{{ 'adminMetrics.amount' | translate }}</th>
                  <td mat-cell *matCellDef="let r">{{ r.currency }} {{ (r.amountCents / 100) | number:'1.2-2' }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="companyCols"></tr>
                <tr mat-row *matRowDef="let r; columns: companyCols"></tr>
              </table>
              @if (!o.topCompanies?.length) {
                <p class="text-muted no-data">{{ 'adminMetrics.empty' | translate }}</p>
              }
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>{{ 'adminMetrics.topNutris' | translate }}</mat-card-title></mat-card-header>
            <mat-card-content class="table-wrap">
              <table mat-table [dataSource]="o.topNutris">
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>{{ 'adminMetrics.nutri' | translate }}</th>
                  <td mat-cell *matCellDef="let r">{{ r.name }}</td>
                </ng-container>
                <ng-container matColumnDef="clients">
                  <th mat-header-cell *matHeaderCellDef>{{ 'adminMetrics.activeClients' | translate }}</th>
                  <td mat-cell *matCellDef="let r">{{ r.activeClients }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="nutriCols"></tr>
                <tr mat-row *matRowDef="let r; columns: nutriCols"></tr>
              </table>
              @if (!o.topNutris?.length) {
                <p class="text-muted no-data">{{ 'adminMetrics.empty' | translate }}</p>
              }
            </mat-card-content>
          </mat-card>

          <mat-card>
            <mat-card-header><mat-card-title>{{ 'adminMetrics.actionsLast30d' | translate }}</mat-card-title></mat-card-header>
            <mat-card-content class="table-wrap">
              @let actions = o.suggestionsLast30d;
              <table mat-table [dataSource]="actions">
                <ng-container matColumnDef="action">
                  <th mat-header-cell *matHeaderCellDef>{{ 'adminMetrics.action' | translate }}</th>
                  <td mat-cell *matCellDef="let r"><mat-chip>{{ r.action }}</mat-chip></td>
                </ng-container>
                <ng-container matColumnDef="count">
                  <th mat-header-cell *matHeaderCellDef>{{ 'adminMetrics.count' | translate }}</th>
                  <td mat-cell *matCellDef="let r">{{ r.count | number }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="actionCols"></tr>
                <tr mat-row *matRowDef="let r; columns: actionCols"></tr>
              </table>
              @if (!actions.length) {
                <p class="text-muted no-data">{{ 'adminMetrics.empty' | translate }}</p>
              }
            </mat-card-content>
          </mat-card>
        </div>
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

    .charts-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 16px; margin-bottom: 24px;
    }
    .charts-grid canvas { max-height: 280px; }

    .tables-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 16px;
    }
    .table-wrap { overflow-x: auto; padding: 0 !important; }
    .table-wrap table { width: 100%; }
    .no-data { padding: 16px; text-align: center; }
    table td, table th { padding: 10px 16px !important; color: var(--nt-text) !important; border-color: var(--nt-border) !important; }
    table th { color: var(--nt-text-muted) !important; font-weight: 600 !important; }
    mat-chip { font-size: 0.75rem; }
  `]
})
export class AdminMetricsComponent implements OnInit {
  private api = inject(ApiService);

  loading = signal(true);
  overview = signal<AdminMetricsOverview | null>(null);

  companyCols = ['name', 'amount'];
  nutriCols = ['name', 'clients'];
  actionCols = ['action', 'count'];

  barOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } }
  };
  pieOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'right' } }
  };

  mrrByCurrencyData = computed<ChartConfiguration<'bar'>['data'] | null>(() => {
    const o = this.overview();
    if (!o || !Object.keys(o.mrrByCurrency).length) return null;
    return {
      labels: Object.keys(o.mrrByCurrency),
      datasets: [{
        data: Object.values(o.mrrByCurrency).map(c => c / 100),
        backgroundColor: ['#3b82f6', '#10b981', '#f59e0b']
      }]
    };
  });

  planDistData = computed<ChartConfiguration<'doughnut'>['data'] | null>(() => {
    const o = this.overview();
    if (!o || !Object.keys(o.planDistribution).length) return null;
    return {
      labels: Object.keys(o.planDistribution),
      datasets: [{
        data: Object.values(o.planDistribution),
        backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
      }]
    };
  });

  ngOnInit(): void {
    this.api.adminMetricsOverview().subscribe({
      next: o => { this.overview.set(o); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
