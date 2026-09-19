import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';

export interface RevokeDialogData {
  nutriName: string;
  clientsCount: number;
  otherNutris: { id: number; name: string }[];
}

export interface RevokeDialogResult {
  action: 'REVOKE_CLIENTS' | 'TRANSFER';
  targetNutriId?: number;
}

@Component({
  selector: 'app-revoke-nutri-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatRadioModule, MatFormFieldModule, MatSelectModule,
    MatDialogModule, TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>link_off</mat-icon>
      {{ 'company.nutris.revokeDialogTitle' | translate:{ name: data.nutriName } }}
    </h2>
    <mat-dialog-content class="stack">
      <p class="hint">
        {{ 'company.nutris.revokeDialogHint' | translate:{ count: data.clientsCount } }}
      </p>

      <mat-radio-group [(ngModel)]="action" class="options">
        <mat-radio-button value="REVOKE_CLIENTS">
          <div class="opt-label">
            <strong>{{ 'company.nutris.optRevoke' | translate }}</strong>
            <div class="text-muted small">{{ 'company.nutris.optRevokeHint' | translate }}</div>
          </div>
        </mat-radio-button>
        <mat-radio-button value="TRANSFER" [disabled]="!data.otherNutris.length">
          <div class="opt-label">
            <strong>{{ 'company.nutris.optTransfer' | translate }}</strong>
            <div class="text-muted small">
              @if (data.otherNutris.length) {
                {{ 'company.nutris.optTransferHint' | translate }}
              } @else {
                {{ 'company.nutris.optTransferUnavailable' | translate }}
              }
            </div>
          </div>
        </mat-radio-button>
      </mat-radio-group>

      @if (action === 'TRANSFER') {
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'company.nutris.targetNutriLabel' | translate }}</mat-label>
          <mat-select [(ngModel)]="targetNutriId">
            @for (n of data.otherNutris; track n.id) {
              <mat-option [value]="n.id">{{ n.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="ref.close(null)">{{ 'app.actions.cancel' | translate }}</button>
      <button mat-flat-button color="warn" (click)="confirm()"
              [disabled]="action === 'TRANSFER' && !targetNutriId">
        <mat-icon>link_off</mat-icon>
        {{ 'company.nutris.confirmRevoke' | translate }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] { display: flex; align-items: center; gap: 8px; mat-icon { color: #ef4444; } }
    .stack { display: flex; flex-direction: column; gap: 16px; min-width: min(90vw, 520px); }
    .hint { margin: 0; color: var(--nt-text-muted); }
    .options { display: flex; flex-direction: column; gap: 12px; }
    .opt-label { display: inline-flex; flex-direction: column; gap: 2px; }
    .small { font-size: 0.85rem; }
    mat-form-field { width: 100%; }
  `]
})
export class RevokeNutriDialogComponent {
  action: 'REVOKE_CLIENTS' | 'TRANSFER' = 'REVOKE_CLIENTS';
  targetNutriId?: number;

  constructor(
    public ref: MatDialogRef<RevokeNutriDialogComponent, RevokeDialogResult | null>,
    @Inject(MAT_DIALOG_DATA) public data: RevokeDialogData
  ) {}

  confirm() {
    if (this.action === 'TRANSFER' && !this.targetNutriId) return;
    this.ref.close({ action: this.action, targetNutriId: this.targetNutriId });
  }
}
