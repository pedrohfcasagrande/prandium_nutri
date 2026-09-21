import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { PracticeService } from '../../services/practice.service';
import { ClientLink, MenuPlan, NutriMetricsOverview, Subscription } from '../../models/models';
import { SkeletonComponent } from '../../components/skeleton.component';

@Component({
  selector: 'app-nutri-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatIconModule, MatButtonModule,
    MatProgressSpinnerModule, TranslateModule,
    SkeletonComponent
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'nutriDashboard.greeting' | translate:{ name: firstName() } }}</h1>
          <p>{{ 'nutriDashboard.subtitle' | translate }}</p>
        </div>
      </div>

      @if (loading()) {
        <div class="kpi-grid">
          @for (i of [0,1,2,3]; track i) { <app-skeleton type="kpi"></app-skeleton> }
        </div>
        <div class="action-grid" style="margin-top:24px">
          @for (i of [0,1,2]; track i) { <app-skeleton type="card" height="140px"></app-skeleton> }
        </div>
      } @else {
        <div class="kpi-grid">
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#dcfce7;color:#166534"><mat-icon>group</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriDashboard.activeClients' | translate }}</div>
              <div class="kpi-value">{{ metrics()?.activeClients ?? 0 }}</div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#dbeafe;color:#1e40af"><mat-icon>menu_book</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriDashboard.menusActive' | translate }}</div>
              <div class="kpi-value">{{ metrics()?.menusActive ?? 0 }}</div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#fef3c7;color:#92400e"><mat-icon>trending_up</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriDashboard.adherence' | translate }}</div>
              <div class="kpi-value">{{ ((metrics()?.avgAdherence7d ?? 0) * 100) | number:'1.0-0' }}%</div>
              <div class="kpi-sub">{{ 'nutriDashboard.last7d' | translate }}</div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <div class="kpi-icon" style="background:#ede9fe;color:#6d28d9"><mat-icon>auto_awesome</mat-icon></div>
            <div>
              <div class="kpi-label">{{ 'nutriDashboard.aiMonth' | translate }}</div>
              <div class="kpi-value">
                {{ metrics()?.aiSuggestionsMonth ?? 0 }}
                @if ((metrics()?.aiSuggestionsLimit ?? 0) > 0) {
                  <span class="kpi-limit">/ {{ metrics()?.aiSuggestionsLimit }}</span>
                }
              </div>
            </div>
          </mat-card>
        </div>

        <div class="action-grid">
          <mat-card class="action-card" [class.has-work]="pendingMenus().length > 0">
            <div class="action-head">
              <mat-icon>rate_review</mat-icon>
              <h3>{{ 'nutriDashboard.pendingMenus' | translate }}</h3>
            </div>
            @if (pendingMenus().length === 0) {
              <p class="text-muted">{{ 'nutriDashboard.pendingMenusEmpty' | translate }}</p>
            } @else {
              <p><strong>{{ pendingMenus().length }}</strong> {{ 'nutriDashboard.pendingMenusCount' | translate }}</p>
              <button mat-flat-button color="primary" routerLink="/nutritionist/pending-menus">
                {{ 'nutriDashboard.reviewNow' | translate }}
              </button>
            }
          </mat-card>

          <mat-card class="action-card" [class.has-work]="clientRequests().length > 0">
            <div class="action-head">
              <mat-icon>contact_mail</mat-icon>
              <h3>{{ 'nutriDashboard.newRequests' | translate }}</h3>
            </div>
            @if (clientRequests().length === 0) {
              <p class="text-muted">{{ 'nutriDashboard.newRequestsEmpty' | translate }}</p>
            } @else {
              <p><strong>{{ clientRequests().length }}</strong> {{ 'nutriDashboard.newRequestsCount' | translate }}</p>
              <button mat-flat-button color="primary" routerLink="/nutritionist/clients">
                {{ 'nutriDashboard.reviewNow' | translate }}
              </button>
            }
          </mat-card>

          <mat-card class="action-card" [class.has-work]="needsAttention() > 0">
            <div class="action-head">
              <mat-icon>priority_high</mat-icon>
              <h3>{{ 'nutriDashboard.attention' | translate }}</h3>
            </div>
            @if (needsAttention() === 0) {
              <p class="text-muted">{{ 'nutriDashboard.attentionEmpty' | translate }}</p>
            } @else {
              <p><strong>{{ needsAttention() }}</strong> {{ 'nutriDashboard.attentionCount' | translate }}</p>
              <button mat-flat-button color="primary" routerLink="/nutritionist/metrics">
                {{ 'nutriDashboard.viewDetail' | translate }}
              </button>
            }
          </mat-card>
        </div>

        <mat-card class="plan-card">
          <div class="plan-head">
            <mat-icon>workspace_premium</mat-icon>
            <div>
              <h3>{{ 'nutriDashboard.myPlan' | translate }}</h3>
              <p class="text-muted">{{ 'nutriDashboard.myPlanSubtitle' | translate }}</p>
            </div>
          </div>
          @if (subscription(); as s) {
            <div class="plan-details">
              <div>
                <div class="plan-label">{{ 'nutriDashboard.currentPlan' | translate }}</div>
                <div class="plan-value">{{ s.planName || s.planCode || '-' }}</div>
              </div>
              @if (isPaidPlan(s)) {
                <div>
                  <div class="plan-label">{{ 'nutriDashboard.status' | translate }}</div>
                  <span class="status-pill" [class]="'pill-' + s.status.toLowerCase()">
                    {{ ('nutriDashboard.sub.' + s.status) | translate }}
                  </span>
                </div>
                @if (s.currentPeriodEnd) {
                  <div>
                    <div class="plan-label">{{ 'nutriDashboard.renewsAt' | translate }}</div>
                    <div class="plan-value">{{ s.currentPeriodEnd | date:'shortDate' }}</div>
                  </div>
                }
              }
            </div>
            <div class="plan-actions">
              <a mat-stroked-button routerLink="/pricing">{{ 'nutriDashboard.viewPlans' | translate }}</a>
              @if (isPaidPlan(s)) {
                <a mat-stroked-button routerLink="/billing">{{ 'nutriDashboard.manageBilling' | translate }}</a>
              }
            </div>
          } @else {
            <p class="text-muted">{{ 'nutriDashboard.noSubscription' | translate }}</p>
          }
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .loading { display: flex; justify-content: center; padding: 60px; }
    .kpi-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px; margin-bottom: 24px;
    }
    .kpi {
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      gap: 10px; padding: 24px 20px; text-align: center;
    }
    .kpi-icon {
      width: 56px; height: 56px; border-radius: 14px;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
    .kpi-label { font-size: 0.85rem; color: var(--nt-text-muted); }
    .kpi-value { font-size: 1.75rem; font-weight: 700; }
    .kpi-limit { font-size: 1rem; color: var(--nt-text-muted); font-weight: 500; }
    .kpi-sub { font-size: 0.8rem; color: var(--nt-text-muted); margin-top: 2px; }

    .action-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px; margin-bottom: 24px;
    }
    .action-card { padding: 20px !important; display: flex; flex-direction: column; gap: 8px; }
    .action-card .action-head {
      display: flex; align-items: center; gap: 10px;
      mat-icon { color: var(--nt-text-muted); }
    }
    .action-card.has-work .action-head mat-icon { color: var(--nt-accent); }
    .action-card h3 { margin: 0; font-size: 1.05rem; font-weight: 600; }
    .action-card p { margin: 4px 0 12px; }
    .action-card button { align-self: flex-start; }

    .plan-card { padding: 20px !important; }
    .plan-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
      mat-icon { font-size: 32px; width: 32px; height: 32px; color: var(--nt-accent); }
      h3 { margin: 0; }
      p { margin: 4px 0 0; }
    }
    .plan-details { display: flex; gap: 32px; flex-wrap: wrap; margin-bottom: 16px; }
    .plan-label { font-size: 0.8rem; color: var(--nt-text-muted); }
    .plan-value { font-weight: 600; font-size: 1.05rem; }
    .plan-actions { display: flex; gap: 8px; flex-wrap: wrap; }

    .status-pill {
      display: inline-flex; padding: 3px 10px; border-radius: 12px;
      font-size: 0.75rem; font-weight: 700; letter-spacing: 0.02em;
    }
    .pill-active, .pill-trialing { background: #dcfce7; color: #166534; }
    .pill-past_due { background: #fee2e2; color: #991b1b; }
    .pill-canceled, .pill-suspended { background: var(--nt-surface-2); color: var(--nt-text-muted); }
  `]
})
export class NutriDashboardComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private practiceCtx = inject(PracticeService);

  loading = signal(true);
  metrics = signal<NutriMetricsOverview | null>(null);
  pendingMenus = signal<MenuPlan[]>([]);
  clientRequests = signal<ClientLink[]>([]);
  subscription = signal<Subscription | null>(null);

  firstName = computed(() => {
    const HONORIFICS = /^(dr|dra|sr|sra|srta|prof|profa)\.?$/i;
    const tokens = (this.auth.currentUser()?.name ?? '').trim().split(/\s+/).filter(Boolean);
    if (tokens.length === 0) return '';
    const first = tokens[0];
    if (HONORIFICS.test(first)) {
      const next = tokens.slice(1).find(t => !HONORIFICS.test(t));
      return next ? `${first} ${next}` : first;
    }
    return first;
  });
  needsAttention = computed(() => this.metrics()?.clientsNeedingAttention ?? 0);

  isPaidPlan(s: Subscription): boolean {
    if ((s.amountCents ?? 0) > 0) return true;
    return !!s.planTier && s.planTier.toUpperCase() !== 'FREE';
  }

  private firstLoad = true;
  constructor() {
    // Recarrega ao trocar a practice ativa (nao no primeiro run)
    effect(() => {
      this.practiceCtx.activePracticeId();
      if (this.firstLoad) { this.firstLoad = false; return; }
      this.load();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void { this.load(); }

  private load() {
    this.loading.set(true);
    const pid = this.practiceCtx.activePracticeId();
    forkJoin({
      metrics: this.api.nutriMetricsOverview(pid),
      pending: this.api.nutriPendingMenus(pid),
      requests: this.api.clientRequests(pid),
      sub: this.api.mySubscription()
    }).subscribe({
      next: r => {
        this.metrics.set(r.metrics);
        this.pendingMenus.set(r.pending);
        this.clientRequests.set(r.requests);
        this.subscription.set(r.sub);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
