import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AdminUser } from '../../models/models';
import { ImpersonationService } from '../../services/impersonation.service';
import { SearchSortBarComponent } from '../../components/search-sort-bar.component';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSlideToggleModule, MatTooltipModule, MatSnackBarModule,
    TranslateModule, SearchSortBarComponent, MatPaginatorModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'admin.title' | translate }}</h1>
          <p>{{ 'admin.subtitle' | translate }}</p>
        </div>
        <div class="stat-badge">
          <mat-icon>group</mat-icon>
          <strong>{{ total() }}</strong>
          <span class="text-muted">{{ 'admin.usersCount' | translate:{ count: '' } }}</span>
        </div>
      </div>

      <app-search-sort-bar (search)="onSearch($event)" (sortChange)="onSort($event)"></app-search-sort-bar>

      @if (users().length === 0) {
        <div class="empty card-surface">
          <mat-icon>group</mat-icon>
          <p>{{ 'admin.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (u of users(); track u.id; let i = $index) {
            <mat-card class="row-card stagger-item" [style.--i]="i"
                      [class.active]="u.active" [class.inactive]="!u.active"
                      [class.admin-row]="u.role === 'ADMIN'">
              <div class="avatar">{{ u.name?.[0]?.toUpperCase() || '?' }}</div>
              <div class="info">
                <div class="name">
                  {{ u.name }}
                  @if (u.crn) { <span class="crn-inline">· {{ u.crn }}</span> }
                </div>
                <div class="email">
                  {{ u.email }}
                  @if (u.cpf) { <span class="cpf-inline">· {{ u.cpf }}</span> }
                </div>
                <div class="meta">
                  <span class="status-chip" [class.chip-admin]="u.role === 'ADMIN'"
                                            [class.chip-role]="u.role !== 'ADMIN'">
                    <mat-icon>{{ u.role === 'ADMIN' ? 'shield' : 'person' }}</mat-icon>
                    {{ u.role }}
                  </span>
                  <span class="provider-badge">
                    <mat-icon>{{ u.provider === 'GOOGLE' ? 'g_translate' : 'lock' }}</mat-icon>
                    {{ u.provider }}
                  </span>
                  <span class="status-chip" [class.chip-active]="u.active" [class.chip-inactive]="!u.active">
                    <mat-icon>{{ u.active ? 'check_circle' : 'cancel' }}</mat-icon>
                    {{ (u.active ? 'admin.statusActive' : 'admin.statusInactive') | translate }}
                  </span>
                </div>
              </div>
              <div class="actions">
                <mat-slide-toggle color="primary" [checked]="u.active"
                                  (change)="toggle(u, $event.checked)"
                                  [disabled]="u.role === 'ADMIN'"
                                  [matTooltip]="'admin.active' | translate"></mat-slide-toggle>
                <button mat-icon-button color="primary" (click)="manage(u)"
                        [disabled]="u.role === 'ADMIN'"
                        [matTooltip]="'admin.manageTooltip' | translate">
                  <mat-icon>manage_accounts</mat-icon>
                </button>
                <button mat-icon-button color="warn" (click)="remove(u)"
                        [disabled]="u.role === 'ADMIN'"
                        [matTooltip]="'admin.deleteTooltip' | translate">
                  <mat-icon>delete_outline</mat-icon>
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
    .stat-badge {
      display: inline-flex; align-items: center; gap: 8px;
      background: var(--nt-surface); padding: 8px 14px;
      border-radius: 999px; box-shadow: var(--nt-shadow);
      strong { font-size: 1.1rem; }
    }

    .list { display: flex; flex-direction: column; gap: 12px; }
    .row-card {
      padding: 16px 20px !important;
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 16px;
    }
    .row-card.active { border-left: 4px solid var(--nt-primary) !important; }
    .row-card.inactive { border-left: 4px solid var(--nt-border-strong) !important; opacity: 0.85; }
    .row-card.admin-row { border-left: 4px solid var(--nt-accent) !important; opacity: 1; }

    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff; display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 1.1rem; flex-shrink: 0;
    }
    .info { min-width: 0; }
    .name { font-weight: 600; font-size: 1.05rem; }
    .crn-inline, .cpf-inline { font-weight: 400; font-size: 0.85em;
      color: var(--nt-text-muted); font-variant-numeric: tabular-nums; margin-left: 2px; }
    .email { color: var(--nt-text-muted); font-size: 0.9rem; margin: 2px 0 6px;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .meta { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

    .status-chip {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 4px 12px; border-radius: 999px;
      font-weight: 600; font-size: 0.72rem;
      text-transform: uppercase; letter-spacing: 0.03em;
      mat-icon { font-size: 14px; width: 14px; height: 14px; }
    }
    .chip-active { background: #d1fae5; color: #065f46; }
    .chip-inactive { background: #f1f5f9; color: #64748b; }
    .chip-admin { background: #fef3c7; color: #92400e; }
    .chip-role { background: var(--nt-primary-soft); color: var(--nt-primary-dark); }

    .provider-badge {
      display: inline-flex; align-items: center; gap: 4px;
      color: var(--nt-text-muted); font-size: 0.8rem;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    .actions { display: flex; gap: 4px; align-items: center; justify-self: end; flex-wrap: wrap; }

    @media (max-width: 720px) {
      .row-card { grid-template-columns: auto 1fr; }
      .actions { grid-column: 1 / -1; justify-self: stretch; justify-content: flex-end; }
    }
  `]
})
export class AdminUsersComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private router = inject(Router);
  private impersonation = inject(ImpersonationService);

  users = signal<AdminUser[]>([]);
  query = '';
  sort: 'created' | 'name' = 'name';
  pageIndex = 0;
  pageSize = 10;
  total = signal(0);

  ngOnInit(): void { this.load(); }

  load() {
    this.api.adminListUsers(this.query, this.sort, this.pageIndex, this.pageSize)
        .subscribe(p => { this.users.set(p.items); this.total.set(p.total); });
  }

  onSearch(q: string) { this.query = q; this.pageIndex = 0; this.load(); }
  onSort(s: 'created' | 'name') { this.sort = s; this.pageIndex = 0; this.load(); }
  onPage(e: PageEvent) { this.pageIndex = e.pageIndex; this.pageSize = e.pageSize; this.load(); }

  manage(u: AdminUser) {
    this.impersonation.start({ id: u.id, name: u.name, email: u.email });
    this.router.navigate([u.profileCompleted ? '/dashboard' : '/profile-setup']);
  }

  toggle(u: AdminUser, active: boolean) {
    this.api.adminSetActive(u.id, active).subscribe({
      next: () => this.load(),
      error: () => this.snack.open(this.translate.instant('admin.toggleFail'), 'OK', { duration: 3000 })
    });
  }

  remove(u: AdminUser) {
    const msg = this.translate.instant('admin.deleteConfirm', { email: u.email });
    if (!confirm(msg)) return;
    this.api.adminDelete(u.id).subscribe({
      next: () => this.load(),
      error: () => this.snack.open(this.translate.instant('admin.deleteFail'), 'OK', { duration: 3000 })
    });
  }
}
