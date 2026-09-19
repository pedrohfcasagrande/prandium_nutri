import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { MenuPlan } from '../../models/models';
import { DatePickerComponent } from '../../components/date-picker.component';

@Component({
  selector: 'app-menu-form-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatDialogModule, TranslateModule, DatePickerComponent
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>add_circle_outline</mat-icon>
      {{ 'menus.newTitle' | translate }}
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
    .stack { display: flex; flex-direction: column; gap: 14px; min-width: min(90vw, 640px); }
    .row { display: flex; flex-wrap: wrap; gap: 12px; }
    .grow { flex: 1 1 200px; }
  `]
})
export class MenuFormDialogComponent {
  private dialogRef = inject(MatDialogRef<MenuFormDialogComponent, MenuPlan | null>);
  draft: MenuPlan;

  constructor(@Inject(MAT_DIALOG_DATA) data: MenuPlan | null) {
    this.draft = data ?? this.blank();
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

  save() {
    if (!this.draft.name || !this.draft.content) return;
    this.dialogRef.close(this.draft);
  }

  cancel() {
    this.dialogRef.close(null);
  }
}
