import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BaseChartDirective } from 'ng2-charts';
import type { ChartConfiguration } from 'chart.js';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { ProgressEntry, ProgressSummary } from '../../models/models';
import { DatePickerComponent } from '../../components/date-picker.component';

@Component({
  selector: 'app-progress',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSnackBarModule,
    BaseChartDirective, TranslateModule, DatePickerComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'progress.title' | translate }}</h1>
          <p>{{ 'progress.subtitle' | translate }}</p>
        </div>
      </div>

      <div class="stats-row">
        <div class="stat-pill">
          <div class="pill-label">{{ 'progress.current' | translate }}</div>
          <div class="pill-value">
            {{ summary()?.currentWeight ? (summary()!.currentWeight | number:'1.0-1') : '—' }}
            <span class="pill-unit">kg</span>
          </div>
        </div>
        <div class="stat-pill">
          <div class="pill-label">{{ 'progress.starting' | translate }}</div>
          <div class="pill-value">
            {{ summary()?.startingWeight ? (summary()!.startingWeight | number:'1.0-1') : '—' }}
            <span class="pill-unit">kg</span>
          </div>
        </div>
        <div class="stat-pill" [class.positive]="(summary()?.weightDelta ?? 0) < 0"
                              [class.negative]="(summary()?.weightDelta ?? 0) > 0">
          <div class="pill-label">{{ 'progress.delta' | translate }}</div>
          <div class="pill-value">
            {{ summary()?.weightDelta !== null && summary()?.weightDelta !== undefined
                ? ((summary()!.weightDelta! > 0 ? '+' : '') + (summary()!.weightDelta! | number:'1.0-1'))
                : '—' }}
            <span class="pill-unit">kg</span>
          </div>
        </div>
      </div>

      <mat-card>
        <div class="chart-head">
          <div>
            <mat-card-title>{{ 'progress.chart' | translate }}</mat-card-title>
            <mat-card-subtitle>{{ summary()?.entriesCount || 0 }} pts</mat-card-subtitle>
          </div>
        </div>
        <div class="chart-wrap">
          @if (summary() && summary()!.series.length > 0) {
            <canvas baseChart
                    [type]="'line'"
                    [data]="chartData()"
                    [options]="chartOptions">
            </canvas>
          } @else {
            <div class="empty">
              <mat-icon>show_chart</mat-icon>
              <p>{{ 'progress.empty' | translate }}</p>
            </div>
          }
        </div>
      </mat-card>

      <mat-card class="entry-card">
        <mat-card-header>
          <mat-card-title>{{ 'progress.newEntry' | translate }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="grid">
            <app-date-picker [(ngModel)]="draft.date" [label]="'progress.date' | translate"></app-date-picker>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'progress.weight' | translate }}</mat-label>
              <input matInput type="number" step="0.1" [(ngModel)]="draft.weightKg">
              <span matSuffix>kg</span>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'progress.bodyFat' | translate }}</mat-label>
              <input matInput type="number" step="0.1" [(ngModel)]="draft.bodyFatPercent">
              <span matSuffix>%</span>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'progress.waist' | translate }}</mat-label>
              <input matInput type="number" step="0.1" [(ngModel)]="draft.waistCm">
              <span matSuffix>cm</span>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'progress.hip' | translate }}</mat-label>
              <input matInput type="number" step="0.1" [(ngModel)]="draft.hipCm">
              <span matSuffix>cm</span>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'progress.arm' | translate }}</mat-label>
              <input matInput type="number" step="0.1" [(ngModel)]="draft.armCm">
              <span matSuffix>cm</span>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>{{ 'progress.chest' | translate }}</mat-label>
              <input matInput type="number" step="0.1" [(ngModel)]="draft.chestCm">
              <span matSuffix>cm</span>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full">
              <mat-label>{{ 'progress.note' | translate }}</mat-label>
              <textarea matInput rows="2" [(ngModel)]="draft.note"></textarea>
            </mat-form-field>
          </div>
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-flat-button color="primary" (click)="save()" [disabled]="!draft.weightKg">
            <mat-icon>save</mat-icon> {{ 'progress.save' | translate }}
          </button>
        </mat-card-actions>
      </mat-card>

      <div class="page-header history-header">
        <div><h2>{{ 'progress.history' | translate }}</h2></div>
      </div>

      @if (!entries().length) {
        <div class="empty card-surface">
          <mat-icon>monitor_weight</mat-icon>
          <p>{{ 'progress.empty' | translate }}</p>
        </div>
      } @else {
        <div class="entry-list">
          @for (e of entries(); track e.id) {
            <mat-card class="entry-row">
              <div class="entry-date">
                <mat-icon>event</mat-icon>
                {{ e.date }}
              </div>
              <div class="entry-metrics">
                <span class="metric weight"><strong>{{ e.weightKg | number:'1.0-1' }}</strong> kg</span>
                @if (e.bodyFatPercent) { <span class="metric">{{ e.bodyFatPercent }}% fat</span> }
                @if (e.waistCm) { <span class="metric">cintura {{ e.waistCm }}cm</span> }
                @if (e.hipCm) { <span class="metric">quadril {{ e.hipCm }}cm</span> }
                @if (e.armCm) { <span class="metric">braço {{ e.armCm }}cm</span> }
                @if (e.chestCm) { <span class="metric">peito {{ e.chestCm }}cm</span> }
              </div>
              @if (e.note) { <div class="entry-note">{{ e.note }}</div> }
              <button mat-icon-button color="warn" (click)="remove(e)" class="entry-remove">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .stats-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 12px;
    }
    .stat-pill {
      background: var(--nt-surface);
      border: 1px solid var(--nt-border);
      border-radius: var(--nt-radius);
      padding: 16px 20px;
      display: flex; flex-direction: column; gap: 4px;
      box-shadow: var(--nt-shadow);
    }
    .pill-label {
      font-size: 0.75rem; color: var(--nt-text-muted);
      text-transform: uppercase; letter-spacing: 0.06em;
    }
    .pill-value { font-size: 1.4rem; font-weight: 700; letter-spacing: -0.02em; }
    .pill-unit { font-size: 0.9rem; font-weight: 500; color: var(--nt-text-muted); margin-left: 4px; }
    .stat-pill.positive .pill-value { color: #16a34a; }
    .stat-pill.negative .pill-value { color: #dc2626; }

    .chart-head { padding: 20px 24px 0; display: flex; justify-content: space-between; align-items: baseline; }
    .chart-wrap { padding: 16px 24px 24px; min-height: 240px; position: relative; }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
    }
    .full { grid-column: 1 / -1; }

    .history-header h2 { font-size: 1.25rem; margin: 0; font-weight: 600; }

    .entry-list { display: flex; flex-direction: column; gap: 10px; }
    .entry-row {
      display: grid;
      grid-template-columns: auto 1fr auto;
      grid-template-areas: "date metrics remove" "note note note";
      gap: 12px;
      padding: 14px 20px !important;
      align-items: center;
    }
    .entry-date {
      grid-area: date;
      display: flex; align-items: center; gap: 6px;
      color: var(--nt-primary-dark); font-weight: 600;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }
    .entry-metrics {
      grid-area: metrics;
      display: flex; gap: 12px; flex-wrap: wrap; align-items: center;
    }
    .metric { color: var(--nt-text-muted); font-size: 0.9rem; }
    .metric.weight { color: var(--nt-text); font-size: 1rem; }
    .metric strong { color: var(--nt-primary-dark); font-size: 1.1rem; }
    .entry-note {
      grid-area: note;
      color: var(--nt-text-muted); font-size: 0.85rem; font-style: italic;
      padding-top: 4px; border-top: 1px dashed var(--nt-border);
    }
    .entry-remove { grid-area: remove; }
  `]
})
export class ProgressComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  summary = signal<ProgressSummary | null>(null);
  entries = signal<ProgressEntry[]>([]);
  draft: ProgressEntry = this.blankDraft();

  chartData = computed<ChartConfiguration<'line'>['data']>(() => {
    const s = this.summary();
    return {
      labels: s ? s.series.map(p => p.date) : [],
      datasets: [{
        label: this.translate.instant('progress.weight'),
        data: s ? s.series.map(p => p.weightKg) : [],
        borderColor: '#0d9488',
        backgroundColor: 'rgba(13, 148, 136, 0.15)',
        pointBackgroundColor: '#0d9488',
        tension: 0.35,
        fill: true
      }]
    };
  });

  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { beginAtZero: false, grid: { color: 'rgba(148, 163, 184, 0.15)' } },
      x: { grid: { display: false } }
    }
  };

  ngOnInit(): void { this.load(); }

  blankDraft(): ProgressEntry {
    return { date: new Date().toISOString().slice(0, 10), weightKg: 70 };
  }

  load() {
    this.api.progressSummary(90).subscribe(s => this.summary.set(s));
    this.api.listProgress().subscribe(e => this.entries.set([...e].reverse()));
  }

  save() {
    if (!this.draft.weightKg) return;
    this.api.saveProgress(this.draft).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('progress.saved'), 'OK', { duration: 2000 });
        this.draft = this.blankDraft();
        this.load();
      },
      error: () => this.snack.open(this.translate.instant('progress.saveFail'), 'OK', { duration: 3000 })
    });
  }

  remove(e: ProgressEntry) {
    if (!e.id) return;
    if (!confirm(this.translate.instant('progress.removeConfirm'))) return;
    this.api.deleteProgress(e.id).subscribe(() => this.load());
  }
}
