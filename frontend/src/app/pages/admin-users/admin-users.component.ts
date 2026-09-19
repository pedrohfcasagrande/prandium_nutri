import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AdminUser } from '../../models/models';
import { ImpersonationService } from '../../services/impersonation.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule,
    MatSlideToggleModule, MatChipsModule, MatTooltipModule, MatSnackBarModule,
    TranslateModule
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
          <strong>{{ users().length }}</strong>
          <span class="text-muted">{{ 'admin.usersCount' | translate:{ count: '' } }}</span>
        </div>
      </div>

      <mat-card>
        <mat-card-content class="table-wrap">
          <table mat-table [dataSource]="users()" class="full">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>{{ 'admin.user' | translate }}</th>
              <td mat-cell *matCellDef="let u">
                <div class="user-cell">
                  <div class="avatar-sm">{{ u.name?.[0]?.toUpperCase() }}</div>
                  <div>
                    <div><strong>{{ u.name }}</strong></div>
                    <div class="text-muted small">{{ u.email }}</div>
                  </div>
                </div>
              </td>
            </ng-container>

            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef>{{ 'admin.role' | translate }}</th>
              <td mat-cell *matCellDef="let u">
                <mat-chip [class.admin-chip]="u.role === 'ADMIN'">{{ u.role }}</mat-chip>
              </td>
            </ng-container>

            <ng-container matColumnDef="provider">
              <th mat-header-cell *matHeaderCellDef>{{ 'admin.provider' | translate }}</th>
              <td mat-cell *matCellDef="let u">
                <span class="provider-badge">
                  <mat-icon>{{ u.provider === 'GOOGLE' ? 'g_translate' : 'lock' }}</mat-icon>
                  {{ u.provider }}
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="active">
              <th mat-header-cell *matHeaderCellDef>{{ 'admin.active' | translate }}</th>
              <td mat-cell *matCellDef="let u">
                <mat-slide-toggle color="primary" [checked]="u.active"
                                  (change)="toggle(u, $event.checked)"
                                  [disabled]="u.role === 'ADMIN'"></mat-slide-toggle>
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>{{ 'admin.actions' | translate }}</th>
              <td mat-cell *matCellDef="let u">
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
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .full { width: 100%; }
    .table-wrap { overflow-x: auto; padding: 0 !important; }

    .stat-badge {
      display: inline-flex; align-items: center; gap: 8px;
      background: var(--nt-surface); padding: 8px 14px;
      border-radius: 999px; box-shadow: var(--nt-shadow);
      strong { font-size: 1.1rem; }
    }

    .user-cell { display: flex; align-items: center; gap: 12px; }
    .avatar-sm {
      width: 36px; height: 36px;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-weight: 600;
    }
    .small { font-size: 0.85rem; }

    mat-chip { font-weight: 600; }
    .admin-chip { background: #fef3c7 !important; color: #92400e !important; }

    .provider-badge {
      display: inline-flex; align-items: center; gap: 4px;
      color: var(--nt-text-muted); font-size: 0.9rem;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }

    table tr.mat-mdc-header-row { background: var(--nt-surface-2); }
    table td, table th { padding: 12px 16px !important; color: var(--nt-text) !important; border-color: var(--nt-border) !important; }
    table th { color: var(--nt-text-muted) !important; font-weight: 600 !important; }
  `]
})
export class AdminUsersComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private router = inject(Router);
  private impersonation = inject(ImpersonationService);

  users = signal<AdminUser[]>([]);
  cols = ['name', 'role', 'provider', 'active', 'actions'];

  ngOnInit(): void { this.load(); }
  load() { this.api.adminListUsers().subscribe(u => this.users.set(u)); }

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
