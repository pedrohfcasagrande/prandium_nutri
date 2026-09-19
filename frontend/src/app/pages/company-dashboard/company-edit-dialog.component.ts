import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';
import { Company } from '../../models/models';

@Component({
  selector: 'app-company-edit-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDialogModule, TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>corporate_fare</mat-icon>
      {{ 'company.edit.title' | translate }}
    </h2>
    <mat-dialog-content class="stack">
      <div class="row">
        <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
          <mat-label>{{ 'company.edit.name' | translate }}</mat-label>
          <input matInput [(ngModel)]="draft.name">
        </mat-form-field>
        <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
          <mat-label>{{ 'company.edit.tradeName' | translate }}</mat-label>
          <input matInput [(ngModel)]="draft.tradeName">
        </mat-form-field>
      </div>
      <div class="row">
        <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
          <mat-label>{{ 'company.edit.taxId' | translate }}</mat-label>
          <input matInput [(ngModel)]="draft.taxId">
        </mat-form-field>
        <mat-form-field appearance="outline" class="grow country" subscriptSizing="dynamic">
          <mat-label>{{ 'company.edit.country' | translate }}</mat-label>
          <mat-select [(ngModel)]="draft.countryCode">
            <mat-option value="BR">🇧🇷 Brasil</mat-option>
            <mat-option value="US">🇺🇸 United States</mat-option>
            <mat-option value="PT">🇵🇹 Portugal</mat-option>
            <mat-option value="ES">🇪🇸 España</mat-option>
            <mat-option value="FR">🇫🇷 France</mat-option>
            <mat-option value="DE">🇩🇪 Deutschland</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
      <div class="row">
        <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
          <mat-label>{{ 'company.edit.email' | translate }}</mat-label>
          <input matInput type="email" [(ngModel)]="draft.contactEmail">
        </mat-form-field>
        <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
          <mat-label>{{ 'company.edit.phone' | translate }}</mat-label>
          <input matInput [(ngModel)]="draft.contactPhone">
        </mat-form-field>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="ref.close(null)">{{ 'app.actions.cancel' | translate }}</button>
      <button mat-flat-button color="primary" (click)="save()"
              [disabled]="!draft.name || !draft.contactEmail">
        <mat-icon>save</mat-icon>
        {{ 'app.actions.save' | translate }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
    }
    .stack { display: flex; flex-direction: column; gap: 12px; min-width: min(90vw, 560px); }
    .row { display: flex; flex-wrap: wrap; gap: 12px; }
    .grow { flex: 1 1 220px; }
    .country { flex: 0 0 180px; }
  `]
})
export class CompanyEditDialogComponent {
  draft: Company;

  constructor(
    public ref: MatDialogRef<CompanyEditDialogComponent, Company | null>,
    @Inject(MAT_DIALOG_DATA) data: Company
  ) {
    this.draft = { ...data };
  }

  save() {
    if (!this.draft.name || !this.draft.contactEmail) return;
    this.ref.close(this.draft);
  }
}
