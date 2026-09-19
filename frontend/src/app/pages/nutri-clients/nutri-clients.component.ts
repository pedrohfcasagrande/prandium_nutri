import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { ImpersonationService } from '../../services/impersonation.service';
import { ClientLink, LinkStatus } from '../../models/models';

@Component({
  selector: 'app-nutri-clients',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatTooltipModule,
    MatDividerModule, MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutri.clients.title' | translate }}</h1>
          <p>{{ 'nutri.clients.subtitle' | translate }}</p>
        </div>
      </div>

      <mat-card class="invite-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>person_add</mat-icon>
            {{ 'nutri.clients.inviteTitle' | translate }}
          </mat-card-title>
          <mat-card-subtitle>{{ 'nutri.clients.inviteSubtitle' | translate }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="invite-row">
            <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
              <mat-label>{{ 'nutri.clients.emailLabel' | translate }}</mat-label>
              <input matInput type="email" [(ngModel)]="email"
                     [placeholder]="'nutri.clients.emailHint' | translate"
                     (keydown.enter)="invite()">
              <mat-icon matPrefix>mail</mat-icon>
            </mat-form-field>
            <button mat-flat-button color="primary" (click)="invite()"
                    [disabled]="!email || loading()">
              <mat-icon>send</mat-icon>
              {{ 'nutri.clients.inviteBtn' | translate }}
            </button>
          </div>
        </mat-card-content>
      </mat-card>

      @if (requests().length) {
        <div class="page-header history-header">
          <h2>
            <mat-icon class="header-icon" style="color:#f59e0b">contact_mail</mat-icon>
            {{ 'nutri.requests.title' | translate }} ({{ requests().length }})
          </h2>
        </div>
        <div class="client-list">
          @for (r of requests(); track r.id) {
            <mat-card class="client-card pending request-card">
              <div class="client-info">
                <div class="avatar avatar-pending">{{ (r.clientName || '?')[0].toUpperCase() }}</div>
                <div class="client-text">
                  <div class="client-name">{{ r.clientName }}</div>
                  <div class="client-email">{{ r.clientEmail }}</div>
                  <div class="client-meta">
                    <mat-chip class="status-chip chip-pending">
                      {{ 'nutri.requests.chip' | translate }}
                    </mat-chip>
                    <span class="text-muted small">{{ r.invitedAt | date:'short' }}</span>
                  </div>
                </div>
              </div>
              <div class="client-actions">
                <button mat-flat-button color="primary" (click)="accept(r)">
                  <mat-icon>check_circle</mat-icon>
                  {{ 'nutri.requests.accept' | translate }}
                </button>
                <button mat-stroked-button color="warn" (click)="reject(r)">
                  <mat-icon>cancel</mat-icon>
                  {{ 'nutri.requests.reject' | translate }}
                </button>
              </div>
            </mat-card>
          }
        </div>
      }

      <div class="page-header history-header">
        <h2>{{ 'nutri.clients.listTitle' | translate }} ({{ clients().length }})</h2>
      </div>

      @if (!clients().length) {
        <div class="empty card-surface">
          <mat-icon>group_add</mat-icon>
          <p>{{ 'nutri.clients.empty' | translate }}</p>
        </div>
      } @else {
        <div class="client-list">
          @for (c of clients(); track c.id) {
            <mat-card class="client-card" [class.pending]="c.status === 'PENDING'"
                                          [class.active]="c.status === 'ACTIVE'">
              <div class="client-info">
                <div class="avatar" [class.avatar-pending]="c.status === 'PENDING'">
                  {{ (c.clientName || '?')[0].toUpperCase() }}
                </div>
                <div class="client-text">
                  <div class="client-name">{{ c.clientName }}</div>
                  <div class="client-email">{{ c.clientEmail }}</div>
                  <div class="client-meta">
                    <mat-chip class="status-chip" [class]="'chip-' + c.status.toLowerCase()">
                      {{ ('nutri.status.' + c.status) | translate }}
                    </mat-chip>
                    @if (c.acceptedAt) {
                      <span class="text-muted small">
                        {{ 'nutri.clients.acceptedOn' | translate }} {{ c.acceptedAt | date:'shortDate' }}
                      </span>
                    } @else {
                      <span class="text-muted small">
                        {{ 'nutri.clients.invitedOn' | translate }} {{ c.invitedAt | date:'shortDate' }}
                      </span>
                    }
                  </div>
                </div>
              </div>
              <div class="client-actions">
                @if (c.status === 'ACTIVE' && c.clientId) {
                  <button mat-flat-button color="primary" (click)="manage(c)"
                          [matTooltip]="'nutri.clients.manageTooltip' | translate">
                    <mat-icon>manage_accounts</mat-icon>
                    {{ 'nutri.clients.manage' | translate }}
                  </button>
                }
                @if (canRevoke(c.status)) {
                  <button mat-icon-button color="warn" (click)="revoke(c)"
                          [matTooltip]="'nutri.clients.revokeTooltip' | translate">
                    <mat-icon>link_off</mat-icon>
                  </button>
                }
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .invite-card mat-card-title { display: flex; align-items: center; gap: 8px; }
    .invite-card mat-icon { color: var(--nt-primary); }

    .invite-row { display: flex; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .grow { flex: 1 1 260px; }

    .history-header h2 { font-size: 1.35rem; margin: 0; font-weight: 600;
      display: flex; align-items: center; gap: 8px;
    }
    .request-card { border-left: 4px solid #f59e0b !important; }

    .client-list { display: flex; flex-direction: column; gap: 12px; }
    .client-card {
      padding: 16px 20px !important;
      display: flex; justify-content: space-between; align-items: center;
      gap: 16px; flex-wrap: wrap;
    }
    .client-card.pending { border-left: 4px solid var(--nt-accent) !important; }
    .client-card.active { border-left: 4px solid var(--nt-primary) !important; }

    .client-info { display: flex; gap: 14px; align-items: center; }
    .avatar {
      width: 48px; height: 48px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 1.1rem;
    }
    .avatar.avatar-pending {
      background: linear-gradient(135deg, var(--nt-accent), #d97706);
    }
    .client-name { font-weight: 600; font-size: 1.05rem; }
    .client-email { color: var(--nt-text-muted); font-size: 0.9rem; margin: 2px 0 4px; }
    .client-meta { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .small { font-size: 0.8rem; }

    .status-chip { font-weight: 600; font-size: 0.75rem !important; }
    .chip-pending { background: #fef3c7 !important; color: #92400e !important; }
    .chip-active { background: #d1fae5 !important; color: #065f46 !important; }
    .chip-rejected, .chip-revoked_by_nutri, .chip-revoked_by_client, .chip-expired {
      background: var(--nt-surface-2) !important; color: var(--nt-text-muted) !important;
    }

    .client-actions { display: flex; gap: 8px; align-items: center; }
  `]
})
export class NutriClientsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private impersonation = inject(ImpersonationService);
  private router = inject(Router);

  clients = signal<ClientLink[]>([]);
  requests = signal<ClientLink[]>([]);
  email = '';
  loading = signal(false);

  ngOnInit(): void { this.load(); this.loadRequests(); }

  load() { this.api.nutriListClients().subscribe(c => this.clients.set(c)); }
  loadRequests() { this.api.clientRequests().subscribe({
    next: r => this.requests.set(r),
    error: () => this.requests.set([])
  }); }

  accept(r: ClientLink) {
    this.api.acceptClientRequest(r.id).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutri.requests.accepted'), 'OK', { duration: 2500 });
        this.loadRequests();
        this.load();
      },
      error: err => this.snack.open(err?.error?.error ?? 'Erro', 'OK', { duration: 3000 })
    });
  }

  reject(r: ClientLink) {
    const note = prompt(this.translate.instant('nutri.requests.rejectPrompt')) || undefined;
    this.api.rejectClientRequest(r.id, note).subscribe({
      next: () => {
        this.snack.open(this.translate.instant('nutri.requests.rejected'), 'OK', { duration: 2500 });
        this.loadRequests();
      },
      error: err => this.snack.open(err?.error?.error ?? 'Erro', 'OK', { duration: 3000 })
    });
  }

  invite() {
    if (!this.email) return;
    this.loading.set(true);
    this.api.nutriInviteClient(this.email).subscribe({
      next: () => {
        this.loading.set(false);
        this.email = '';
        this.snack.open(this.translate.instant('nutri.clients.inviteSent'), 'OK', { duration: 2500 });
        this.load();
      },
      error: err => {
        this.loading.set(false);
        this.snack.open(err?.error?.error ?? this.translate.instant('nutri.clients.inviteFail'),
                        'OK', { duration: 3000 });
      }
    });
  }

  manage(c: ClientLink) {
    if (!c.clientId) return;
    this.impersonation.start({ id: c.clientId, name: c.clientName, email: c.clientEmail });
    this.router.navigate(['/dashboard']);
  }

  revoke(c: ClientLink) {
    if (!confirm(this.translate.instant('nutri.clients.revokeConfirm', { name: c.clientName }))) return;
    this.api.nutriRevokeClient(c.id).subscribe(() => this.load());
  }

  canRevoke(status: string): boolean {
    return status === 'ACTIVE' || status === 'PENDING';
  }
}
