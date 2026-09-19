import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { Subscription } from '../../models/models';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule,
    MatChipsModule, MatSnackBarModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'billing.title' | translate }}</h1>
          <p>{{ 'billing.subtitle' | translate }}</p>
        </div>
        <a mat-flat-button color="primary" routerLink="/pricing">
          <mat-icon>upgrade</mat-icon> {{ 'billing.viewAllPlans' | translate }}
        </a>
      </div>

      @if (sub()) {
        <mat-card class="plan-card">
          <div class="plan-hero" [class]="'tier-' + sub()!.planTier.toLowerCase()">
            <div class="hero-icon"><mat-icon>{{ tierIcon(sub()!.planTier) }}</mat-icon></div>
            <div class="hero-info">
              <div class="tier-label">{{ 'billing.currentPlan' | translate }}</div>
              <div class="plan-name">{{ sub()!.planName }}</div>
              <div class="plan-price">
                @if (sub()!.amountCents === 0) {
                  <span class="free">{{ 'billing.free' | translate }}</span>
                } @else {
                  <span class="amount">{{ formatPrice(sub()!.amountCents, sub()!.currency) }}</span>
                  <span class="period">/ {{ 'billing.perMonth' | translate }}</span>
                }
              </div>
            </div>
            <mat-chip [class]="'status-' + sub()!.status.toLowerCase()">
              {{ 'billing.statusMap.' + sub()!.status | translate }}
            </mat-chip>
          </div>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>
              <mat-icon>tune</mat-icon> {{ 'billing.limitsUsage' | translate }}
            </mat-card-title>
            <mat-card-subtitle>{{ 'billing.limitsSubtitle' | translate }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content class="limits-list">
            @for (feature of listFeatures(sub()!.features); track feature.key) {
              <div class="limit-row">
                <div class="limit-info">
                  <mat-icon>{{ featureIcon(feature.key) }}</mat-icon>
                  <div>
                    <div class="limit-label">{{ 'billing.features.' + feature.key | translate }}</div>
                    <div class="limit-value">
                      @if (feature.value === null || feature.value === undefined) {
                        <span class="unlimited">{{ 'billing.unlimited' | translate }}</span>
                      } @else if (isBoolean(feature.value)) {
                        <span [class.included]="feature.value">
                          {{ (feature.value ? 'billing.included' : 'billing.notIncluded') | translate }}
                        </span>
                      } @else {
                        {{ usageFor(feature.key) }} / {{ feature.value }}
                      }
                    </div>
                  </div>
                </div>
                @if (isNumber(feature.value)) {
                  <mat-progress-bar mode="determinate"
                                    [value]="pct(usageFor(feature.key), feature.value)"
                                    [color]="pct(usageFor(feature.key), feature.value) > 80 ? 'warn' : 'primary'">
                  </mat-progress-bar>
                }
              </div>
            }
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>
              <mat-icon>receipt_long</mat-icon> {{ 'billing.periodInfo' | translate }}
            </mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="info-row">
              <span class="text-muted">{{ 'billing.currency' | translate }}</span>
              <strong>{{ sub()!.currency }}</strong>
            </div>
            <div class="info-row">
              <span class="text-muted">{{ 'billing.provider' | translate }}</span>
              <strong>{{ sub()!.provider }}</strong>
            </div>
            <div class="info-row">
              <span class="text-muted">{{ 'billing.periodStart' | translate }}</span>
              <strong>{{ sub()!.currentPeriodStart | date:'short' }}</strong>
            </div>
            @if (sub()!.provider !== 'MANUAL') {
              <div class="info-row">
                <span class="text-muted">{{ 'billing.nextRenewal' | translate }}</span>
                <strong>{{ sub()!.currentPeriodEnd | date:'short' }}</strong>
              </div>
            }
          </mat-card-content>
          @if (sub()!.provider !== 'MANUAL' && !sub()!.cancelAtPeriodEnd) {
            <mat-card-actions align="end">
              <button mat-stroked-button color="warn" (click)="cancel()">
                <mat-icon>cancel</mat-icon> {{ 'billing.cancelPlan' | translate }}
              </button>
            </mat-card-actions>
          } @else if (sub()!.cancelAtPeriodEnd) {
            <mat-card-actions>
              <div class="cancel-scheduled">
                <mat-icon>schedule</mat-icon>
                {{ 'billing.cancelScheduled' | translate:{ date: (sub()!.currentPeriodEnd | date:'short') } }}
              </div>
            </mat-card-actions>
          }
        </mat-card>
      } @else {
        <div class="empty card-surface">
          <mat-icon>credit_card_off</mat-icon>
          <p>{{ 'billing.noSubscription' | translate }}</p>
          <a mat-flat-button color="primary" routerLink="/pricing">
            <mat-icon>upgrade</mat-icon> {{ 'billing.viewAllPlans' | translate }}
          </a>
        </div>
      }
    </div>
  `,
  styles: [`
    mat-card-title { display: flex; align-items: center; gap: 8px;
      mat-icon { color: var(--nt-primary); }
    }

    .plan-card { padding: 0 !important; overflow: hidden; }
    .plan-hero {
      display: flex; align-items: center; gap: 20px;
      padding: 24px;
      color: #fff;
      background: linear-gradient(135deg, #6b7280, #4b5563);

      &.tier-free { background: linear-gradient(135deg, #6b7280, #4b5563); }
      &.tier-starter { background: linear-gradient(135deg, #3b82f6, #2563eb); }
      &.tier-growth { background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark)); }
      &.tier-pro { background: linear-gradient(135deg, #8b5cf6, #6366f1); }
      &.tier-plus { background: linear-gradient(135deg, #8b5cf6, #6366f1); }
      &.tier-business { background: linear-gradient(135deg, #f97316, #ea580c); }
    }
    .hero-icon {
      width: 64px; height: 64px;
      background: rgba(255,255,255,0.2);
      border-radius: 16px;
      display: flex; align-items: center; justify-content: center;
      mat-icon { font-size: 36px; width: 36px; height: 36px; }
    }
    .hero-info { flex: 1; }
    .tier-label { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; opacity: 0.9; }
    .plan-name { font-size: 1.8rem; font-weight: 700; letter-spacing: -0.02em; margin: 2px 0; }
    .plan-price {
      display: flex; align-items: baseline; gap: 4px;
      .free { font-size: 1.2rem; font-weight: 600; }
      .amount { font-size: 1.5rem; font-weight: 700; }
      .period { opacity: 0.85; }
    }
    .status-active { background: rgba(255,255,255,0.25) !important; color: #fff !important; font-weight: 600; }
    .status-trialing { background: rgba(251,191,36,0.9) !important; color: #422006 !important; }
    .status-past_due, .status-suspended { background: rgba(220,38,38,0.9) !important; color: #fff !important; }

    .limits-list { display: flex; flex-direction: column; gap: 20px; }
    .limit-row { display: flex; flex-direction: column; gap: 8px; }
    .limit-info { display: flex; align-items: center; gap: 12px;
      mat-icon { color: var(--nt-primary-dark); }
    }
    .limit-label { font-weight: 600; }
    .limit-value { color: var(--nt-text-muted); font-size: 0.9rem;
      .unlimited { color: var(--nt-primary-dark); font-weight: 600; }
      .included { color: var(--nt-primary-dark); font-weight: 600; }
    }

    .info-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 8px 0; border-bottom: 1px solid var(--nt-border);
      &:last-child { border-bottom: none; }
    }
    .cancel-scheduled {
      display: flex; align-items: center; gap: 8px;
      background: #fef3c7; color: #92400e;
      padding: 10px 14px; border-radius: 10px; font-size: 0.9rem;
      width: 100%;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }
  `]
})
export class BillingComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);
  sub = signal<Subscription | null>(null);

  ngOnInit(): void { this.reload(); }

  reload() { this.api.mySubscription().subscribe(s => this.sub.set(s)); }

  cancel() {
    if (!confirm(this.translate.instant('billing.cancelConfirm'))) return;
    this.api.cancelSubscription(true).subscribe({
      next: r => {
        this.snack.open(r.message || this.translate.instant('billing.canceledOk'),
                        'OK', { duration: 3500 });
        setTimeout(() => this.reload(), 1500);
      },
      error: err => this.snack.open(err?.error?.error ?? 'Erro', 'OK', { duration: 3500 })
    });
  }

  listFeatures(features: Record<string, any>): { key: string; value: any }[] {
    if (!features) return [];
    return Object.entries(features).map(([k, v]) => ({ key: k, value: v }));
  }

  usageFor(key: string): number {
    return this.sub()?.currentMonthUsage?.[key] ?? 0;
  }

  pct(used: number, limit: number): number {
    if (!limit) return 0;
    return Math.min(100, Math.round((used / limit) * 100));
  }

  formatPrice(cents: number, currency: string): string {
    const val = cents / 100;
    const locale = currency === 'BRL' ? 'pt-BR' : currency === 'EUR' ? 'fr-FR' : 'en-US';
    return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(val);
  }

  tierIcon(tier: string): string {
    switch (tier?.toUpperCase()) {
      case 'FREE': return 'card_giftcard';
      case 'STARTER': return 'rocket_launch';
      case 'GROWTH': return 'trending_up';
      case 'BUSINESS': return 'workspace_premium';
      case 'PRO': return 'stars';
      case 'PLUS': return 'add_circle';
      default: return 'credit_card';
    }
  }

  featureIcon(key: string): string {
    switch (key) {
      case 'maxClients': return 'groups';
      case 'maxNutritionists': return 'medical_services';
      case 'maxClientsPerNutri': return 'group';
      case 'aiSuggestionsMonthly': return 'auto_awesome';
      case 'menuLimit': return 'menu_book';
      case 'customBranding': return 'brush';
      default: return 'check_circle';
    }
  }

  isBoolean(v: any): boolean { return typeof v === 'boolean'; }
  isNumber(v: any): boolean { return typeof v === 'number'; }
}
