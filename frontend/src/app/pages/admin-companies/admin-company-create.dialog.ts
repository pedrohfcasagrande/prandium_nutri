import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';

@Component({
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>corporate_fare</mat-icon> {{ 'adminCompanies.dialog.title' | translate }}
    </h2>
    <mat-dialog-content>
      <p class="text-muted">{{ 'adminCompanies.dialog.subtitle' | translate }}</p>

      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ 'registerCompany.companyName' | translate }}</mat-label>
        <input matInput [(ngModel)]="name">
      </mat-form-field>

      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ 'registerCompany.tradeName' | translate }}</mat-label>
        <input matInput [(ngModel)]="tradeName">
      </mat-form-field>

      <div class="row">
        <mat-form-field appearance="outline" class="grow">
          <mat-label>{{ 'registerCompany.taxId' | translate }}</mat-label>
          <input matInput [(ngModel)]="taxId">
        </mat-form-field>

        <mat-form-field appearance="outline" class="country">
          <mat-label>{{ 'registerCompany.country' | translate }}</mat-label>
          <mat-select [(ngModel)]="countryCode">
            <mat-option value="BR">Brasil</mat-option>
            <mat-option value="PT">Portugal</mat-option>
            <mat-option value="US">United States</mat-option>
            <mat-option value="FR">France</mat-option>
            <mat-option value="ES">España</mat-option>
            <mat-option value="DE">Deutschland</mat-option>
            <mat-option value="IT">Italia</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ 'adminCompanies.dialog.contactEmail' | translate }}</mat-label>
        <input matInput type="email" [(ngModel)]="contactEmail">
        <mat-icon matPrefix>mail</mat-icon>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ 'registerCompany.contactPhone' | translate }}</mat-label>
        <input matInput [(ngModel)]="contactPhone">
        <mat-icon matPrefix>call</mat-icon>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full">
        <mat-label>{{ 'adminCompanies.dialog.ownerEmail' | translate }}</mat-label>
        <input matInput type="email" [(ngModel)]="ownerEmail">
        <mat-icon matPrefix>person</mat-icon>
      </mat-form-field>

      <p class="hint text-muted">
        <mat-icon>info</mat-icon>
        {{ 'adminCompanies.dialog.hint' | translate }}
      </p>

      @if (error()) {
        <div class="alert-error"><mat-icon>error_outline</mat-icon> {{ error() }}</div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="close(false)">{{ 'app.actions.cancel' | translate }}</button>
      <button mat-flat-button color="primary" (click)="submit()" [disabled]="loading()">
        <mat-icon>send</mat-icon> {{ 'adminCompanies.dialog.submit' | translate }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] {
      display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
    }
    mat-dialog-content { display: flex; flex-direction: column; gap: 4px; }
    .full { width: 100%; }
    .row { display: flex; gap: 12px; }
    .grow { flex: 1; }
    .country { width: 140px; }
    .hint {
      display: flex; align-items: center; gap: 8px; margin: 8px 0 0;
      font-size: 0.85rem;
      mat-icon { color: var(--nt-primary); }
    }
  `]
})
export class AdminCompanyCreateDialog {
  private ref = inject(MatDialogRef<AdminCompanyCreateDialog>);
  private api = inject(ApiService);
  private translate = inject(TranslateService);

  name = ''; tradeName = ''; taxId = ''; countryCode = 'BR';
  contactEmail = ''; contactPhone = ''; ownerEmail = '';
  loading = signal(false);
  error = signal<string | null>(null);

  submit() {
    this.error.set(null);
    if (!this.name || !this.contactEmail || !this.ownerEmail) {
      this.error.set(this.translate.instant('adminCompanies.dialog.emptyFields')); return;
    }
    this.loading.set(true);
    this.api.adminCreateCompany({
      name: this.name, tradeName: this.tradeName, taxId: this.taxId,
      countryCode: this.countryCode, contactEmail: this.contactEmail,
      contactPhone: this.contactPhone, ownerEmail: this.ownerEmail
    }).subscribe({
      next: () => { this.loading.set(false); this.ref.close(true); },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.error ?? 'Erro ao criar empresa');
      }
    });
  }

  close(created: boolean) { this.ref.close(created); }
}
