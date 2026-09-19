import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ImpersonationService } from '../../services/impersonation.service';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BaseChartDirective } from 'ng2-charts';
import type { ChartConfiguration } from 'chart.js';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { DailySummary, MenuPlan, ProgressSummary } from '../../models/models';
import { DatePickerComponent } from '../../components/date-picker.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatProgressBarModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, BaseChartDirective, TranslateModule,
    DatePickerComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'dashboard.title' | translate }}</h1>
          <p>{{ 'dashboard.subtitle' | translate }}</p>
        </div>
        <app-date-picker class="date-picker" [(ngModel)]="date"
                         (ngModelChange)="load()" [label]="'dashboard.date' | translate"></app-date-picker>
      </div>

      @if (summary()) {
        <div class="grid-cards">
          <mat-card class="stat-card">
            <div class="stat-header">
              <div class="stat-icon calories"><mat-icon>local_fire_department</mat-icon></div>
              <div>
                <div class="stat-label">{{ 'dashboard.calories' | translate }}</div>
                <div class="stat-value">
                  {{ summary()!.caloriesConsumed | number:'1.0-0' }}
                  <span class="stat-goal">/ {{ summary()!.calorieGoal | number:'1.0-0' }} kcal</span>
                </div>
              </div>
            </div>
            <mat-progress-bar mode="determinate" [value]="pct(summary()!.caloriesConsumed, summary()!.calorieGoal)" class="thick-bar"></mat-progress-bar>
            <div class="stat-footer">
              <span class="text-muted">{{ 'dashboard.remaining' | translate }}</span>
              <strong>{{ summary()!.calorieRemaining | number:'1.0-0' }} kcal</strong>
            </div>
          </mat-card>

          <mat-card class="stat-card">
            <div class="stat-header">
              <div class="stat-icon protein"><mat-icon>fitness_center</mat-icon></div>
              <div>
                <div class="stat-label">{{ 'dashboard.protein' | translate }}</div>
                <div class="stat-value">
                  {{ summary()!.proteinConsumed | number:'1.0-1' }}
                  <span class="stat-goal">/ {{ summary()!.proteinGoal | number:'1.0-0' }} g</span>
                </div>
              </div>
            </div>
            <mat-progress-bar mode="determinate" color="accent" [value]="pct(summary()!.proteinConsumed, summary()!.proteinGoal)" class="thick-bar"></mat-progress-bar>
            <div class="stat-footer">
              <span class="text-muted">{{ 'dashboard.remaining' | translate }}</span>
              <strong>{{ summary()!.proteinRemaining | number:'1.0-1' }} g</strong>
            </div>
          </mat-card>

          <mat-card class="stat-card">
            <div class="stat-header">
              <div class="stat-icon macros"><mat-icon>pie_chart</mat-icon></div>
              <div>
                <div class="stat-label">{{ 'dashboard.macros' | translate }}</div>
              </div>
            </div>
            <div class="macro-row">
              <span class="text-muted">{{ 'dashboard.carbs' | translate }}</span>
              <strong>{{ summary()!.carbsConsumed | number:'1.0-1' }} g</strong>
            </div>
            <div class="macro-row">
              <span class="text-muted">{{ 'dashboard.fat' | translate }}</span>
              <strong>{{ summary()!.fatConsumed | number:'1.0-1' }} g</strong>
            </div>
            <div class="macro-row">
              <span class="text-muted">{{ 'dashboard.mealsCount' | translate }}</span>
              <strong>{{ summary()!.meals.length }}</strong>
            </div>
          </mat-card>
        </div>

        <!-- Card de evolução -->
        <mat-card class="progress-card">
          <div class="progress-head">
            <div class="progress-title">
              <div class="progress-icon"><mat-icon>monitor_weight</mat-icon></div>
              <div>
                <div class="p-label">{{ 'dashboard.progressCard' | translate }}</div>
                @if (progress() && progress()!.currentWeight !== null) {
                  <div class="p-stats">
                    <span class="p-cur">{{ progress()!.currentWeight | number:'1.0-1' }} kg</span>
                    @if (progress()!.weightDelta !== null && progress()!.weightDelta !== undefined) {
                      <span class="p-delta"
                            [class.p-delta-neg]="(progress()!.weightDelta ?? 0) > 0"
                            [class.p-delta-pos]="(progress()!.weightDelta ?? 0) < 0">
                        {{ progress()!.weightDelta! > 0 ? '+' : '' }}{{ progress()!.weightDelta | number:'1.0-1' }} kg
                      </span>
                    }
                  </div>
                }
              </div>
            </div>
            <a mat-stroked-button routerLink="/progress">
              <mat-icon>arrow_forward</mat-icon>
            </a>
          </div>
          <div class="progress-chart">
            @if (progress() && progress()!.series.length > 1) {
              <canvas baseChart
                      [type]="'line'"
                      [data]="chartData()"
                      [options]="chartOptions">
              </canvas>
            } @else {
              <div class="mini-empty">
                <mat-icon>show_chart</mat-icon>
                <span>{{ 'dashboard.noProgress' | translate }}</span>
                <a mat-flat-button color="primary" routerLink="/progress">
                  <mat-icon>add</mat-icon> {{ 'dashboard.logProgress' | translate }}
                </a>
              </div>
            }
          </div>
        </mat-card>

        @if (activePlan(); as ap) {
          <mat-card class="active-plan-card">
            <div class="plan-head">
              <div class="plan-icon"><mat-icon>menu_book</mat-icon></div>
              <div class="plan-head-text">
                <div class="p-label">{{ 'dashboard.activePlan' | translate }}</div>
                <div class="p-name">{{ ap.name }}</div>
              </div>
              <span class="spacer"></span>
              <a mat-flat-button color="primary" routerLink="/menus">
                <mat-icon>menu_book</mat-icon>
                {{ 'dashboard.viewMenu' | translate }}
              </a>
            </div>
            @if (todaysMenuPreview(); as preview) {
              <div class="today-block">
                <div class="today-head">
                  <mat-icon>today</mat-icon>
                  <span>
                    {{ (isToday() ? 'dashboard.todaysMenu' : 'dashboard.dateMenu')
                        | translate:{ day: preview.dayLabel, date: formattedSelectedDate() } }}
                  </span>
                </div>
                <pre class="today-content">{{ preview.text }}</pre>
              </div>
            }
          </mat-card>
        }

        <mat-card>
          <mat-card-header><mat-card-title>{{ 'dashboard.quickActions' | translate }}</mat-card-title></mat-card-header>
          <mat-card-content class="actions-row">
            <a mat-flat-button color="primary" routerLink="/meals">
              <mat-icon>add</mat-icon> {{ 'dashboard.registerMeal' | translate }}
            </a>
            <a mat-stroked-button routerLink="/suggestions">
              <mat-icon>auto_awesome</mat-icon> {{ 'dashboard.suggestMenu' | translate }}
            </a>
            <a mat-stroked-button routerLink="/menus">
              <mat-icon>menu_book</mat-icon> {{ 'dashboard.myMenus' | translate }}
            </a>
            <a mat-stroked-button routerLink="/progress">
              <mat-icon>monitor_weight</mat-icon> {{ 'dashboard.logProgress' | translate }}
            </a>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .date-picker { width: 200px; }
    @media (max-width: 600px) { .date-picker { width: 100%; } }

    .stat-card { padding: 20px !important; display: flex; flex-direction: column; gap: 16px; }
    .stat-header { display: flex; gap: 14px; align-items: center; }
    .stat-icon {
      width: 48px; height: 48px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center; color: #fff;
      &.calories { background: linear-gradient(135deg, var(--nt-accent), #d97706); }
      &.protein { background: linear-gradient(135deg, var(--nt-purple), #7c3aed); }
      &.macros { background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark)); }
    }
    .stat-label { font-size: 0.85rem; color: var(--nt-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .stat-value { font-size: 1.6rem; font-weight: 700; letter-spacing: -0.02em; }
    .stat-goal { color: var(--nt-text-muted); font-size: 0.9rem; font-weight: 500; }
    .thick-bar { height: 10px !important; border-radius: 5px; overflow: hidden; }
    .stat-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 4px; }
    .macro-row {
      display: flex; justify-content: space-between; padding: 6px 0;
      border-bottom: 1px solid var(--nt-border);
      &:last-child { border-bottom: none; }
    }

    .progress-card { padding: 0 !important; overflow: hidden; }
    .progress-head {
      display: flex; justify-content: space-between; align-items: center; gap: 12px;
      padding: 20px 24px 12px;
    }
    .progress-title { display: flex; gap: 14px; align-items: center; }
    .progress-icon {
      width: 44px; height: 44px; border-radius: 12px;
      background: linear-gradient(135deg, #3b82f6, #2563eb);
      color: #fff;
      display: flex; align-items: center; justify-content: center;
    }
    .p-label { font-size: 0.85rem; color: var(--nt-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .p-stats { display: flex; align-items: baseline; gap: 12px; margin-top: 2px; }
    .p-cur { font-size: 1.4rem; font-weight: 700; letter-spacing: -0.02em; }
    .p-delta { font-size: 0.95rem; font-weight: 600; }
    .p-delta-pos { color: #16a34a; }
    .p-delta-neg { color: #dc2626; }

    .progress-chart { padding: 0 12px 16px; height: 160px; position: relative; }
    .mini-empty {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 20px; height: 100%; padding: 12px 0; color: var(--nt-text-muted);
      mat-icon { font-size: 36px; width: 36px; height: 36px; opacity: 0.5; }
    }

    .actions-row { display: flex; gap: 12px; flex-wrap: wrap; }

    .active-plan-card {
      padding: 16px 20px !important;
      border-left: 4px solid var(--nt-primary) !important;
      display: flex; flex-direction: column; gap: 12px;
    }
    .plan-head {
      display: flex; align-items: center; gap: 12px;
    }
    .plan-head-text { min-width: 0; }
    .today-block {
      padding: 12px 14px; border-radius: var(--nt-radius);
      background: var(--nt-surface-2);
    }
    .today-head {
      display: flex; align-items: center; gap: 8px;
      font-weight: 600; color: var(--nt-primary-dark); margin-bottom: 8px;
      mat-icon { color: var(--nt-primary); font-size: 20px; width: 20px; height: 20px; }
    }
    .today-content {
      margin: 0; white-space: pre-wrap; font-family: inherit;
      font-size: 0.88rem; line-height: 1.6; color: var(--nt-text);
    }
    .plan-icon {
      width: 44px; height: 44px; border-radius: 12px;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff;
      display: flex; align-items: center; justify-content: center;
    }
    .p-label { font-size: 0.75rem; color: var(--nt-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .p-name { font-weight: 600; font-size: 1.1rem; margin-top: 2px; }
    .spacer { flex: 1; }
  `]
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private imp = inject(ImpersonationService);
  private router = inject(Router);
  summary = signal<DailySummary | null>(null);
  progress = signal<ProgressSummary | null>(null);
  activePlan = signal<MenuPlan | null>(null);
  date = new Date().toISOString().slice(0, 10);

  chartData = computed<ChartConfiguration<'line'>['data']>(() => {
    const p = this.progress();
    return {
      labels: p ? p.series.map(x => x.date) : [],
      datasets: [{
        data: p ? p.series.map(x => x.weightKg) : [],
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        pointRadius: 0,
        tension: 0.35,
        fill: true
      }]
    };
  });

  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false }, tooltip: { enabled: true } },
    scales: {
      y: { display: false, beginAtZero: false },
      x: { display: false }
    }
  };

  ngOnInit(): void {
    // Nutri/empresa/admin sem impersonation nao tem "meu dashboard de refeicoes";
    // redireciona para a home apropriada do papel efetivo.
    if (!this.imp.target()) {
      const role = this.auth.currentUser()?.role;
      if (role === 'NUTRITIONIST') { this.router.navigate(['/nutritionist/dashboard']); return; }
      if (role === 'COMPANY')      { this.router.navigate(['/company/dashboard']); return; }
    }
    this.load();
  }

  load() {
    this.api.dashboard(this.date).subscribe(s => this.summary.set(s));
    this.api.progressSummary(30).subscribe(p => this.progress.set(p));
    this.api.activeMenuPlan().subscribe(p => this.activePlan.set(p));
  }

  pct(v: number, goal: number): number { return goal ? Math.min(100, Math.round((v / goal) * 100)) : 0; }

  /**
   * Tenta extrair a secao do cardapio referente ao dia de hoje.
   * Se for cardapio semanal (## Dia 1 - Segunda, etc), retorna so as linhas do dia atual.
   * Se for cardapio diario, retorna null (o hero-card ja mostra tudo).
   */
  todaysMenuPreview(): { dayLabel: string; text: string } | null {
    const plan = this.activePlan();
    if (!plan?.content) return null;

    const lines = plan.content.split('\n');
    const headerRegex = /^#{1,4}\s+(.+)/;
    const dayRegex = /^(dia\s*\d+|segunda|terca|terça|quarta|quinta|sexta|sabado|sábado|domingo|monday|tuesday|wednesday|thursday|friday|saturday|sunday|lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)/i;

    const dayHeaders: { index: number; title: string; dayNumber: number | null; dayName: string | null }[] = [];
    lines.forEach((raw, idx) => {
      const m = raw.trim().match(headerRegex);
      if (!m) return;
      const cleanTitle = m[1].replace(/\*\*(.*?)\*\*/g, '$1').trim();
      if (!dayRegex.test(cleanTitle)) return;
      const numMatch = cleanTitle.match(/dia\s*(\d+)/i);
      const nameMatch = cleanTitle.match(/(segunda|terca|terça|quarta|quinta|sexta|sabado|sábado|domingo|monday|tuesday|wednesday|thursday|friday|saturday|sunday|lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)/i);
      dayHeaders.push({
        index: idx,
        title: cleanTitle,
        dayNumber: numMatch ? Number(numMatch[1]) : null,
        dayName: nameMatch ? nameMatch[1].toLowerCase() : null
      });
    });

    if (!dayHeaders.length) return null;

    // Usa a data selecionada no dashboard (nao a de hoje) para casar com o dia do cardapio
    const selected = this.parseSelectedDate();
    const jsDay = selected.getDay();
    const dayNameByJs = ['domingo', 'segunda', 'terca', 'quarta', 'quinta', 'sexta', 'sabado'];
    const targetDayName = dayNameByJs[jsDay];

    // Tenta match por nome de dia primeiro
    let target = dayHeaders.find(h => h.dayName?.startsWith(targetDayName.slice(0, 3))
                                    || h.dayName === targetDayName
                                    || h.dayName === targetDayName.replace('c', 'ç'));
    // Fallback: usar "Dia N" onde N = dia semana (segunda=1, domingo=7)
    if (!target) {
      const isoDay = jsDay === 0 ? 7 : jsDay;
      target = dayHeaders.find(h => h.dayNumber === isoDay);
    }
    if (!target) target = dayHeaders[0];

    // Coleta linhas ate o proximo dia
    const nextIdx = dayHeaders.find(h => h.index > target!.index)?.index ?? lines.length;
    const body = lines.slice(target.index + 1, nextIdx)
                      .filter(l => l.trim())
                      .join('\n')
                      .trim();

    return { dayLabel: target.title, text: body };
  }

  /** Interpreta this.date (ISO YYYY-MM-DD) como Date local (sem shift de fuso). */
  private parseSelectedDate(): Date {
    if (!this.date) return new Date();
    const [y, m, d] = this.date.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  isToday(): boolean {
    return this.date === new Date().toISOString().slice(0, 10);
  }

  formattedSelectedDate(): string {
    const d = this.parseSelectedDate();
    return d.toLocaleDateString();
  }
}
