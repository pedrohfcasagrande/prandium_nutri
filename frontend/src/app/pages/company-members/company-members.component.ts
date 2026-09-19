import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { CompanyMember, CompanyMemberRole } from '../../models/models';

@Component({
  selector: 'app-company-members',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatTooltipModule,
    MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'companyMembers.title' | translate }}</h1>
          <p>{{ 'companyMembers.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-card class="invite-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>person_add</mat-icon>
            {{ 'companyMembers.inviteTitle' | translate }}
          </mat-card-title>
          <mat-card-subtitle>{{ 'companyMembers.inviteSubtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="invite-row">
            <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
              <mat-label>{{ 'companyMembers.email' | translate }}</mat-label>
              <input matInput type="email" [(ngModel)]="email" (keydown.enter)="invite()">
              <mat-icon matPrefix>mail</mat-icon>
            </mat-form-field>
            <mat-form-field appearance="outline" class="role" subscriptSizing="dynamic">
              <mat-label>{{ 'companyMembers.role' | translate }}</mat-label>
              <mat-select [(ngModel)]="role">
                <mat-option value="ADMIN">{{ 'companyMembers.roleAdmin' | translate }}</mat-option>
                <mat-option value="OWNER">{{ 'companyMembers.roleOwner' | translate }}</mat-option>
              </mat-select>
            </mat-form-field>
            <button mat-flat-button color="primary" (click)="invite()" [disabled]="!email">
              <mat-icon>send</mat-icon> {{ 'companyMembers.inviteBtn' | translate }}
            </button>
          </div>
        </mat-card-content>
      </mat-card>

      <div class="page-header history-header">
        <h2>{{ 'companyMembers.listTitle' | translate }}</h2>
      </div>

      @if (!members().length) {
        <div class="empty card-surface">
          <mat-icon>group</mat-icon>
          <p>{{ 'companyMembers.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (m of members(); track m.id) {
            <mat-card class="member-card" [class.pending]="m.status === 'PENDING'">
              <div class="avatar" [class.avatar-pending]="m.status === 'PENDING'">
                {{ (m.userName || '?')[0].toUpperCase() }}
              </div>
              <div class="member-info">
                <div class="member-name">{{ m.userName }}</div>
                <div class="member-email">{{ m.userEmail }}</div>
                <div class="member-meta">
                  <span class="chip role-chip" [class.owner-chip]="m.roleInCompany === 'OWNER'">
                    {{ (m.roleInCompany === 'OWNER' ? 'companyMembers.roleOwner' : 'companyMembers.roleAdmin') | translate }}
                  </span>
                  <span class="chip status-chip" [class]="'chip-' + m.status.toLowerCase()">
                    {{ ('nutri.status.' + m.status) | translate }}
                  </span>
                </div>
              </div>
              @if (canRevoke(m.status) && !(m.roleInCompany === 'OWNER' && m.status === 'ACTIVE')) {
                <button mat-icon-button color="warn" (click)="revoke(m)"
                        [matTooltip]="'companyMembers.revokeTooltip' | translate">
                  <mat-icon>link_off</mat-icon>
                </button>
              }
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    mat-card-title { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
    }
    .invite-row { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .grow { flex: 1 1 240px; }
    .role { width: 140px; }
    .history-header h2 { font-size: 1.35rem; margin: 0; font-weight: 600; }

    .list { display: flex; flex-direction: column; gap: 12px; }
    .member-card {
      padding: 16px 20px !important;
      display: grid;
      grid-template-columns: auto 1fr auto;
      align-items: center;
      gap: 16px;
    }
    .member-card.pending { border-left: 4px solid var(--nt-accent) !important; }

    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, #3b82f6, #2563eb);
      color: #fff; font-weight: 700; font-size: 1.1rem;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .avatar.avatar-pending { background: linear-gradient(135deg, var(--nt-accent), #d97706); }
    .member-info { min-width: 0; }
    .member-name { font-weight: 600; font-size: 1.05rem; }
    .member-email {
      color: var(--nt-text-muted); font-size: 0.9rem; margin: 2px 0 6px;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .member-meta { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

    .chip {
      display: inline-flex; align-items: center;
      padding: 4px 12px; border-radius: 999px;
      font-weight: 600; font-size: 0.72rem;
      text-transform: uppercase; letter-spacing: 0.03em;
      background: var(--nt-surface-2); color: var(--nt-text);
    }
    .role-chip { background: rgba(59, 130, 246, 0.15); color: #1e40af; }
    .owner-chip { background: #fef3c7; color: #92400e; }
    .chip-pending { background: #fef3c7; color: #92400e; }
    .chip-active { background: #d1fae5; color: #065f46; }
    .chip-rejected, .chip-revoked_by_nutri, .chip-revoked_by_client, .chip-revoked_by_company, .chip-expired {
      background: var(--nt-surface-2); color: var(--nt-text-muted);
    }

    @media (max-width: 640px) {
      .member-card { grid-template-columns: auto 1fr; }
      .member-card button { grid-column: 1 / -1; justify-self: start; }
    }
  `]
})
export class CompanyMembersComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  members = signal<CompanyMember[]>([]);
  email = '';
  role: CompanyMemberRole = 'ADMIN';

  ngOnInit(): void { this.load(); }

  load() { this.api.companyListMembers().subscribe(m => this.members.set(m)); }

  invite() {
    if (!this.email) return;
    this.api.companyInviteMember(this.email, this.role).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('companyMembers.inviteSent'), 'OK', { duration: 2500 });
        this.email = ''; this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? this.translate.instant('companyMembers.inviteFail'),
                                    'OK', { duration: 3000 })
    });
  }

  revoke(m: CompanyMember) {
    if (!confirm(this.translate.instant('companyMembers.revokeConfirm', { name: m.userName }))) return;
    this.api.companyRevokeMember(m.id).subscribe(() => this.load());
  }

  canRevoke(status: string): boolean {
    return status === 'ACTIVE' || status === 'PENDING';
  }
}
