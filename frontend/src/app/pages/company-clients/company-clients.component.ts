import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { CompanyClient, CompanyNutriLink } from '../../models/models';

@Component({
  selector: 'app-company-clients',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatTableModule,
    MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'companyClients.title' | translate }}</h1>
          <p>{{ 'companyClients.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-form-field appearance="outline" class="search" subscriptSizing="dynamic">
        <mat-label>{{ 'companyClients.search' | translate }}</mat-label>
        <mat-icon matPrefix>search</mat-icon>
        <input matInput [(ngModel)]="query" [placeholder]="'companyClients.searchHint' | translate">
      </mat-form-field>

      @if (filtered().length === 0) {
        <div class="empty card-surface">
          <mat-icon>groups</mat-icon>
          <p>{{ 'companyClients.empty' | translate }}</p>
        </div>
      } @else {
        <mat-card class="table-card">
          <table mat-table [dataSource]="filtered()">
            <ng-container matColumnDef="client">
              <th mat-header-cell *matHeaderCellDef>{{ 'companyClients.client' | translate }}</th>
              <td mat-cell *matCellDef="let r">
                <div class="cli-cell">
                  <div class="avatar">{{ (r.clientName || '?')[0].toUpperCase() }}</div>
                  <div class="min-0">
                    <div><strong>{{ r.clientName }}</strong></div>
                    <div class="text-muted small">{{ r.clientEmail }}</div>
                  </div>
                </div>
              </td>
            </ng-container>
            <ng-container matColumnDef="nutri">
              <th mat-header-cell *matHeaderCellDef>{{ 'companyClients.currentNutri' | translate }}</th>
              <td mat-cell *matCellDef="let r">{{ r.nutriName }}</td>
            </ng-container>
            <ng-container matColumnDef="lastMeal">
              <th mat-header-cell *matHeaderCellDef>{{ 'companyClients.lastMeal' | translate }}</th>
              <td mat-cell *matCellDef="let r">
                {{ r.lastMealDate ? (r.lastMealDate | date:'shortDate') : ('companyClients.never' | translate) }}
              </td>
            </ng-container>
            <ng-container matColumnDef="action">
              <th mat-header-cell *matHeaderCellDef>{{ 'companyClients.transfer' | translate }}</th>
              <td mat-cell *matCellDef="let r">
                <div class="transfer-cell">
                  <mat-form-field appearance="outline" subscriptSizing="dynamic" class="transfer-select">
                    <mat-select #sel [value]="r.nutriId">
                      @for (n of activeNutris(); track n.id) {
                        <mat-option [value]="n.id">{{ n.name }}</mat-option>
                      }
                    </mat-select>
                  </mat-form-field>
                  <button mat-flat-button color="primary"
                          (click)="transfer(r, sel.value)"
                          [disabled]="sel.value === r.nutriId">
                    <mat-icon>swap_horiz</mat-icon>
                    {{ 'companyClients.transferBtn' | translate }}
                  </button>
                </div>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let r; columns: cols"></tr>
          </table>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .search { width: 100%; max-width: 480px; margin-bottom: 20px; }
    .table-card { padding: 0 !important; overflow-x: auto; }
    table { width: 100%; }
    th, td { padding: 12px 16px !important; color: var(--nt-text) !important;
             border-color: var(--nt-border) !important; }
    th { color: var(--nt-text-muted) !important; font-weight: 600 !important;
         background: var(--nt-surface-2); }
    .cli-cell { display: flex; align-items: center; gap: 12px; }
    .min-0 { min-width: 0; }
    .avatar {
      width: 40px; height: 40px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff; display: flex; align-items: center; justify-content: center;
      font-weight: 700; flex-shrink: 0;
    }
    .transfer-cell { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .transfer-select { width: 200px; }
    .small { font-size: 0.85rem; }
  `]
})
export class CompanyClientsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  clients = signal<CompanyClient[]>([]);
  nutris = signal<CompanyNutriLink[]>([]);
  query = '';
  cols = ['client', 'nutri', 'lastMeal', 'action'];

  activeNutris = computed(() =>
    this.nutris().filter(n => n.status === 'ACTIVE' && n.nutritionistId)
                 .map(n => ({ id: n.nutritionistId!, name: n.nutritionistName })));

  filtered = computed(() => {
    const q = this.query.trim().toLowerCase();
    if (!q) return this.clients();
    return this.clients().filter(c =>
      c.clientName.toLowerCase().includes(q)
      || c.clientEmail.toLowerCase().includes(q)
      || c.nutriName.toLowerCase().includes(q));
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.companyListClients().subscribe(c => this.clients.set(c));
    this.api.companyNutritionists().subscribe(n => this.nutris.set(n));
  }

  transfer(r: CompanyClient, targetNutriId: number) {
    if (targetNutriId === r.nutriId) return;
    this.api.companyTransferClient(r.clientId, targetNutriId).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('companyClients.transferred'), 'OK', { duration: 2500 });
        this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('companyClients.transferErr'),
                                     'OK', { duration: 3000 })
    });
  }
}
