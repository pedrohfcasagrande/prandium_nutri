import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { PracticeService } from '../../services/practice.service';
import { NotificationPollService } from '../../services/notification-poll.service';
import { ClientLink, Food, MenuPlan } from '../../models/models';

type MealSlot = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER';

@Component({
  selector: 'app-nutri-menu-plans',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatTabsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatDividerModule, MatChipsModule, MatTooltipModule,
    MatButtonToggleModule, MatProgressSpinnerModule, MatAutocompleteModule,
    TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriMenus.title' | translate }}</h1>
          <p>{{ 'nutriMenus.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-tab-group animationDuration="150ms">
        <!-- Tab 1: Pendentes de aprovacao -->
        <mat-tab [label]="'nutriMenus.tabPending' | translate">
          @if (pending().length === 0) {
            <div class="empty card-surface">
              <mat-icon>inbox</mat-icon>
              <p>{{ 'nutriMenus.pendingEmpty' | translate }}</p>
            </div>
          } @else {
            <div class="list">
              @for (p of pending(); track p.id) {
                <mat-card class="row-card">
                  <mat-icon class="head-icon">assignment</mat-icon>
                  <div class="info">
                    <div class="name">{{ p.name }}</div>
                    <div class="text-muted small">
                      {{ 'nutriMenus.from' | translate }}: <strong>{{ p.ownerName }}</strong>
                      · {{ p.ownerEmail }}
                    </div>
                    @if (p.submittedAt) {
                      <div class="text-muted small">
                        {{ 'nutriMenus.submittedAt' | translate }}: {{ p.submittedAt | date:'short' }}
                      </div>
                    }
                  </div>
                  <div class="actions">
                    <button mat-flat-button color="primary" (click)="approve(p)">
                      <mat-icon>check</mat-icon>{{ 'nutriMenus.approve' | translate }}
                    </button>
                    <button mat-stroked-button color="warn" (click)="startReject(p)">
                      <mat-icon>close</mat-icon>{{ 'nutriMenus.reject' | translate }}
                    </button>
                  </div>
                  @if (rejectingId() === p.id) {
                    <div class="reject-box">
                      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="grow">
                        <mat-label>{{ 'nutriMenus.rejectReason' | translate }}</mat-label>
                        <input matInput [(ngModel)]="rejectReason">
                      </mat-form-field>
                      <button mat-flat-button color="warn" (click)="confirmReject(p)">{{ 'nutriMenus.confirm' | translate }}</button>
                      <button mat-button (click)="cancelReject()">{{ 'nutriMenus.cancel' | translate }}</button>
                    </div>
                  }
                </mat-card>
              }
            </div>
          }
        </mat-tab>

        <!-- Tab 2: Cardapios dos clientes -->
        <mat-tab [label]="'nutriMenus.tabAuthor' | translate">
          <div class="author-tab">
            <div class="client-picker">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>{{ 'nutriMenus.pickClient' | translate }}</mat-label>
                <mat-select [(ngModel)]="selectedClientId" (selectionChange)="onClientChange()">
                  @for (c of clients(); track c.id) {
                    <mat-option [value]="c.clientId">{{ c.clientName }} · {{ c.clientEmail }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <button mat-flat-button color="primary" [disabled]="!selectedClientId" (click)="newDraft()">
                <mat-icon>add</mat-icon>{{ 'nutriMenus.new' | translate }}
              </button>
            </div>

            @if (selectedClientId) {
              @if (clientMenus().length === 0 && !draft()) {
                <div class="empty card-surface">
                  <mat-icon>menu_book</mat-icon>
                  <p>{{ 'nutriMenus.clientEmpty' | translate }}</p>
                </div>
              } @else {
                <div class="list">
                  @for (m of clientMenus(); track m.id) {
                    <mat-card class="row-card">
                      <mat-icon class="head-icon" [class.active]="m.active">restaurant_menu</mat-icon>
                      <div class="info">
                        <div class="name">
                          {{ m.name }}
                          @if (m.active) {
                            <span class="chip chip-active">{{ 'nutriMenus.chipActive' | translate }}</span>
                          }
                          <span class="chip" [class]="'chip-' + (m.status ?? '').toLowerCase()">{{ ('nutriMenus.status.' + m.status) | translate }}</span>
                        </div>
                        <div class="text-muted small">
                          @if (m.startDate || m.endDate) {
                            {{ 'nutriMenus.period' | translate }}: {{ m.startDate | date:'shortDate' }} - {{ m.endDate | date:'shortDate' }} ·
                          }
                          {{ 'nutriMenus.createdAt' | translate }}: {{ m.createdAt | date:'short' }}
                        </div>
                      </div>
                      <div class="actions">
                        <button mat-stroked-button (click)="editDraft(m)">
                          <mat-icon>edit</mat-icon>{{ 'nutriMenus.edit' | translate }}
                        </button>
                        <button mat-icon-button color="warn" (click)="remove(m)"
                                [matTooltip]="'nutriMenus.delete' | translate">
                          <mat-icon>delete_outline</mat-icon>
                        </button>
                      </div>
                    </mat-card>
                  }
                </div>
              }

              @if (draft(); as d) {
                <mat-card class="editor-card">
                  <div class="editor-hero">
                    <div class="editor-hero-icon"><mat-icon>restaurant_menu</mat-icon></div>
                    <div>
                      <div class="editor-hero-title">
                        {{ (d.id ? 'nutriMenus.editing' : 'nutriMenus.newing') | translate }}
                      </div>
                      <div class="editor-hero-sub">{{ 'nutriMenus.editorSubtitle' | translate }}</div>
                    </div>
                  </div>

                  <div class="editor-body">
                    <!-- 1. Metadados do cardapio (sempre visivel) -->
                    <div class="section-label">{{ 'nutriMenus.sectionBasics' | translate }}</div>
                    <mat-form-field appearance="outline" subscriptSizing="dynamic">
                      <mat-label>{{ 'nutriMenus.name' | translate }}</mat-label>
                      <input matInput [(ngModel)]="d.name" required>
                    </mat-form-field>
                    <div class="two-cols">
                      <mat-form-field appearance="outline" subscriptSizing="dynamic">
                        <mat-label>{{ 'nutriMenus.startDate' | translate }}</mat-label>
                        <input matInput type="date" [(ngModel)]="d.startDate">
                      </mat-form-field>
                      <mat-form-field appearance="outline" subscriptSizing="dynamic">
                        <mat-label>{{ 'nutriMenus.endDate' | translate }}</mat-label>
                        <input matInput type="date" [(ngModel)]="d.endDate">
                      </mat-form-field>
                    </div>

                    <!-- 2. Tabs: Manual vs IA -->
                    <mat-tab-group class="mode-tabs" animationDuration="150ms" [(selectedIndex)]="editorMode">
                      <!-- Manual -->
                      <mat-tab>
                        <ng-template mat-tab-label>
                          <mat-icon>edit_note</mat-icon>
                          {{ 'nutriMenus.tabManual' | translate }}
                        </ng-template>
                        <div class="tab-body">
                          <p class="section-hint">{{ 'nutriMenus.manualHint' | translate }}</p>

                          <!-- Assistente de item: adiciona uma linha formatada ao content -->
                          <div class="add-item-card">
                            <div class="add-item-head">
                              <mat-icon>restaurant</mat-icon>
                              <span>{{ 'menus.addItemTitle' | translate }}</span>
                            </div>
                            <div class="add-item-row">
                              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="meal-select">
                                <mat-label>{{ 'menus.meal' | translate }}</mat-label>
                                <mat-select [(ngModel)]="itemMeal">
                                  <mat-option value="BREAKFAST">{{ 'menus.mealTypes.BREAKFAST' | translate }}</mat-option>
                                  <mat-option value="LUNCH">{{ 'menus.mealTypes.LUNCH' | translate }}</mat-option>
                                  <mat-option value="SNACK">{{ 'menus.mealTypes.SNACK' | translate }}</mat-option>
                                  <mat-option value="DINNER">{{ 'menus.mealTypes.DINNER' | translate }}</mat-option>
                                </mat-select>
                              </mat-form-field>

                              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="food-input">
                                <mat-label>{{ 'menus.food' | translate }}</mat-label>
                                <input matInput [(ngModel)]="itemFoodQuery" (input)="searchItemFoods()"
                                       [matAutocomplete]="foodAuto">
                                <mat-icon matIconSuffix>search</mat-icon>
                                <mat-autocomplete #foodAuto="matAutocomplete"
                                                  (optionSelected)="pickFood($event.option.value)"
                                                  [displayWith]="displayFood">
                                  @for (f of foodOptions(); track (f.id || f.name)) {
                                    <mat-option [value]="f">
                                      <div class="food-option">
                                        <span class="food-name">
                                          {{ f.name }}
                                          @if (f.aiSuggested) {
                                            <span class="ai-badge" [title]="'meals.aiTooltip' | translate">
                                              <mat-icon>auto_awesome</mat-icon> IA
                                            </span>
                                          }
                                        </span>
                                        <span class="kcal">{{ f.caloriesPer100g }} kcal/100g</span>
                                      </div>
                                    </mat-option>
                                  }
                                  @if (foodAiLoading()) {
                                    <mat-option [disabled]="true">
                                      <span class="ai-loading">
                                        <mat-icon>auto_awesome</mat-icon>
                                        {{ 'meals.aiSearching' | translate }}
                                      </span>
                                    </mat-option>
                                  }
                                </mat-autocomplete>
                              </mat-form-field>

                              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="grams-input">
                                <mat-label>{{ 'menus.grams' | translate }}</mat-label>
                                <input matInput type="number" min="1" [(ngModel)]="itemGrams">
                                <span matSuffix>g</span>
                              </mat-form-field>

                              <button mat-flat-button color="primary" class="add-btn"
                                      (click)="addItem(d)"
                                      [disabled]="!itemFood || !itemGrams">
                                <mat-icon>add</mat-icon>
                                {{ 'menus.addItem' | translate }}
                              </button>
                            </div>
                            @if (itemFood && itemGrams) {
                              <div class="preview">
                                {{ itemFood.name }} · {{ itemGrams }}g ·
                                {{ (itemFood.caloriesPer100g * itemGrams / 100) | number:'1.0-0' }} kcal
                              </div>
                            }
                          </div>

                          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="content">
                            <mat-label>{{ 'nutriMenus.content' | translate }}</mat-label>
                            <textarea matInput [(ngModel)]="d.content" rows="14" required
                                      [placeholder]="'nutriMenus.contentHint' | translate"></textarea>
                          </mat-form-field>
                        </div>
                      </mat-tab>

                      <!-- IA -->
                      <mat-tab>
                        <ng-template mat-tab-label>
                          <mat-icon class="ai-tab-icon">auto_awesome</mat-icon>
                          {{ 'nutriMenus.tabAi' | translate }}
                        </ng-template>
                        <div class="tab-body">
                          <p class="section-hint">{{ 'nutriMenus.aiSubtitle' | translate }}</p>
                          <mat-form-field appearance="outline" subscriptSizing="dynamic">
                            <mat-label>{{ 'nutriMenus.aiNotes' | translate }}</mat-label>
                            <textarea matInput rows="2" [(ngModel)]="aiNotes"
                                      [placeholder]="'nutriMenus.aiNotesHint' | translate"></textarea>
                          </mat-form-field>
                          <div class="ai-scope-row">
                            <span class="scope-label">{{ 'nutriMenus.aiScope' | translate }}</span>
                            <mat-button-toggle-group [value]="aiScope" (change)="aiScope = $event.value" hideSingleSelectionIndicator>
                              <mat-button-toggle value="DAY">
                                <mat-icon>today</mat-icon> {{ 'nutriMenus.aiScopeDay' | translate }}
                              </mat-button-toggle>
                              <mat-button-toggle value="WEEK">
                                <mat-icon>date_range</mat-icon> {{ 'nutriMenus.aiScopeWeek' | translate }}
                              </mat-button-toggle>
                            </mat-button-toggle-group>
                          </div>
                          <div class="ai-action-row">
                            <button mat-flat-button color="primary" class="ai-gen-btn"
                                    (click)="generate()" [disabled]="generating()">
                              <span class="ai-gen-btn-content">
                                @if (generating()) {
                                  <mat-spinner diameter="20"></mat-spinner>
                                  <span>{{ 'nutriMenus.aiGenerating' | translate }}</span>
                                } @else {
                                  <mat-icon>auto_awesome</mat-icon>
                                  <span>{{ (aiScope === 'WEEK' ? 'nutriMenus.aiGenerateWeek' : 'nutriMenus.aiGenerateDay') | translate }}</span>
                                }
                              </span>
                            </button>
                          </div>

                          <!-- Preview/edit do conteudo gerado -->
                          <div class="section-label sub">{{ 'nutriMenus.aiPreviewLabel' | translate }}</div>
                          <p class="section-hint">{{ 'nutriMenus.aiPreviewHint' | translate }}</p>
                          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="content">
                            <mat-label>{{ 'nutriMenus.content' | translate }}</mat-label>
                            <textarea matInput [(ngModel)]="d.content" rows="12" required
                                      [placeholder]="'nutriMenus.aiPreviewPlaceholder' | translate"></textarea>
                          </mat-form-field>
                        </div>
                      </mat-tab>
                    </mat-tab-group>
                  </div>

                  <div class="editor-actions">
                    <label class="active-toggle">
                      <input type="checkbox" [(ngModel)]="d.active">
                      {{ 'nutriMenus.setActive' | translate }}
                    </label>
                    <span class="spacer"></span>
                    <button mat-button (click)="cancelEdit()">{{ 'nutriMenus.cancel' | translate }}</button>
                    <button mat-flat-button color="primary" (click)="save()" [disabled]="!d.name || !d.content">
                      {{ 'nutriMenus.save' | translate }}
                    </button>
                  </div>
                </mat-card>
              }
            } @else {
              <div class="empty card-surface">
                <mat-icon>person_search</mat-icon>
                <p>{{ 'nutriMenus.pickClientHint' | translate }}</p>
              </div>
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .list { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }
    .row-card {
      padding: 14px 20px !important;
      display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 16px;
    }
    .head-icon { color: var(--nt-text-muted); }
    .head-icon.active { color: var(--nt-primary); }
    .info { min-width: 0; }
    .name { font-weight: 600; font-size: 1.05rem; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .small { font-size: 0.82rem; }
    .actions { display: flex; gap: 6px; }
    .chip { padding: 2px 10px; border-radius: 999px; font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em; }
    .chip-active { background: #dcfce7; color: #166534; }
    .chip-approved { background: var(--nt-primary-soft); color: var(--nt-primary-dark); }
    .chip-draft { background: #f1f5f9; color: #475569; }
    .chip-pending { background: #fef3c7; color: #92400e; }
    .chip-rejected { background: #fee2e2; color: #991b1b; }
    .reject-box { grid-column: 1 / -1; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding-top: 8px; border-top: 1px solid var(--nt-border); }
    .grow { flex: 1; min-width: 220px; }
    .author-tab { margin-top: 16px; display: flex; flex-direction: column; gap: 16px; }
    .client-picker { display: flex; gap: 12px; align-items: center; }
    .client-picker mat-form-field { flex: 1; max-width: 480px; }
    /* Editor no mesmo padrao visual de /suggestions */
    .editor-card { padding: 0 !important; overflow: hidden; }
    .editor-hero {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: linear-gradient(135deg, #8b5cf6, #6366f1);
      color: #fff;
    }
    .editor-hero-icon {
      width: 48px; height: 48px;
      background: rgba(255,255,255,0.2);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
    .editor-hero-title { font-size: 1.15rem; font-weight: 700; letter-spacing: -0.01em; }
    .editor-hero-sub { font-size: 0.85rem; opacity: 0.9; }
    .editor-body { padding: 24px; display: flex; flex-direction: column; gap: 14px; }
    .editor-body mat-form-field { width: 100%; }
    .two-cols { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    @media (max-width: 600px) { .two-cols { grid-template-columns: 1fr; } }

    .section-label {
      display: inline-flex; align-items: center; gap: 6px;
      font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted); font-weight: 700;
      margin-top: 12px; padding-top: 12px;
      border-top: 1px solid var(--nt-border);
    }
    .section-label:first-child { margin-top: 0; padding-top: 0; border-top: none; }
    .section-label.ai-label mat-icon { color: #8b5cf6; font-size: 18px; width: 18px; height: 18px; }
    .section-hint { margin: 0 0 4px; font-size: 0.85rem; color: var(--nt-text-muted); }

    .ai-scope-row { display: flex; flex-direction: column; align-items: center; gap: 8px; margin-top: 4px; }
    .scope-label {
      font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted);
    }
    .ai-scope-row mat-button-toggle-group { border-radius: 999px; overflow: hidden; }
    .ai-scope-row mat-button-toggle mat-icon {
      font-size: 18px; width: 18px; height: 18px; margin-right: 6px;
      vertical-align: middle;
    }
    .ai-action-row { display: flex; justify-content: center; padding-top: 4px; }
    .ai-gen-btn {
      min-width: 220px;
      height: 48px;
      padding: 0 28px !important;
      border-radius: 999px !important;
      font-weight: 600;
      letter-spacing: 0.01em;
    }
    .ai-gen-btn .ai-gen-btn-content {
      display: inline-flex; align-items: center; justify-content: center;
      gap: 10px; width: 100%;
    }
    .ai-gen-btn mat-icon,
    .ai-gen-btn mat-spinner { margin: 0 !important; flex-shrink: 0; }

    .content textarea { font-family: 'SF Mono', 'Menlo', monospace; font-size: 0.9rem; }
    .editor-actions {
      display: flex; gap: 8px; align-items: center; flex-wrap: wrap;
      padding: 16px 24px;
      background: var(--nt-surface-2);
      border-top: 1px solid var(--nt-border);
    }
    .active-toggle { display: inline-flex; gap: 6px; align-items: center; font-size: 0.9rem; }
    .spacer { flex: 1; }

    /* Tabs internas Manual/IA */
    .mode-tabs { margin-top: 8px; }
    .mode-tabs ::ng-deep .mat-mdc-tab-labels { justify-content: flex-start; }
    .mode-tabs ::ng-deep .mdc-tab {
      padding: 0 16px;
      font-weight: 600;
      mat-icon { font-size: 18px; width: 18px; height: 18px; margin-right: 6px; }
    }
    .mode-tabs ::ng-deep .mdc-tab .ai-tab-icon { color: #8b5cf6; }
    .tab-body {
      padding: 16px 0;
      display: flex; flex-direction: column; gap: 12px;
    }
    .section-label.sub {
      margin-top: 4px; padding-top: 12px;
      border-top: 1px dashed var(--nt-border);
    }

    /* Assistente de item na Manual tab (padrao do dialog de /menus) */
    .add-item-card {
      background: var(--nt-surface-2);
      border: 1px dashed var(--nt-border-strong);
      border-radius: var(--nt-radius);
      padding: 14px;
      display: flex; flex-direction: column; gap: 10px;
    }
    .add-item-head {
      display: flex; align-items: center; gap: 8px;
      font-weight: 600; font-size: 0.9rem; color: var(--nt-text);
      mat-icon { color: var(--nt-primary); font-size: 20px; width: 20px; height: 20px; }
    }
    .add-item-row {
      display: grid;
      grid-template-columns: 160px 1fr 120px auto;
      gap: 10px; align-items: start;
    }
    @media (max-width: 640px) {
      .add-item-row { grid-template-columns: 1fr 1fr; }
      .add-item-row .add-btn { grid-column: 1 / -1; }
    }
    .food-input { min-width: 0; }
    .food-option { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
    .food-option .food-name { display: inline-flex; align-items: center; gap: 6px; }
    .food-option .kcal { color: var(--nt-text-muted); font-size: 0.8rem; }
    .ai-badge {
      display: inline-flex; align-items: center; gap: 3px;
      background: #ede9fe; color: #5b21b6;
      padding: 1px 8px; border-radius: 999px;
      font-size: 0.68rem; font-weight: 700;
      text-transform: uppercase; letter-spacing: 0.03em;
      mat-icon { font-size: 12px; width: 12px; height: 12px; }
    }
    .ai-loading {
      display: inline-flex; align-items: center; gap: 6px;
      color: #5b21b6; font-size: 0.85rem;
      mat-icon { color: #8b5cf6; font-size: 16px; width: 16px; height: 16px;
                 animation: pulse 1.4s ease-in-out infinite; }
    }
    @keyframes pulse { 0%, 100% { opacity: 0.6; } 50% { opacity: 1; } }
    .add-btn { align-self: center; }
    .preview {
      background: var(--nt-primary-soft); color: var(--nt-primary-dark);
      padding: 8px 12px; border-radius: 8px;
      font-size: 0.88rem; font-weight: 500;
    }
  `]
})
export class NutriMenuPlansComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private polls = inject(NotificationPollService);
  private practiceCtx = inject(PracticeService);

  // Pending tab
  pending = signal<MenuPlan[]>([]);
  rejectingId = signal<number | null>(null);
  rejectReason = '';

  // Author tab
  clients = signal<ClientLink[]>([]);
  selectedClientId: number | null = null;
  clientMenus = signal<MenuPlan[]>([]);

  // Assistente IA + modo do editor (0=manual, 1=IA)
  aiNotes = '';
  aiScope: 'WEEK' | 'DAY' = 'WEEK';
  generating = signal(false);
  editorMode = 0;

  // Assistente de item na tab Manual (mesmo padrao do dialog de /menus)
  itemMeal: MealSlot = 'BREAKFAST';
  itemFoodQuery = '';
  itemFood: Food | null = null;
  itemGrams: number | null = null;
  foodOptions = signal<Food[]>([]);
  foodAiLoading = signal(false);
  private foodAiLookupTimer: any = null;
  private readonly mealLabels: Record<MealSlot, string> = {
    BREAKFAST: 'Café da manhã',
    LUNCH: 'Almoço',
    SNACK: 'Lanche',
    DINNER: 'Jantar'
  };
  draft = signal<MenuPlan | null>(null);

  private firstLoad = true;
  constructor() {
    effect(() => {
      this.practiceCtx.activePracticeId();
      if (this.firstLoad) { this.firstLoad = false; return; }
      this.load();
      this.loadClients();
      // Fix: cliente selecionado pode pertencer a outra practice — resetar tudo
      // que depende dele pra evitar vazamento de cardapio entre practices.
      this.selectedClientId = null;
      this.clientMenus.set([]);
      this.draft.set(null);
    }, { allowSignalWrites: true });
  }

  ngOnInit() {
    this.load();
    this.loadClients();
  }

  load() {
    this.api.nutriPendingMenus(this.practiceCtx.activePracticeId()).subscribe(list => this.pending.set(list));
  }

  loadClients() {
    this.api.nutriListClients(this.practiceCtx.activePracticeId(), '', 'name', 0, 200)
        .subscribe(p => {
          this.clients.set(p.items.filter(c => c.status === 'ACTIVE' && c.clientId));
        });
  }

  onClientChange() {
    this.draft.set(null);
    if (this.selectedClientId) this.loadClientMenus();
    else this.clientMenus.set([]);
  }

  loadClientMenus() {
    if (!this.selectedClientId) return;
    this.api.nutriListClientMenus(this.selectedClientId, this.practiceCtx.activePracticeId())
        .subscribe(list => this.clientMenus.set(list));
  }

  newDraft() {
    this.draft.set({
      name: '', source: 'MANUAL', content: '', active: false
    });
    this.editorMode = 0; // Manual por padrao pra novo
  }

  editDraft(m: MenuPlan) {
    this.draft.set({ ...m });
    // Se ja tem conteudo gerado por IA, abre na aba IA
    this.editorMode = m.source === 'AI' ? 1 : 0;
  }

  cancelEdit() { this.draft.set(null); }

  // ----- Assistente de item (Manual tab) -----
  searchItemFoods() {
    const q = typeof this.itemFoodQuery === 'string' ? this.itemFoodQuery.trim() : '';
    if (this.foodAiLookupTimer) { clearTimeout(this.foodAiLookupTimer); this.foodAiLookupTimer = null; }
    this.foodAiLoading.set(false);
    this.api.searchFoods(q).subscribe(f => {
      this.foodOptions.set(f.slice(0, 20));
      if (q.length >= 3 && f.length === 0) {
        this.foodAiLookupTimer = setTimeout(() => this.runItemAiLookup(q), 500);
      }
    });
  }

  private runItemAiLookup(q: string) {
    if (q !== (this.itemFoodQuery ?? '').trim()) return;
    this.foodAiLoading.set(true);
    this.api.aiLookupFoods(q).subscribe({
      next: sugg => {
        this.foodAiLoading.set(false);
        if (q !== (this.itemFoodQuery ?? '').trim()) return;
        this.foodOptions.set(sugg);
      },
      error: () => this.foodAiLoading.set(false)
    });
  }

  displayFood(f: Food | null): string { return f ? f.name : ''; }
  pickFood(f: Food) { this.itemFood = f; }

  addItem(d: MenuPlan) {
    if (!this.itemFood || !this.itemGrams) return;
    const kcal = Math.round(this.itemFood.caloriesPer100g * this.itemGrams / 100);
    const meal = this.mealLabels[this.itemMeal];
    const line = `${meal}: ${this.itemFood.name} — ${this.itemGrams}g (~ ${kcal} kcal)`;
    d.content = d.content ? `${d.content.replace(/\s+$/, '')}\n${line}` : line;
    this.draft.set({ ...d });
    // reset
    this.itemFood = null;
    this.itemFoodQuery = '';
    this.itemGrams = null;
    this.foodOptions.set([]);
  }

  generate() {
    const d = this.draft();
    if (!d || !this.selectedClientId) return;
    const pid = this.practiceCtx.activePracticeId();
    this.generating.set(true);
    this.api.nutriGenerateMenuForClient(this.selectedClientId,
        { extraNotes: this.aiNotes || undefined, scope: this.aiScope }, pid)
      .subscribe({
        next: r => {
          this.generating.set(false);
          const current = this.draft();
          if (!current) return;
          const prev = current.content ? current.content.trimEnd() + '\n\n' : '';
          this.draft.set({ ...current, content: prev + r.menu, source: 'AI' });
          this.snack.open(this.translate.instant('nutriMenus.aiGenerated'), 'OK', { duration: 2500 });
        },
        error: err => {
          this.generating.set(false);
          const msg = err?.error?.error === 'plan_limit_reached'
            ? this.translate.instant('nutriMenus.aiLimitReached')
            : (err?.error?.error ?? this.translate.instant('nutriMenus.aiErr'));
          this.snack.open(msg, 'OK', { duration: 4000 });
        }
      });
  }

  save() {
    const d = this.draft();
    if (!d || !this.selectedClientId) return;
    const pid = this.practiceCtx.activePracticeId();
    const req$ = d.id
      ? this.api.nutriUpdateMenu(d.id, d, pid)
      : this.api.nutriCreateMenuForClient(this.selectedClientId, d, pid);
    req$.subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.saved'), 'OK', { duration: 2500 });
        this.draft.set(null);
        this.loadClientMenus();
        this.polls.refresh();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('nutriMenus.saveErr'),
                                     'OK', { duration: 3000 })
    });
  }

  remove(m: MenuPlan) {
    if (!m.id) return;
    if (!confirm(this.translate.instant('nutriMenus.deleteConfirm', { name: m.name }))) return;
    this.api.nutriDeleteMenu(m.id, this.practiceCtx.activePracticeId()).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.deleted'), 'OK', { duration: 2500 });
        this.loadClientMenus();
      },
      error: () => this.snack.open(this.translate.instant('nutriMenus.saveErr'), 'OK', { duration: 3000 })
    });
  }

  approve(p: MenuPlan) {
    if (!p.id) return;
    this.api.approveMenu(p.id).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.approved'), 'OK', { duration: 2500 });
        this.load();
        this.polls.refresh();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('nutriMenus.error'), 'OK', { duration: 3000 })
    });
  }

  startReject(p: MenuPlan) { this.rejectingId.set(p.id!); this.rejectReason = ''; }
  cancelReject() { this.rejectingId.set(null); this.rejectReason = ''; }
  confirmReject(p: MenuPlan) {
    if (!p.id) return;
    this.api.rejectMenu(p.id, this.rejectReason).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutriMenus.rejected'), 'OK', { duration: 2500 });
        this.cancelReject();
        this.load();
        this.polls.refresh();
      },
      error: () => this.snack.open(this.translate.instant('nutriMenus.error'), 'OK', { duration: 3000 })
    });
  }
}
