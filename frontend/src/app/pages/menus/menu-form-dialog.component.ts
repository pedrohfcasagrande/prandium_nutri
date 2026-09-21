import { Component, Inject, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { TranslateModule } from '@ngx-translate/core';
import { Food, MenuPlan } from '../../models/models';
import { DatePickerComponent } from '../../components/date-picker.component';
import { ApiService } from '../../services/api.service';

type MealSlot = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER';

@Component({
  selector: 'app-menu-form-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule, MatDialogModule, MatAutocompleteModule,
    TranslateModule, DatePickerComponent
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>add_circle_outline</mat-icon>
      {{ (draft.id ? 'menus.editTitle' : 'menus.newTitle') | translate }}
    </h2>
    <mat-dialog-content class="stack">
      <p class="subtitle text-muted">{{ 'menus.newSubtitle' | translate }}</p>

      <div class="row">
        <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
          <mat-label>{{ 'menus.name' | translate }}</mat-label>
          <input matInput [(ngModel)]="draft.name" [placeholder]="'menus.namePlaceholder' | translate">
        </mat-form-field>
      </div>
      <div class="row">
        <app-date-picker class="grow" [(ngModel)]="draft.startDate" name="startDate"
                         [label]="'menus.startDate' | translate"></app-date-picker>
        <app-date-picker class="grow" [(ngModel)]="draft.endDate" name="endDate"
                         [label]="'menus.endDate' | translate"></app-date-picker>
      </div>

      <!-- Assistente de itens: adiciona uma linha formatada ao content -->
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
            <input matInput [(ngModel)]="itemFoodQuery" (input)="searchFoods()"
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
              @if (aiLoading()) {
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
                  (click)="addItem()"
                  [disabled]="!itemFood || !itemGrams">
            <mat-icon>add</mat-icon>
            {{ 'menus.addItem' | translate }}
          </button>
        </div>
        @if (itemFood && itemGrams) {
          <div class="preview">
            {{ itemFood.name }} · {{ itemGrams }}g · {{ (itemFood.caloriesPer100g * itemGrams / 100) | number:'1.0-0' }} kcal
          </div>
        }
      </div>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>{{ 'menus.content' | translate }}</mat-label>
        <textarea matInput rows="12" [(ngModel)]="draft.content"
                  [placeholder]="'menus.contentHint' | translate"></textarea>
      </mat-form-field>

      <mat-slide-toggle color="primary" [(ngModel)]="draft.active">
        {{ 'menus.setActive' | translate }}
      </mat-slide-toggle>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="cancel()">
        {{ 'app.actions.cancel' | translate }}
      </button>
      <button mat-flat-button color="primary" (click)="save()"
              [disabled]="!draft.name || !draft.content">
        <mat-icon>save</mat-icon>
        {{ 'menus.save' | translate }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] {
      display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
    }
    .subtitle { margin: 0 0 8px; font-size: 0.9rem; }
    .stack { display: flex; flex-direction: column; gap: 14px; min-width: min(90vw, 720px); }
    .row { display: flex; flex-wrap: wrap; gap: 12px; }
    .grow { flex: 1 1 200px; }

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
      grid-template-columns: 140px 1fr 120px auto;
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
export class MenuFormDialogComponent {
  private dialogRef = inject(MatDialogRef<MenuFormDialogComponent, MenuPlan | null>);
  private api = inject(ApiService);

  draft: MenuPlan;

  itemMeal: MealSlot = 'BREAKFAST';
  itemFoodQuery = '';
  itemFood: Food | null = null;
  itemGrams: number | null = null;
  foodOptions = signal<Food[]>([]);
  aiLoading = signal(false);
  private aiLookupTimer: any = null;

  private readonly mealLabels: Record<MealSlot, string> = {
    BREAKFAST: 'Café',
    LUNCH: 'Almoço',
    SNACK: 'Lanche',
    DINNER: 'Jantar'
  };

  constructor(@Inject(MAT_DIALOG_DATA) data: MenuPlan | null) {
    this.draft = data ? { ...data } : this.blank();
  }

  private blank(): MenuPlan {
    return {
      name: '',
      source: 'MANUAL',
      content: '',
      startDate: new Date().toISOString().slice(0, 10),
      active: false
    };
  }

  searchFoods() {
    const q = typeof this.itemFoodQuery === 'string' ? this.itemFoodQuery.trim() : '';
    if (this.aiLookupTimer) { clearTimeout(this.aiLookupTimer); this.aiLookupTimer = null; }
    this.aiLoading.set(false);
    this.api.searchFoods(q).subscribe(f => {
      this.foodOptions.set(f.slice(0, 20));
      // Fallback IA quando busca local nao retornou nada
      if (q.length >= 3 && f.length === 0) {
        this.aiLookupTimer = setTimeout(() => this.runAiLookup(q), 500);
      }
    });
  }

  private runAiLookup(q: string) {
    if (q !== (this.itemFoodQuery ?? '').trim()) return;
    this.aiLoading.set(true);
    this.api.aiLookupFoods(q).subscribe({
      next: sugg => {
        this.aiLoading.set(false);
        if (q !== (this.itemFoodQuery ?? '').trim()) return;
        this.foodOptions.set(sugg);
      },
      error: () => this.aiLoading.set(false)
    });
  }

  displayFood(f: Food | null): string { return f ? f.name : ''; }
  pickFood(f: Food) { this.itemFood = f; }

  addItem() {
    if (!this.itemFood || !this.itemGrams) return;
    const kcal = Math.round(this.itemFood.caloriesPer100g * this.itemGrams / 100);
    const meal = this.mealLabels[this.itemMeal];
    const line = `${meal}: ${this.itemFood.name} — ${this.itemGrams}g (~ ${kcal} kcal)`;
    this.draft.content = this.draft.content
      ? `${this.draft.content.replace(/\s+$/, '')}\n${line}`
      : line;
    // reset
    this.itemFood = null;
    this.itemFoodQuery = '';
    this.itemGrams = null;
    this.foodOptions.set([]);
  }

  save() {
    if (!this.draft.name || !this.draft.content) return;
    this.dialogRef.close(this.draft);
  }

  cancel() {
    this.dialogRef.close(null);
  }
}
