import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { ImpersonationService } from '../../services/impersonation.service';
import { EmptyStateComponent } from '../../components/empty-state.component';
import { PracticeService } from '../../services/practice.service';
import { SearchSortBarComponent } from '../../components/search-sort-bar.component';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ClientLink } from '../../models/models';

@Component({
  selector: 'app-nutri-clients',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatTooltipModule,
    MatDividerModule, MatSnackBarModule, TranslateModule,
    SearchSortBarComponent, MatPaginatorModule, EmptyStateComponent
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
            <mat-card class="client-card request-card">
              <div class="avatar avatar-pending">{{ (r.clientName || '?')[0].toUpperCase() }}</div>
              <div class="client-text">
                <div class="client-name">{{ r.clientName }}</div>
                <div class="client-email">{{ r.clientEmail }}</div>
                <div class="client-meta">
                  <span class="status-pill pill-pending">
                    {{ 'nutri.requests.chip' | translate }}
                  </span>
                  <span class="text-muted small">{{ r.invitedAt | date:'short' }}</span>
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
        <h2>{{ 'nutri.clients.listTitle' | translate }} ({{ total() }})</h2>
      </div>
      <app-search-sort-bar [placeholder]="'search.placeholderClients' | translate"
                           (search)="onSearch($event)" (sortChange)="onSort($event)"></app-search-sort-bar>

      @if (!clients().length) {
        <app-empty-state type="clients"
                         [title]="'nutri.clients.emptyTitle' | translate"
                         [description]="'nutri.clients.empty' | translate"></app-empty-state>
      } @else {
        @if (activeSection().length > 0) {
          <div class="client-list">
            @for (c of activeSection(); track c.id; let i = $index) {
              <ng-container *ngTemplateOutlet="clientCard; context: { $implicit: c, i: i }"></ng-container>
            }
          </div>
        }

        @if (historySection().length > 0) {
          <div class="history-toggle">
            <button mat-stroked-button (click)="showHistory.set(!showHistory())">
              <mat-icon>{{ showHistory() ? 'expand_less' : 'expand_more' }}</mat-icon>
              {{ (showHistory() ? 'nutri.clients.hideHistory' : 'nutri.clients.showHistory') | translate }}
              ({{ historySection().length }})
            </button>
          </div>
          @if (showHistory()) {
            <div class="client-list history-list">
              @for (c of historySection(); track c.id; let i = $index) {
                <ng-container *ngTemplateOutlet="clientCard; context: { $implicit: c, i: i }"></ng-container>
              }
            </div>
          }
        }

        <mat-paginator [length]="total()" [pageSize]="pageSize" [pageIndex]="pageIndex"
                       [pageSizeOptions]="[10, 25, 50]" (page)="onPage($event)"
                       showFirstLastButtons></mat-paginator>
      }

      <ng-template #clientCard let-c let-i="i">
        <mat-card class="client-card stagger-item" [style.--i]="i"
                                      [class.pending]="c.status === 'PENDING'"
                                      [class.active]="c.status === 'ACTIVE'">
          <div class="avatar" [class.avatar-pending]="c.status === 'PENDING'">
            {{ (c.clientName || '?')[0].toUpperCase() }}
          </div>
          <div class="client-text">
            <div class="client-name">{{ c.clientName }}</div>
            <div class="client-email">
              {{ c.clientEmail }}
              @if (c.clientCpf) { <span class="cpf-inline">· {{ c.clientCpf }}</span> }
            </div>
            <div class="client-meta">
              <span class="status-pill" [class]="'pill-' + c.status.toLowerCase()">
                {{ ('nutri.status.' + c.status) | translate }}
              </span>
              @if (c.subscriptionStatus) {
                <span class="status-pill sub-pill" [class]="'subpill-' + c.subscriptionStatus.toLowerCase()"
                      [matTooltip]="subTooltip(c)">
                  <mat-icon class="sub-icon">{{ subIcon(c.subscriptionStatus) }}</mat-icon>
                  {{ (c.subscriptionPlanName || c.subscriptionPlanCode) }}
                </span>
              }
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
          <div class="client-actions">
            @if (c.status === 'ACTIVE' && c.clientId) {
              <button mat-stroked-button (click)="openAssessment(c)"
                      [matTooltip]="'nutri.clients.assessmentTooltip' | translate">
                <mat-icon>assignment</mat-icon>
                {{ 'nutri.clients.assessment' | translate }}
              </button>
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
      </ng-template>
    </div>
  `,
  styles: [`
    .invite-card mat-card-title { display: flex; align-items: center; gap: 8px; }
    .invite-card mat-icon { color: var(--nt-primary); }

    .invite-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .grow { flex: 1 1 260px; }

    .history-header h2 { font-size: 1.35rem; margin: 0; font-weight: 600;
      display: flex; align-items: center; gap: 8px;
    }
    .request-card { border-left: 4px solid #f59e0b !important; }

    .client-list { display: flex; flex-direction: column; gap: 12px; }
    .client-card {
      padding: 16px 20px !important;
      display: grid;
      grid-template-columns: auto 1fr auto;
      gap: 16px; align-items: center;
    }
    .client-card.pending { border-left: 4px solid var(--nt-accent) !important; }
    .client-card.active { border-left: 4px solid var(--nt-primary) !important; }

    .history-toggle {
      margin: 20px 0 12px;
      display: flex; justify-content: center;
    }
    .history-list .client-card { opacity: 0.75; }
    .request-card { border-left: 4px solid #f59e0b !important; }

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
    .client-text { min-width: 0; }
    .client-name { font-weight: 600; font-size: 1.05rem; }
    .client-email { color: var(--nt-text-muted); font-size: 0.9rem; margin: 2px 0 6px; }
    .cpf-inline { font-variant-numeric: tabular-nums; margin-left: 4px; }
    .client-meta { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .small { font-size: 0.8rem; }

    .status-pill {
      display: inline-flex; align-items: center;
      padding: 2px 10px; border-radius: 12px;
      font-size: 0.75rem; font-weight: 700; letter-spacing: 0.02em;
    }
    .pill-pending { background: #fef3c7; color: #92400e; }
    .pill-active { background: #d1fae5; color: #065f46; }
    .pill-rejected, .pill-revoked_by_nutri, .pill-revoked_by_client, .pill-expired {
      background: var(--nt-surface-2); color: var(--nt-text-muted);
    }
    .sub-pill { gap: 4px; cursor: help; }
    .sub-icon { font-size: 12px !important; width: 12px !important; height: 12px !important; }
    .subpill-active, .subpill-trialing { background: #dcfce7; color: #166534; }
    .subpill-past_due { background: #fee2e2; color: #991b1b; }
    .subpill-canceled, .subpill-suspended { background: var(--nt-surface-2); color: var(--nt-text-muted); }

    .client-actions { display: flex; gap: 8px; align-items: center; justify-self: end; }

    @media (max-width: 640px) {
      .client-card { grid-template-columns: auto 1fr; }
      .client-actions { grid-column: 1 / -1; justify-self: start; }
    }
  `]
})
export class NutriClientsComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  private impersonation = inject(ImpersonationService);
  private practiceCtx = inject(PracticeService);
  private router = inject(Router);

  clients = signal<ClientLink[]>([]);
  requests = signal<ClientLink[]>([]);
  email = '';
  loading = signal(false);
  showHistory = signal(false);

  // Separa vinculos ativos/pendentes dos historicos (revogados/rejeitados/expirados)
  // pra evitar mesmo cliente aparecer duplicado quando cliente foi revogado e re-vinculado.
  private isActiveOrPending = (c: ClientLink) => c.status === 'ACTIVE' || c.status === 'PENDING';
  activeSection = computed(() => this.clients().filter(this.isActiveOrPending));
  historySection = computed(() => this.clients().filter(c => !this.isActiveOrPending(c)));

  private firstLoad = true;
  constructor() {
    // Recarrega quando a practice ativa muda (nao no primeiro run)
    effect(() => {
      this.practiceCtx.activePracticeId();
      if (this.firstLoad) { this.firstLoad = false; return; }
      this.load();
      this.loadRequests();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void { this.load(); this.loadRequests(); }

  query = '';
  sort: 'created' | 'name' = 'name';
  pageIndex = 0;
  pageSize = 10;
  total = signal(0);

  load() {
    this.api.nutriListClients(this.practiceCtx.activePracticeId(), this.query, this.sort,
                              this.pageIndex, this.pageSize)
        .subscribe(p => { this.clients.set(p.items); this.total.set(p.total); });
  }

  onSearch(q: string) { this.query = q; this.pageIndex = 0; this.load(); }
  onSort(s: 'created' | 'name') { this.sort = s; this.pageIndex = 0; this.load(); }
  onPage(e: PageEvent) { this.pageIndex = e.pageIndex; this.pageSize = e.pageSize; this.load(); }
  loadRequests() { this.api.clientRequests(this.practiceCtx.activePracticeId()).subscribe({
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
    this.api.nutriInviteClient(this.email, this.practiceCtx.activePracticeId()).subscribe({
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

  openAssessment(c: ClientLink) {
    if (!c.clientId) return;
    this.router.navigate(['/nutritionist/clients', c.clientId, 'assessment']);
  }

  revoke(c: ClientLink) {
    if (!confirm(this.translate.instant('nutri.clients.revokeConfirm', { name: c.clientName }))) return;
    this.api.nutriRevokeClient(c.id).subscribe(() => this.load());
  }

  canRevoke(status: string): boolean {
    return status === 'ACTIVE' || status === 'PENDING';
  }

  subIcon(status: string): string {
    switch (status) {
      case 'ACTIVE':
      case 'TRIALING':  return 'workspace_premium';
      case 'PAST_DUE':  return 'error';
      case 'CANCELED':
      case 'SUSPENDED': return 'cancel';
      default:          return 'info';
    }
  }

  subTooltip(c: ClientLink): string {
    const parts = [this.translate.instant('nutri.clients.subStatus.' + c.subscriptionStatus)];
    if (c.subscriptionRenewsAt) {
      parts.push(this.translate.instant('nutri.clients.subRenews', {
        date: new Date(c.subscriptionRenewsAt).toLocaleDateString()
      }));
    }
    return parts.join(' · ');
  }
}
