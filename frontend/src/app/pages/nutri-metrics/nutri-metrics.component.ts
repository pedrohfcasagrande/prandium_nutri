import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { NutriMetricsOverview } from '../../models/models';
import { AlertBannerComponent } from '../../components/alert-banner.component';

@Component({
  selector: 'app-nutri-metrics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatIconModule, MatTableModule, MatChipsModule,
    MatProgressSpinnerModule, MatProgressBarModule, TranslateModule, AlertBannerComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriMetrics.title' | translate }}</h1>
          <p>{{ 'nutriMetrics.subtitle' | translate }}</p>
        </div>
      </div>

      <app-alert-banner scope="nutritionist"></app-alert-banner>

      @if (loading()) {
        <div class="loading"><mat-spinner diameter="40"></mat-spinner></div>
      }
      @if (!loading() && overview(); as o) {
        <div class="kpi-grid">
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#dcfce7;color:#166534"><mat-icon>group</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriMetrics.activeClients' | translate }}</div>
              <div class="kpi-value">{{ o.activeClients | number }}</div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#ede9fe;color:#6d28d9"><mat-icon>auto_awesome</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriMetrics.aiMonth' | translate }}</div>
              <div class="kpi-value">
                {{ o.aiSuggestionsMonth | number }}
                @if (o.aiSuggestionsLimit > 0) {
                  <span class="kpi-limit">/ {{ o.aiSuggestionsLimit }}</span>
                }
              </div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#dbeafe;color:#1e40af"><mat-icon>menu_book</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriMetrics.menusActive' | translate }}</div>
              <div class="kpi-value">{{ o.menusActive | number }}</div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#fef3c7;color:#92400e"><mat-icon>trending_up</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriMetrics.avgAdherence' | translate }}</div>
              <div class="kpi-value">{{ (o.avgAdherence7d * 100) | number:'1.0-0' }}%</div>
              <div class="kpi-sub">{{ 'nutriMetrics.last7d' | translate }}</div>
            </div>
          </mat-card>
          @if (o.clientsNeedingAttention > 0) {
            <mat-card class="kpi">
              <div class="kpi-icon" style="background:#fee2e2;color:#991b1b"><mat-icon>priority_high</mat-icon></div>
              <div>
                <div class="kpi-label">{{ 'nutriMetrics.needsAttention' | translate }}</div>
                <div class="kpi-value">{{ o.clientsNeedingAttention | number }}</div>
              </div>
            </mat-card>
          }
        </div>

        <mat-card>
          <mat-card-header>
            <mat-card-title>{{ 'nutriMetrics.clients' | translate }}</mat-card-title>
          </mat-card-header>
          <mat-card-content class="table-wrap">
            <table mat-table [dataSource]="o.clients">
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>{{ 'nutriMetrics.client' | translate }}</th>
                <td mat-cell *matCellDef="let r">
                  <div><strong>{{ r.name }}</strong></div>
                  <div class="text-muted small">{{ r.email }}</div>
                </td>
              </ng-container>
              <ng-container matColumnDef="adherence">
                <th mat-header-cell *matHeaderCellDef>{{ 'nutriMetrics.adherence' | translate }}</th>
                <td mat-cell *matCellDef="let r">
                  <div class="adherence-cell">
                    <mat-progress-bar mode="determinate" [value]="r.adherence7d * 100"
                                      [color]="r.needsAttention ? 'warn' : 'primary'"></mat-progress-bar>
                    <span>{{ (r.adherence7d * 100) | number:'1.0-0' }}%</span>
                  </div>
                </td>
              </ng-container>
              <ng-container matColumnDef="lastMeal">
                <th mat-header-cell *matHeaderCellDef>{{ 'nutriMetrics.lastMeal' | translate }}</th>
                <td mat-cell *matCellDef="let r">
                  {{ r.lastMealDate ? (r.lastMealDate | date:'shortDate') : ('nutriMetrics.never' | translate) }}
                </td>
              </ng-container>
              <ng-container matColumnDef="menu">
                <th mat-header-cell *matHeaderCellDef>{{ 'nutriMetrics.menu' | translate }}</th>
                <td mat-cell *matCellDef="let r">
                  @if (r.hasActivePlan) {
                    <mat-chip class="chip-ok"><mat-icon>check</mat-icon> {{ 'nutriMetrics.active' | translate }}</mat-chip>
                  } @else {
                    <mat-chip class="chip-warn">{{ 'nutriMetrics.noMenu' | translate }}</mat-chip>
                  }
                </td>
              </ng-container>
              <ng-container matColumnDef="attention">
                <th mat-header-cell *matHeaderCellDef></th>
                <td mat-cell *matCellDef="let r">
                  @if (r.needsAttention) {
                    <mat-chip class="chip-attn">
                      <mat-icon>priority_high</mat-icon> {{ 'nutriMetrics.attention' | translate }}
                    </mat-chip>
                  }
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="cols"></tr>
              <tr mat-row *matRowDef="let r; columns: cols" [class.row-attn]="r.needsAttention"></tr>
            </table>
            @if (!o.clients?.length) {
              <p class="text-muted no-data">{{ 'nutriMetrics.empty' | translate }}</p>
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
    .kpi-limit { font-size: 1rem; color: var(--nt-text-muted); font-weight: 500; }
    .kpi-sub { font-size: 0.8rem; color: var(--nt-text-muted); margin-top: 2px; }
    .table-wrap { overflow-x: auto; padding: 0 !important; }
    .table-wrap table { width: 100%; }
    .no-data { padding: 16px; text-align: center; }
    table td, table th { padding: 12px 16px !important; color: var(--nt-text) !important; border-color: var(--nt-border) !important; }
    table th { color: var(--nt-text-muted) !important; font-weight: 600 !important; }
    .small { font-size: 0.85rem; }
    .adherence-cell {
      display: flex; align-items: center; gap: 10px;
      mat-progress-bar { flex: 1; min-width: 80px; }
    }
    .chip-ok { background: #dcfce7 !important; color: #166534 !important;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }
    .chip-warn { background: #fef3c7 !important; color: #92400e !important; }
    .chip-attn { background: #fee2e2 !important; color: #991b1b !important;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }
    .row-attn { background: rgba(239, 68, 68, 0.04); }
  `]
})
export class NutriMetricsComponent implements OnInit {
  private api = inject(ApiService);
  loading = signal(true);
  overview = signal<NutriMetricsOverview | null>(null);
  cols = ['name', 'adherence', 'lastMeal', 'menu', 'attention'];

  ngOnInit(): void {
    this.api.nutriMetricsOverview().subscribe({
      next: o => { this.overview.set(o); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
