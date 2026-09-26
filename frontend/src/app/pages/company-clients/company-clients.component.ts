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
import { SearchSortBarComponent } from '../../components/search-sort-bar.component';
import { EmptyStateComponent } from '../../components/empty-state.component';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

@Component({
  selector: 'app-company-clients',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatTableModule,
    MatSnackBarModule, TranslateModule,
    SearchSortBarComponent, MatPaginatorModule, EmptyStateComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'companyClients.title' | translate }}</h1>
          <p>{{ 'companyClients.subtitle' | translate }}</p>
        </div>
      </div>

      <app-search-sort-bar [placeholder]="'search.placeholderClients' | translate"
                           (search)="onSearch($event)" (sortChange)="onSort($event)"></app-search-sort-bar>

      @if (clients().length === 0) {
        <app-empty-state type="clients"
                         [description]="'companyClients.empty' | translate"></app-empty-state>
      } @else {
        <div class="list">
          @for (r of clients(); track r.linkId; let i = $index) {
            <mat-card class="row-card active stagger-item" [style.--i]="i">
              <div class="avatar">{{ (r.clientName || '?')[0].toUpperCase() }}</div>
              <div class="info">
                <div class="name">{{ r.clientName }}</div>
                <div class="email">
                  {{ r.clientEmail }}
                  @if (r.clientCpf) { <span class="cpf-inline">· {{ r.clientCpf }}</span> }
                </div>
                <div class="meta">
                  <span class="status-chip chip-active">
                    <mat-icon>medical_services</mat-icon> {{ r.nutriName }}
                  </span>
                  <span class="text-muted small">
                    {{ 'companyClients.lastMeal' | translate }}:
                    {{ r.lastMealDate ? (r.lastMealDate | date:'shortDate') : ('companyClients.never' | translate) }}
                  </span>
                </div>
              </div>
              <div class="actions">
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
            </mat-card>
          }
        </div>
        <mat-paginator [length]="total()" [pageSize]="pageSize" [pageIndex]="pageIndex"
                       [pageSizeOptions]="[10, 25, 50]" (page)="onPage($event)"
                       showFirstLastButtons></mat-paginator>
      }
    </div>
  `,
  styles: [`
    .list { display: flex; flex-direction: column; gap: 12px; }
    .row-card {
      padding: 16px 20px !important;
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 16px;
    }
    .row-card.active { border-left: 4px solid var(--nt-primary) !important; }
    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff; display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 1.1rem; flex-shrink: 0;
    }
    .info { min-width: 0; }
    .name { font-weight: 600; font-size: 1.05rem; }
    .cpf-inline { font-weight: 400; font-size: 0.85em;
      color: var(--nt-text-muted); font-variant-numeric: tabular-nums; margin-left: 2px; }
    .email { color: var(--nt-text-muted); font-size: 0.9rem; margin: 2px 0 6px;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .meta { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .small { font-size: 0.8rem; }

    .status-chip {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 4px 12px; border-radius: 999px;
      font-weight: 600; font-size: 0.72rem;
      text-transform: uppercase; letter-spacing: 0.03em;
      mat-icon { font-size: 14px; width: 14px; height: 14px; }
    }
    .chip-active { background: #d1fae5; color: #065f46; }

    .actions { display: flex; gap: 8px; align-items: center; justify-self: end; flex-wrap: wrap; }
    .transfer-select { width: 200px; }

    @media (max-width: 720px) {
      .row-card { grid-template-columns: auto 1fr; }
      .actions { grid-column: 1 / -1; justify-self: stretch; }
      .transfer-select { flex: 1; }
    }
  `]
})
export class CompanyClientsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  clients = signal<CompanyClient[]>([]);
  nutris = signal<CompanyNutriLink[]>([]);
  query = '';
  sort: 'created' | 'name' = 'name';
  pageIndex = 0;
  pageSize = 10;
  total = signal(0);

  activeNutris = computed(() =>
    this.nutris().filter(n => n.status === 'ACTIVE' && n.nutritionistId)
                 .map(n => ({ id: n.nutritionistId!, name: n.nutritionistName })));

  ngOnInit() { this.load(); }

  onSearch(q: string) { this.query = q; this.pageIndex = 0; this.load(); }
  onSort(s: 'created' | 'name') { this.sort = s; this.pageIndex = 0; this.load(); }
  onPage(e: PageEvent) { this.pageIndex = e.pageIndex; this.pageSize = e.pageSize; this.load(); }

  load() {
    this.api.companyListClients(this.query, this.sort, this.pageIndex, this.pageSize)
        .subscribe(p => { this.clients.set(p.items); this.total.set(p.total); });
    // Nutris pra transfer dropdown — sem paginação (todos os ativos)
    this.api.companyNutritionists(undefined, undefined, 0, 1000)
        .subscribe(p => this.nutris.set(p.items));
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
