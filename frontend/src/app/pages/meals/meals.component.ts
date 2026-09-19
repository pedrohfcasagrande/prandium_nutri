import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { Food, Meal, MealType } from '../../models/models';
import { DatePickerComponent } from '../../components/date-picker.component';

@Component({
  selector: 'app-meals',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatAutocompleteModule,
    MatDividerModule, MatButtonToggleModule, MatSnackBarModule, TranslateModule,
    DatePickerComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'meals.title' | translate }}</h1>
          <p>{{ 'meals.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-card class="new-meal">
        <div class="new-meal-head" [class]="'type-' + draft.type.toLowerCase()">
          <div class="type-icon"><mat-icon>{{ typeIcon(draft.type) }}</mat-icon></div>
          <div class="type-info">
            <div class="type-label">{{ 'meals.newMeal' | translate }}</div>
            <div class="type-value">{{ 'meals.types.' + draft.type | translate }}</div>
          </div>
        </div>

        <div class="new-meal-body">
          <div class="type-selector">
            <mat-button-toggle-group [(value)]="draft.type" hideSingleSelectionIndicator>
              @for (t of allTypes; track t) {
                <mat-button-toggle [value]="t">
                  <mat-icon>{{ typeIcon(t) }}</mat-icon>
                  <span class="toggle-label">{{ 'meals.types.' + t | translate }}</span>
                </mat-button-toggle>
              }
            </mat-button-toggle-group>
          </div>

          <div class="datetime-row">
            <app-date-picker [(ngModel)]="draft.date" [label]="'meals.date' | translate"></app-date-picker>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>{{ 'meals.time' | translate }}</mat-label>
              <input matInput type="time" [(ngModel)]="draft.time">
              <mat-icon matSuffix>schedule</mat-icon>
            </mat-form-field>
          </div>

          <mat-divider></mat-divider>

          <div class="add-food-row">
            <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
              <mat-label>{{ 'meals.searchFood' | translate }}</mat-label>
              <input matInput [(ngModel)]="foodQuery" (input)="searchFoods()"
                     [matAutocomplete]="auto" [placeholder]="'meals.searchHint' | translate">
              <mat-icon matPrefix>search</mat-icon>
              <mat-autocomplete #auto="matAutocomplete"
                                (optionSelected)="selectFood($event.option.value)"
                                [displayWith]="displayFood">
                @for (f of foodOptions(); track f.id) {
                  <mat-option [value]="f">
                    <div class="food-option">
                      <span class="food-name">{{ f.name }}</span>
                      <span class="food-kcal">{{ 'meals.kcalPer100g' | translate:{ value: f.caloriesPer100g } }}</span>
                    </div>
                  </mat-option>
                }
              </mat-autocomplete>
            </mat-form-field>

            <mat-form-field appearance="outline" class="grams" subscriptSizing="dynamic">
              <mat-label>{{ 'meals.grams' | translate }}</mat-label>
              <input matInput type="number" [(ngModel)]="pendingGrams" min="0">
              <span matSuffix>g</span>
            </mat-form-field>

            <button mat-flat-button color="primary" class="add-btn"
                    (click)="addItem()" [disabled]="!pendingFood || !pendingGrams">
              <mat-icon>add</mat-icon>
              <span>{{ 'meals.add' | translate }}</span>
            </button>
          </div>

          @if (draft.items.length) {
            <div class="preview-block">
              <div class="preview-header">
                <span class="preview-label">{{ 'meals.preview' | translate }}</span>
                <span class="preview-totals">
                  <strong>{{ previewKcal() | number:'1.0-0' }}</strong> kcal ·
                  <strong>{{ previewProtein() | number:'1.0-1' }}</strong> g {{ 'meals.protein' | translate }}
                </span>
              </div>
              <div class="items-list">
                @for (it of draft.items; track $index) {
                  <div class="item-row">
                    <div class="item-info">
                      <span class="item-name">{{ it.foodName }}</span>
                      <span class="item-meta">{{ it.grams }}g · {{ itemKcal(it) | number:'1.0-0' }} kcal</span>
                    </div>
                    <button mat-icon-button class="item-remove" (click)="removeItem($index)">
                      <mat-icon>close</mat-icon>
                    </button>
                  </div>
                }
              </div>
            </div>
          } @else {
            <div class="preview-empty">
              <mat-icon>egg_alt</mat-icon>
              <span>{{ 'meals.noItems' | translate }}</span>
            </div>
          }

          <div class="save-row">
            <button mat-flat-button color="primary" class="save-btn"
                    (click)="save()" [disabled]="!draft.items.length">
              <mat-icon>check</mat-icon>
              <span>{{ 'meals.save' | translate }}</span>
            </button>
          </div>
        </div>
      </mat-card>

      <div class="page-header history-header">
        <div><h2>{{ 'meals.history' | translate }}</h2></div>
        <app-date-picker class="date-picker" [(ngModel)]="filterDate"
                         (ngModelChange)="load()" [label]="'meals.date' | translate"></app-date-picker>
      </div>

      @if (!meals().length) {
        <div class="empty card-surface">
          <mat-icon>restaurant</mat-icon>
          <p>{{ 'meals.empty' | translate }}</p>
        </div>
      } @else {
        <div class="meal-list">
          @for (m of meals(); track m.id) {
            <mat-card class="meal-card" [class]="'type-' + m.type.toLowerCase()">
              <div class="meal-card-side"></div>
              <div class="meal-card-content">
                <div class="meal-card-head">
                  <div class="meal-badge">
                    <mat-icon>{{ typeIcon(m.type) }}</mat-icon>
                    <div>
                      <div class="meal-title">{{ 'meals.types.' + m.type | translate }}</div>
                      @if (m.time) { <div class="meal-time">{{ m.time }}</div> }
                    </div>
                  </div>
                  <div class="meal-totals">
                    <div class="total-item">
                      <div class="total-value">{{ m.totalCalories | number:'1.0-0' }}</div>
                      <div class="total-label">kcal</div>
                    </div>
                    <div class="total-item">
                      <div class="total-value">{{ m.totalProtein | number:'1.0-1' }}</div>
                      <div class="total-label">g prot</div>
                    </div>
                    <button mat-icon-button color="warn" (click)="remove(m)"
                            [attr.aria-label]="'meals.remove' | translate">
                      <mat-icon>delete_outline</mat-icon>
                    </button>
                  </div>
                </div>
                <mat-divider></mat-divider>
                <div class="items-list compact">
                  @for (it of m.items; track it.id) {
                    <div class="item-row">
                      <div class="item-info">
                        <span class="item-name">{{ it.foodName }}</span>
                        <span class="item-meta">
                          {{ it.grams }}g · {{ it.calories | number:'1.0-0' }} kcal · {{ it.protein | number:'1.0-1' }}g prot
                        </span>
                      </div>
                    </div>
                  }
                </div>
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .type-breakfast { --meal-c1: #f59e0b; --meal-c2: #d97706; }
    .type-lunch     { --meal-c1: #0d9488; --meal-c2: #0f766e; }
    .type-snack     { --meal-c1: #8b5cf6; --meal-c2: #7c3aed; }
    .type-dinner    { --meal-c1: #3b82f6; --meal-c2: #2563eb; }
    .type-other     { --meal-c1: #64748b; --meal-c2: #475569; }

    .new-meal { padding: 0 !important; overflow: hidden; }

    .new-meal-head {
      display: flex; align-items: center; gap: 16px;
      padding: 20px 24px;
      background: linear-gradient(135deg, var(--meal-c1), var(--meal-c2));
      color: #fff;
    }
    .type-icon {
      width: 48px; height: 48px;
      background: rgba(255,255,255,0.2);
      border-radius: 12px;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
    .type-label { font-size: 0.8rem; opacity: 0.85; text-transform: uppercase; letter-spacing: 0.05em; }
    .type-value { font-size: 1.35rem; font-weight: 700; letter-spacing: -0.01em; }

    .new-meal-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }

    .type-selector {
      overflow-x: auto;
      mat-button-toggle-group { flex-wrap: nowrap; }
      mat-button-toggle { min-width: 100px; }
      .toggle-label { margin-left: 6px; }
    }
    @media (max-width: 600px) {
      .type-selector .toggle-label { display: none; }
      .type-selector mat-button-toggle { min-width: 56px; }
    }

    .datetime-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 12px;
    }

    .add-food-row {
      display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap;
    }
    .grow { flex: 1 1 260px; }
    .grams { flex: 0 0 160px; }
    .add-btn { height: 56px; padding: 0 20px; display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; }

    .food-option { display: flex; justify-content: space-between; gap: 16px; align-items: center; width: 100%; }
    .food-name { font-weight: 500; }
    .food-kcal { color: var(--nt-text-muted); font-size: 0.85rem; }

    .preview-block {
      background: var(--nt-surface-2);
      border: 1px solid var(--nt-border);
      border-radius: 12px;
      padding: 16px;
      display: flex; flex-direction: column; gap: 12px;
    }
    .preview-header {
      display: flex; justify-content: space-between; align-items: baseline; flex-wrap: wrap; gap: 8px;
    }
    .preview-label {
      font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--nt-text-muted);
    }
    .preview-totals strong { color: var(--nt-primary-dark); font-weight: 700; }

    .items-list { display: flex; flex-direction: column; gap: 6px; }
    .items-list.compact { padding-top: 12px; }
    .item-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 8px 10px;
      border-radius: 8px;
      background: var(--nt-surface);
      border: 1px solid var(--nt-border);
    }
    .items-list.compact .item-row { background: transparent; border: none; padding: 4px 0; }
    .item-info { display: flex; flex-direction: column; gap: 2px; }
    .item-name { font-weight: 500; }
    .item-meta { font-size: 0.8rem; color: var(--nt-text-muted); }
    .item-remove { width: 32px !important; height: 32px !important; }

    .preview-empty {
      display: flex; align-items: center; justify-content: center; gap: 10px;
      padding: 32px 16px;
      color: var(--nt-text-muted);
      font-size: 0.9rem;
      border: 2px dashed var(--nt-border);
      border-radius: 12px;
      mat-icon { opacity: 0.6; }
    }

    .save-row { display: flex; justify-content: flex-end; }
    .save-btn { height: 46px; padding: 0 24px; display: inline-flex; align-items: center; gap: 4px; }

    .history-header h2 {
      font-size: 1.35rem; margin: 0; font-weight: 600; letter-spacing: -0.01em;
      color: var(--nt-text);
    }
    .date-picker { width: 200px; }
    @media (max-width: 600px) { .date-picker { width: 100%; } }

    .meal-list { display: flex; flex-direction: column; gap: 12px; }
    .meal-card {
      padding: 0 !important; overflow: hidden;
      display: flex; align-items: stretch; gap: 0;
    }
    .meal-card-side { width: 6px; background: linear-gradient(180deg, var(--meal-c1), var(--meal-c2)); }
    .meal-card-content { flex: 1; padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; }

    .meal-card-head {
      display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap;
    }
    .meal-badge { display: flex; align-items: center; gap: 12px; }
    .meal-badge mat-icon {
      color: var(--meal-c1); font-size: 28px; width: 28px; height: 28px;
    }
    .meal-title { font-weight: 600; font-size: 1.05rem; }
    .meal-time { color: var(--nt-text-muted); font-size: 0.85rem; }

    .meal-totals { display: flex; align-items: center; gap: 16px; }
    .total-item { text-align: center; }
    .total-value { font-weight: 700; font-size: 1.1rem; color: var(--nt-text); }
    .total-label { font-size: 0.75rem; color: var(--nt-text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
  `]
})
export class MealsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  meals = signal<Meal[]>([]);
  foodOptions = signal<Food[]>([]);
  filterDate = new Date().toISOString().slice(0, 10);
  foodQuery = '';
  pendingFood: Food | null = null;
  pendingGrams: number | null = null;
  draft: Meal = this.blankDraft();

  allTypes: MealType[] = ['BREAKFAST', 'LUNCH', 'SNACK', 'DINNER', 'OTHER'];

  previewKcal = () => this.draft.items.reduce((s, it) => s + this.itemKcal(it), 0);
  previewProtein = () => this.draft.items.reduce((s, it) => s + this.itemProtein(it), 0);

  ngOnInit(): void { this.load(); }

  blankDraft(): Meal {
    return {
      date: new Date().toISOString().slice(0, 10),
      time: new Date().toTimeString().slice(0, 5),
      type: 'LUNCH' as MealType,
      items: []
    };
  }

  load() { this.api.listMeals(this.filterDate).subscribe(m => this.meals.set(m)); }

  searchFoods() {
    const q = typeof this.foodQuery === 'string' ? this.foodQuery : '';
    this.api.searchFoods(q).subscribe(f => this.foodOptions.set(f.slice(0, 20)));
  }

  displayFood(f: Food | null): string { return f ? f.name : ''; }
  selectFood(f: Food) { this.pendingFood = f; }

  addItem() {
    if (!this.pendingFood || !this.pendingGrams) return;
    const factor = this.pendingGrams / 100;
    this.draft.items.push({
      foodId: this.pendingFood.id!,
      foodName: this.pendingFood.name,
      grams: this.pendingGrams,
      calories: this.pendingFood.caloriesPer100g * factor,
      protein: this.pendingFood.proteinPer100g * factor
    });
    this.pendingFood = null; this.pendingGrams = null; this.foodQuery = '';
  }

  itemKcal(it: any): number { return it.calories ?? 0; }
  itemProtein(it: any): number { return it.protein ?? 0; }

  removeItem(i: number) { this.draft.items.splice(i, 1); }

  save() {
    this.api.saveMeal(this.draft).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('meals.saved'), 'OK', { duration: 2000 });
        this.draft = this.blankDraft();
        this.load();
      },
      error: () => this.snack.open(this.translate.instant('meals.saveFail'), 'OK', { duration: 3000 })
    });
  }

  remove(m: Meal) {
    if (!m.id) return;
    this.api.deleteMeal(m.id).subscribe(() => this.load());
  }

  typeIcon(t: MealType): string {
    return { BREAKFAST: 'free_breakfast', LUNCH: 'lunch_dining', SNACK: 'cookie',
             DINNER: 'dinner_dining', OTHER: 'restaurant' }[t];
  }
}
