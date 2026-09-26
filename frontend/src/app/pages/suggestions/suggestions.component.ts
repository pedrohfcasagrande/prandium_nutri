import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { MenuPlan } from '../../models/models';

interface MenuSection {
  title: string;
  lines: string[];
  level?: number;
}

interface DayGroup {
  title: string;
  meals: MenuSection[];
}

@Component({
  selector: 'app-suggestions',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatButtonToggleModule, MatProgressSpinnerModule,
    MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'suggestions.title' | translate }}</h1>
          <p>{{ 'suggestions.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-card class="input-card">
        <div class="input-body">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'suggestions.notes' | translate }}</mat-label>
            <textarea matInput rows="3" [(ngModel)]="notes"
                      [placeholder]="'suggestions.notesHint' | translate"></textarea>
          </mat-form-field>

          <div class="prompts">
            <span class="prompts-label">{{ 'suggestions.quickPrompts' | translate }}</span>
            <div class="prompts-chips">
              @for (p of quickPrompts; track p.key) {
                <mat-chip class="prompt-chip" (click)="usePrompt(p.key)">
                  <mat-icon>{{ p.icon }}</mat-icon>
                  <span>{{ ('suggestions.prompts.' + p.key) | translate }}</span>
                </mat-chip>
              }
            </div>
          </div>

          <div class="scope-row">
            <span class="scope-label">{{ 'suggestions.scopeLabel' | translate }}</span>
            <mat-button-toggle-group [value]="scope" (change)="scope = $event.value" hideSingleSelectionIndicator>
              <mat-button-toggle value="DAY">
                <mat-icon>today</mat-icon> {{ 'suggestions.scopeDay' | translate }}
              </mat-button-toggle>
              <mat-button-toggle value="WEEK">
                <mat-icon>date_range</mat-icon> {{ 'suggestions.scopeWeek' | translate }}
              </mat-button-toggle>
            </mat-button-toggle-group>
          </div>

          <div class="action-row">
            <button mat-flat-button color="primary" class="gen-btn"
                    (click)="generate()" [disabled]="loading()">
              <span class="gen-btn-content">
                @if (loading()) {
                  <mat-spinner diameter="20"></mat-spinner>
                  <span>{{ 'suggestions.generating' | translate }}</span>
                } @else {
                  <mat-icon>auto_awesome</mat-icon>
                  <span>{{ scope === 'WEEK' ? ('suggestions.generateWeek' | translate) : ('suggestions.generate' | translate) }}</span>
                }
              </span>
            </button>
          </div>
        </div>
      </mat-card>

      @if (loading()) {
        <div class="loading-cards">
          <div class="skel"></div>
          <div class="skel"></div>
          <div class="skel"></div>
        </div>
      } @else if (days().length) {
        <mat-card class="result-header">
          <div class="result-title">
            <mat-icon>restaurant_menu</mat-icon>
            <span>{{ 'suggestions.resultTitle' | translate }}</span>
          </div>
          <div class="result-actions">
            <button mat-stroked-button color="primary" (click)="saveAsMenu()">
              <mat-icon>bookmark_add</mat-icon>
              {{ 'suggestions.saveAsMenu' | translate }}
            </button>
            <button mat-flat-button color="primary" (click)="saveAsActive()">
              <mat-icon>check_circle</mat-icon>
              {{ 'suggestions.saveAsActive' | translate }}
            </button>
          </div>
        </mat-card>

        <div class="menu-grid">
          @for (d of days(); track $index; let dayIdx = $index) {
            <mat-card class="day-card">
              @if (d.title) {
                <div class="day-head">
                  <mat-icon>event</mat-icon>
                  <span>{{ d.title }}</span>
                </div>
              }
              @for (m of d.meals; track $index) {
                <div class="meal-block">
                  @if (m.title) {
                    <div class="meal-head">
                      <mat-icon>{{ sectionIcon(m.title, $index) }}</mat-icon>
                      <span>{{ m.title }}</span>
                    </div>
                  }
                  <ul class="menu-list">
                    @for (l of m.lines; track $index) {
                      <li>{{ l }}</li>
                    }
                  </ul>
                </div>
              }
            </mat-card>
          }
        </div>
      } @else {
        <div class="empty card-surface">
          <mat-icon>restaurant_menu</mat-icon>
          <p>{{ 'suggestions.empty' | translate }}</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .input-card { padding: 0 !important; overflow: hidden; }

    .input-hero {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: linear-gradient(135deg, #8b5cf6, #6366f1);
      color: #fff;
    }
    .hero-icon {
      width: 48px; height: 48px;
      background: rgba(255,255,255,0.2);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
    .hero-title { font-size: 1.15rem; font-weight: 700; letter-spacing: -0.01em; }
    .hero-sub { font-size: 0.85rem; opacity: 0.9; }

    .input-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }
    .input-body mat-form-field { width: 100%; }

    .prompts { display: flex; flex-direction: column; gap: 10px; align-items: center; }
    .prompts-label {
      font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted);
      text-align: center;
    }
    .prompts-chips { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
    .prompt-chip {
      cursor: pointer;
      padding: 6px 14px !important;
      background: var(--nt-surface-2) !important;
      transition: transform 0.15s ease, box-shadow 0.15s ease;
    }
    .prompt-chip ::ng-deep .mdc-evolution-chip__cell,
    .prompt-chip ::ng-deep .mat-mdc-chip-action,
    .prompt-chip ::ng-deep .mat-mdc-chip-action-label {
      display: inline-flex !important;
      align-items: center !important;
      gap: 10px !important;
    }
    .prompt-chip mat-icon {
      font-size: 18px; width: 18px; height: 18px;
      color: var(--nt-primary); flex-shrink: 0;
      margin-right: 2px;
    }
    .prompt-chip:hover { transform: translateY(-1px); box-shadow: var(--nt-shadow); }

    .scope-row { display: flex; flex-direction: column; align-items: center; gap: 10px; }
    .scope-label {
      font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted);
    }
    .scope-row mat-button-toggle-group { border-radius: 999px; overflow: hidden; }
    .scope-row mat-button-toggle mat-icon {
      font-size: 18px; width: 18px; height: 18px; margin-right: 6px;
      vertical-align: middle;
    }

    .action-row { display: flex; justify-content: center; padding-top: 4px; }
    .gen-btn {
      min-width: 220px;
      height: 48px;
      padding: 0 28px !important;
      border-radius: 999px !important;
      font-weight: 600;
      letter-spacing: 0.01em;
    }
    .gen-btn .gen-btn-content {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
    }
    .gen-btn mat-icon,
    .gen-btn mat-spinner {
      margin: 0 !important;
      flex-shrink: 0;
    }

    .result-header {
      display: flex; justify-content: space-between; align-items: center; gap: 12px;
      padding: 16px 20px !important;
      flex-wrap: wrap;
    }
    .result-title {
      display: flex; align-items: center; gap: 10px;
      font-size: 1.1rem; font-weight: 600;
      color: var(--nt-primary-dark);
      mat-icon { color: var(--nt-primary); }
    }
    .result-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .result-actions button { display: inline-flex; align-items: center; gap: 6px; }

    .menu-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;
    }
    .day-card {
      padding: 20px !important;
      display: flex; flex-direction: column; gap: 14px;
      border-left: 4px solid var(--nt-primary) !important;
    }
    .day-head {
      display: flex; align-items: center; gap: 10px;
      padding-bottom: 10px; border-bottom: 1px solid var(--nt-border);
      font-size: 1.1rem; font-weight: 700; letter-spacing: -0.01em;
      color: var(--nt-primary-dark);
      mat-icon { color: var(--nt-primary); }
    }
    .meal-block { display: flex; flex-direction: column; gap: 6px; }
    .meal-head {
      display: flex; align-items: center; gap: 8px;
      font-size: 0.9rem; font-weight: 600; text-transform: uppercase;
      letter-spacing: 0.04em; color: var(--nt-text-muted);
      mat-icon { color: var(--nt-primary); font-size: 20px; width: 20px; height: 20px; }
    }
    .menu-list { margin: 0; padding-left: 20px; line-height: 1.7; color: var(--nt-text); }
    .menu-list li { padding: 2px 0; }

    .loading-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px;
    }
    .skel {
      height: 140px;
      border-radius: var(--nt-radius);
      background: linear-gradient(90deg,
        var(--nt-surface-2) 0%, var(--nt-surface) 50%, var(--nt-surface-2) 100%);
      background-size: 200% 100%;
      animation: shimmer 1.2s ease-in-out infinite;
    }
    @keyframes shimmer {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }
  `]
})
export class SuggestionsComponent {
  private api = inject(ApiService);
  private translate = inject(TranslateService);
  private snack = inject(MatSnackBar);
  private router = inject(Router);

  notes = '';
  scope: 'DAY' | 'WEEK' = 'DAY';
  raw = signal<string>('');
  sections = signal<MenuSection[]>([]);
  days = signal<DayGroup[]>([]);
  loading = signal(false);

  quickPrompts = [
    { key: 'quickDinner', icon: 'bolt' },
    { key: 'postWorkout', icon: 'fitness_center' },
    { key: 'lightSnack', icon: 'cookie' },
    { key: 'highProtein', icon: 'egg' }
  ];

  usePrompt(key: string) {
    this.notes = this.translate.instant(`suggestions.prompts.${key}`);
  }

  generate() {
    this.loading.set(true);
    this.sections.set([]);
    this.days.set([]);
    this.api.suggest(this.notes, this.scope).subscribe({
      next: r => {
        this.raw.set(r.menu);
        const flat = this.parseMenu(r.menu);
        this.sections.set(flat);
        this.days.set(this.groupByDay(flat));
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.snack.open(err?.error?.error ?? this.translate.instant('suggestions.error'),
                        'OK', { duration: 4000 });
      }
    });
  }

  private buildPlanFromCurrent(active: boolean): MenuPlan {
    return {
      name: this.translate.instant('suggestions.defaultPlanName',
              { date: new Date().toLocaleDateString() }),
      source: 'AI',
      content: this.raw(),
      startDate: new Date().toISOString().slice(0, 10),
      active
    };
  }

  saveAsMenu() {
    sessionStorage.setItem('prandiumnutri.pendingMenu', JSON.stringify(this.buildPlanFromCurrent(false)));
    this.router.navigate(['/menus']);
  }

  saveAsActive() {
    this.api.saveMenuPlan(this.buildPlanFromCurrent(true)).subscribe({
      next: () => this.snack.open(this.translate.instant('suggestions.savedActive'), 'OK', { duration: 2500 }),
      error: () => this.snack.open(this.translate.instant('suggestions.saveError'), 'OK', { duration: 3000 })
    });
  }

  private parseMenu(md: string): MenuSection[] {
    const lines = md.split('\n');
    const sections: MenuSection[] = [];
    let current: MenuSection | null = null;

    for (const raw of lines) {
      const line = raw.trim();
      if (!line) continue;

      const headerMatch = line.match(/^(#{1,4})\s+(.+)/);
      if (headerMatch) {
        if (current && (current.lines.length || current.title)) sections.push(current);
        const cleanTitle = headerMatch[2]
          .replace(/\*\*(.*?)\*\*/g, '$1')       // strip bold
          .replace(/^[\p{Emoji_Presentation}\p{Extended_Pictographic}\s:*_-]+/u, '')  // strip emojis/leading symbols
          .trim();
        current = { title: cleanTitle, lines: [], level: headerMatch[1].length };
        continue;
      }

      const bulletMatch = line.match(/^[-*]\s+(.+)/);
      if (bulletMatch) {
        if (!current) current = { title: '', lines: [] };
        current.lines.push(bulletMatch[1].replace(/\*\*(.*?)\*\*/g, '$1'));
        continue;
      }

      if (!current) current = { title: '', lines: [] };
      current.lines.push(line.replace(/\*\*(.*?)\*\*/g, '$1'));
    }
    if (current) sections.push(current);

    return sections.filter(s => s.lines.length > 0 || s.title);
  }

  private groupByDay(sections: MenuSection[]): DayGroup[] {
    // Detecta cabecalho de dia: "Dia N", "Dia N -", ou nome de dia em PT/EN/FR
    const dayRegex = /(^|\s)(dia\s*\d+|segunda|terca|terça|quarta|quinta|sexta|sabado|sábado|domingo|monday|tuesday|wednesday|thursday|friday|saturday|sunday|lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)/i;
    // "Dia N" e o marcador mais forte (aceita qualquer nivel de header)
    const strongDayRegex = /^dia\s*\d+/i;
    const isDayHeader = (s: MenuSection) =>
      strongDayRegex.test(s.title) ||
      ((s.level ?? 99) <= 2 && dayRegex.test(s.title));

    const hasAnyDay = sections.some(isDayHeader);
    if (!hasAnyDay) {
      return [{ title: '', meals: sections }];
    }

    const days: DayGroup[] = [];
    let current: DayGroup | null = null;
    for (const s of sections) {
      if (isDayHeader(s)) {
        current = { title: s.title, meals: [] };
        // Se o header do dia ja tem bullets, tratar como "refeicao unica" sem titulo
        if (s.lines.length) current.meals.push({ title: '', lines: s.lines });
        days.push(current);
      } else {
        if (!current) {
          current = { title: '', meals: [] };
          days.push(current);
        }
        current.meals.push({ title: s.title, lines: s.lines });
      }
    }
    return days;
  }

  sectionIcon(title: string, idx: number): string {
    const t = (title || '').toLowerCase();
    if (t.includes('café') || t.includes('cafe') || t.includes('break')) return 'free_breakfast';
    if (t.includes('almoç') || t.includes('almoc') || t.includes('lunch') || t.includes('déjeuner')) return 'lunch_dining';
    if (t.includes('lanche') || t.includes('snack') || t.includes('collation')) return 'cookie';
    if (t.includes('jantar') || t.includes('dinner') || t.includes('dîner')) return 'dinner_dining';
    if (t.includes('ceia')) return 'nightlight';
    return ['restaurant_menu', 'restaurant', 'local_dining', 'ramen_dining'][idx % 4];
  }
}
