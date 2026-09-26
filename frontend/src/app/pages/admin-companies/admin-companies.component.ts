import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { AdminCompany } from '../../models/models';
import { AdminCompanyCreateDialog } from './admin-company-create.dialog';
import { SearchSortBarComponent } from '../../components/search-sort-bar.component';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

@Component({
  selector: 'app-admin-companies',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatTableModule, MatChipsModule,
    MatTooltipModule, MatSlideToggleModule, MatDialogModule, MatSnackBarModule,
    TranslateModule, SearchSortBarComponent, MatPaginatorModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'adminCompanies.title' | translate }}</h1>
          <p>{{ 'adminCompanies.subtitle' | translate }}</p>
        </div>
        <button mat-flat-button color="primary" (click)="openCreate()">
          <mat-icon>add</mat-icon> {{ 'adminCompanies.newBtn' | translate }}
        </button>
      </div>

      <app-search-sort-bar [placeholder]="'search.placeholderCompanies' | translate"
                           (search)="onSearch($event)" (sortChange)="onSort($event)"></app-search-sort-bar>

      @if (!companies().length) {
        <div class="empty card-surface">
          <mat-icon>corporate_fare</mat-icon>
          <p>{{ 'adminCompanies.empty' | translate }}</p>
        </div>
      } @else {
        <mat-card>
          <mat-card-content class="table-wrap">
            <table mat-table [dataSource]="companies()" class="full">
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>{{ 'adminCompanies.col.company' | translate }}</th>
                <td mat-cell *matCellDef="let c">
                  <div class="company-cell">
                    <div class="avatar">{{ (c.name || '?')[0].toUpperCase() }}</div>
                    <div>
                      <div><strong>{{ c.name }}</strong></div>
                      @if (c.tradeName) { <div class="text-muted small">{{ c.tradeName }}</div> }
                      <div class="text-muted small">{{ c.taxId || '—' }} · {{ c.countryCode }}</div>
                    </div>
                  </div>
                </td>
              </ng-container>

              <ng-container matColumnDef="owner">
                <th mat-header-cell *matHeaderCellDef>{{ 'adminCompanies.col.owner' | translate }}</th>
                <td mat-cell *matCellDef="let c">
                  @if (c.ownerName) {
                    <div><strong>{{ c.ownerName }}</strong></div>
                    <div class="text-muted small">{{ c.ownerEmail }}</div>
                  } @else {
                    <mat-chip class="chip-pending">{{ 'adminCompanies.awaitingOwner' | translate }}</mat-chip>
                    <div class="text-muted small">{{ c.contactEmail }}</div>
                  }
                </td>
              </ng-container>

              <ng-container matColumnDef="stats">
                <th mat-header-cell *matHeaderCellDef>{{ 'adminCompanies.col.stats' | translate }}</th>
                <td mat-cell *matCellDef="let c">
                  <div class="mini-stats">
                    <span [matTooltip]="'adminCompanies.stats.admins' | translate"><mat-icon>badge</mat-icon> {{ c.activeMembersCount }}</span>
                    <span [matTooltip]="'adminCompanies.stats.pending' | translate"><mat-icon>schedule</mat-icon> {{ c.pendingInvitesCount }}</span>
                    <span [matTooltip]="'adminCompanies.stats.nutris' | translate"><mat-icon>medical_services</mat-icon> {{ c.nutritionistsCount }}</span>
                  </div>
                </td>
              </ng-container>

              <ng-container matColumnDef="active">
                <th mat-header-cell *matHeaderCellDef>{{ 'adminCompanies.col.active' | translate }}</th>
                <td mat-cell *matCellDef="let c">
                  <mat-slide-toggle color="primary" [checked]="c.active"
                                    (change)="toggle(c, $event.checked)"
                                    [matTooltip]="'adminCompanies.col.active' | translate"></mat-slide-toggle>
                </td>
              </ng-container>

              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef>{{ 'adminCompanies.col.actions' | translate }}</th>
                <td mat-cell *matCellDef="let c">
                  @if (!c.ownerUserId) {
                    <button mat-icon-button color="primary" (click)="resendInvite(c)"
                            [matTooltip]="'adminCompanies.resendInvite' | translate">
                      <mat-icon>send</mat-icon>
                    </button>
                  }
                  <button mat-icon-button color="warn" (click)="remove(c)"
                          [matTooltip]="'adminCompanies.deleteTooltip' | translate">
                    <mat-icon>delete_outline</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="cols"></tr>
              <tr mat-row *matRowDef="let row; columns: cols"></tr>
            </table>
            <mat-paginator [length]="total()" [pageSize]="pageSize" [pageIndex]="pageIndex"
                           [pageSizeOptions]="[10, 25, 50]" (page)="onPage($event)"
                           showFirstLastButtons></mat-paginator>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .full { width: 100%; }
    .table-wrap { overflow-x: auto; padding: 0 !important; }
    table tr.mat-mdc-header-row { background: var(--nt-surface-2); }
    table td, table th {
      padding: 12px 16px !important; color: var(--nt-text) !important;
      border-color: var(--nt-border) !important;
    }
    table th { color: var(--nt-text-muted) !important; font-weight: 600 !important; }

    .company-cell { display: flex; align-items: center; gap: 12px; }
    .avatar {
      width: 40px; height: 40px; border-radius: 10px;
      background: linear-gradient(135deg, #3b82f6, #2563eb);
      color: #fff; display: flex; align-items: center; justify-content: center;
      font-weight: 700;
    }
    .small { font-size: 0.8rem; }

    .chip-pending {
      background: #fef3c7 !important; color: #92400e !important;
      font-weight: 600; font-size: 0.75rem !important;
    }

    .mini-stats {
      display: flex; gap: 12px; align-items: center; color: var(--nt-text-muted);
      span { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; }
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
  `]
})
export class AdminCompaniesComponent implements OnInit {
  private api = inject(ApiService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  companies = signal<AdminCompany[]>([]);
  cols = ['name', 'owner', 'stats', 'active', 'actions'];

  ngOnInit(): void { this.load(); }

  query = '';
  sort: 'created' | 'name' = 'name';
  pageIndex = 0;
  pageSize = 10;
  total = signal(0);

  load() {
    this.api.adminListCompanies(this.query, this.sort, this.pageIndex, this.pageSize)
        .subscribe(p => { this.companies.set(p.items); this.total.set(p.total); });
  }

  onSearch(q: string) { this.query = q; this.pageIndex = 0; this.load(); }
  onSort(s: 'created' | 'name') { this.sort = s; this.pageIndex = 0; this.load(); }
  onPage(e: PageEvent) { this.pageIndex = e.pageIndex; this.pageSize = e.pageSize; this.load(); }

  openCreate() {
    const ref = this.dialog.open(AdminCompanyCreateDialog, { width: '560px' });
    ref.afterClosed().subscribe(created => { if (created) this.load(); });
  }

  toggle(c: AdminCompany, active: boolean) {
    this.api.adminSetCompanyActive(c.id, active).subscribe(() => this.load());
  }

  resendInvite(c: AdminCompany) {
    this.api.adminResendOwnerInvite(c.id).subscribe({
      next: () => this.snack.open(this.translate.instant('adminCompanies.inviteResent'), 'OK', { duration: 2500 }),
      error: err => this.snack.open(err?.error?.error ?? 'Erro', 'OK', { duration: 3000 })
    });
  }

  remove(c: AdminCompany) {
    if (!confirm(this.translate.instant('adminCompanies.deleteConfirm', { name: c.name }))) return;
    this.api.adminDeleteCompany(c.id).subscribe(() => this.load());
  }
}
