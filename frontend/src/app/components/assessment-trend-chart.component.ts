import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { TranslateModule } from '@ngx-translate/core';

import { AssessmentSnapshot, ClientAssessment } from '../models/models';
import { bmi, deriveMetrics, Sex } from '../util/assessment-metrics';

type MetricKey =
  | 'weightKg' | 'bodyFatPct' | 'bmi'
  | 'waistCm' | 'hipCm' | 'thighCm' | 'armCm'
  | 'glucoseFasting' | 'hba1c' | 'totalCholesterol' | 'ldl' | 'hdl' | 'triglycerides'
  | 'vitaminD';

interface MetricDef {
  key: MetricKey;
  labelKey: string;
  unit: string;
  color: string;
}

const METRICS: MetricDef[] = [
  { key: 'weightKg',         labelKey: 'assessment.f.weightKg',         unit: 'kg',     color: '#0e8f83' },
  { key: 'bodyFatPct',       labelKey: 'assessment.f.bodyFatPct',       unit: '%',      color: '#f59e0b' },
  { key: 'bmi',              labelKey: 'assessment.trend.bmi',          unit: '',       color: '#7c3aed' },
  { key: 'waistCm',          labelKey: 'assessment.f.waistCm',          unit: 'cm',     color: '#0ea5e9' },
  { key: 'hipCm',            labelKey: 'assessment.f.hipCm',            unit: 'cm',     color: '#22c55e' },
  { key: 'thighCm',          labelKey: 'assessment.f.thighCm',          unit: 'cm',     color: '#10b981' },
  { key: 'armCm',            labelKey: 'assessment.f.armCm',            unit: 'cm',     color: '#f97316' },
  { key: 'glucoseFasting',   labelKey: 'assessment.f.glucoseFasting',   unit: 'mg/dL',  color: '#ef4444' },
  { key: 'hba1c',            labelKey: 'assessment.f.hba1c',            unit: '%',      color: '#dc2626' },
  { key: 'totalCholesterol', labelKey: 'assessment.f.totalCholesterol', unit: 'mg/dL',  color: '#9333ea' },
  { key: 'ldl',              labelKey: 'assessment.f.ldl',              unit: 'mg/dL',  color: '#c026d3' },
  { key: 'hdl',              labelKey: 'assessment.f.hdl',              unit: 'mg/dL',  color: '#06b6d4' },
  { key: 'triglycerides',    labelKey: 'assessment.f.triglycerides',    unit: 'mg/dL',  color: '#f43f5e' },
  { key: 'vitaminD',         labelKey: 'assessment.f.vitaminD',         unit: 'ng/mL',  color: '#eab308' },
];

@Component({
  selector: 'app-assessment-trend-chart',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatChipsModule, BaseChartDirective, TranslateModule],
  template: `
    <mat-card class="trend-card">
      <div class="trend-head">
        <mat-icon>trending_up</mat-icon>
        <h3>{{ 'assessment.trend.title' | translate }}</h3>
      </div>

      <mat-chip-listbox class="metrics" [value]="metric()" (change)="onMetric($event.value)">
        @for (m of availableMetrics(); track m.key) {
          <mat-chip-option [value]="m.key">
            {{ m.labelKey | translate }}
          </mat-chip-option>
        }
      </mat-chip-listbox>

      @if (dataset().labels.length > 1) {
        <div class="chart-wrap">
          <canvas baseChart
                  [type]="'line'"
                  [data]="chartData()"
                  [options]="chartOptions"></canvas>
        </div>
      } @else {
        <p class="text-muted small empty">{{ 'assessment.trend.notEnoughData' | translate }}</p>
      }
    </mat-card>
  `,
  styles: [`
    .trend-card { padding: 16px !important; display: flex; flex-direction: column; gap: 12px; }
    .trend-head { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
      h3 { margin: 0; font-size: 1rem; }
    }
    .metrics { display: flex; flex-wrap: wrap; gap: 6px; }
    .chart-wrap { position: relative; height: 260px; }
    .empty { text-align: center; padding: 30px 0; margin: 0; }
    .small { font-size: 0.85rem; }
  `]
})
export class AssessmentTrendChartComponent {
  @Input() snapshots: AssessmentSnapshot[] = [];
  @Input() current: ClientAssessment | null = null;
  @Input() birthDate: string | null = null;
  @Input() sex: string | null = null;

  metric = signal<MetricKey>('weightKg');

  /** Concatena snapshots + current ordenados por data ascendente. */
  private points = computed(() => {
    const list: Array<{ date: string; a: ClientAssessment; label: string }> = [];
    for (const s of this.snapshots) {
      if (!s.payload) continue;
      list.push({ date: s.createdAt, a: s.payload, label: s.label || `v${s.version}` });
    }
    if (this.current?.updatedAt) {
      list.push({
        date: this.current.updatedAt,
        a: this.current,
        label: 'atual'
      });
    }
    return list.sort((x, y) => new Date(x.date).getTime() - new Date(y.date).getTime());
  });

  /** So mostra chip pra metricas que tem >= 1 valor no historico. */
  availableMetrics = computed(() => {
    const pts = this.points();
    return METRICS.filter(m => pts.some(p => this.valueOf(p.a, m.key) != null));
  });

  dataset = computed(() => {
    const key = this.metric();
    const pts = this.points();
    const labels: string[] = [];
    const values: (number | null)[] = [];
    for (const p of pts) {
      const v = this.valueOf(p.a, key);
      if (v == null) continue;
      const d = new Date(p.date);
      labels.push(d.toLocaleDateString());
      values.push(v);
    }
    return { labels, values };
  });

  chartData = computed<ChartData<'line'>>(() => {
    const ds = this.dataset();
    const m = METRICS.find(x => x.key === this.metric())!;
    return {
      labels: ds.labels,
      datasets: [{
        data: ds.values,
        label: m.labelKey,
        borderColor: m.color,
        backgroundColor: m.color + '22',
        tension: 0.35,
        fill: true,
        pointRadius: 4,
        pointHoverRadius: 6,
      }]
    };
  });

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { mode: 'index', intersect: false }
    },
    scales: {
      y: { beginAtZero: false, ticks: { color: '#8a95a5' }, grid: { color: 'rgba(0,0,0,0.06)' } },
      x: { ticks: { color: '#8a95a5' }, grid: { display: false } },
    }
  };

  onMetric(k: MetricKey) { if (k) this.metric.set(k); }

  private valueOf(a: ClientAssessment, key: MetricKey): number | null {
    if (key === 'bmi') return bmi(a.weightKg, a.heightCm);
    const v = (a as any)[key];
    return v == null ? null : Number(v);
  }
}
