import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Plan, PlanTarget, Pricing } from '../../models/models';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatButtonToggleModule,
    MatSelectModule, MatFormFieldModule, MatChipsModule, MatSnackBarModule, TranslateModule,
    LanguageSwitcherComponent
  ],
  template: `
    <div class="page">
      @if (!auth.isAuthenticated()) {
        <div class="pricing-topbar"><app-language-switcher></app-language-switcher></div>
      }
      @if (auth.isAdmin()) {
        <div class="admin-banner">
          <mat-icon>visibility</mat-icon>
          <div>
            <strong>{{ 'pricing.adminBannerTitle' | translate }}</strong>
            <div class="text-muted small">{{ 'pricing.adminBannerSub' | translate }}</div>
          </div>
        </div>
      }
      @if (limitReached()) {
        <div class="limit-banner">
          <mat-icon>lock</mat-icon>
          <div>
            <strong>{{ 'pricing.limitBannerTitle' | translate }}</strong>
            <div class="text-muted small">
              {{ 'pricing.limitBannerSub' | translate:{ feature: (limitFeature() ? ('billing.features.' + limitFeature() | translate) : '') } }}
            </div>
          </div>
        </div>
      }
      <div class="pricing-hero">
        <div class="brand-icon"><mat-icon>workspace_premium</mat-icon></div>
        <h1>{{ 'pricing.title' | translate }}</h1>
        <p>{{ 'pricing.subtitle' | translate }}</p>

        <div class="filters">
          <mat-button-toggle-group [value]="target" (change)="onTargetChange($event.value)" hideSingleSelectionIndicator>
            <mat-button-toggle value="USER">
              <mat-icon>person</mat-icon> {{ 'pricing.forClient' | translate }}
            </mat-button-toggle>
            <mat-button-toggle value="NUTRITIONIST">
              <mat-icon>medical_services</mat-icon> {{ 'pricing.forNutri' | translate }}
            </mat-button-toggle>
            <mat-button-toggle value="COMPANY">
              <mat-icon>corporate_fare</mat-icon> {{ 'pricing.forCompany' | translate }}
            </mat-button-toggle>
          </mat-button-toggle-group>

          <mat-form-field appearance="outline" class="currency" subscriptSizing="dynamic">
            <mat-label>{{ 'pricing.currency' | translate }}</mat-label>
            <mat-select [value]="currency" (selectionChange)="onCurrencyChange($event.value)">
              <mat-option value="BRL">🇧🇷 BRL</mat-option>
              <mat-option value="USD">🇺🇸 USD</mat-option>
              <mat-option value="EUR">🇪🇺 EUR</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </div>

      @if (pricing()) {
        <div class="plans-grid">
          @for (p of pricing()!.plans; track p.code) {
            <mat-card class="plan-card" [class]="'tier-' + p.tier.toLowerCase()">
              <div class="plan-head">
                <mat-icon>{{ tierIcon(p.tier) }}</mat-icon>
                <div>
                  <div class="tier">{{ p.tier }}</div>
                  <div class="plan-name">{{ p.name }}</div>
                </div>
              </div>
              <div class="plan-price">
                @if (p.amountCents === 0) {
                  <span class="free">{{ 'billing.free' | translate }}</span>
                } @else {
                  <span class="amount">{{ formatPrice(p.amountCents, p.currency) }}</span>
                  <span class="period">/ {{ 'billing.perMonth' | translate }}</span>
                }
              </div>
              <mat-card-content>
                <ul class="feature-list">
                  @for (f of listFeatures(p.features); track f.key) {
                    <li>
                      <mat-icon>{{ featureIconOk(f) }}</mat-icon>
                      <span>{{ formatFeature(f) }}</span>
                    </li>
                  }
                </ul>
              </mat-card-content>
              @if (!auth.isAdmin()) {
                <mat-card-actions>
                  <button mat-flat-button color="primary" class="cta"
                          (click)="upgrade(p)"
                          [disabled]="p.amountCents === 0 || loading()">
                    {{ (p.amountCents === 0 ? 'pricing.freeCta' : 'pricing.upgradeCta') | translate }}
                  </button>
                </mat-card-actions>
              }
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .pricing-topbar { position: absolute; top: 16px; right: 16px; z-index: 1; }
    .admin-banner {
      display: flex; align-items: center; gap: 12px;
      background: #dbeafe; color: #1e40af;
      padding: 14px 20px; border-radius: var(--nt-radius);
      margin-bottom: 16px;
      mat-icon { color: #1d4ed8; }
      .small { color: #1e3a8a; font-size: 0.85rem; }
    }
    .limit-banner {
      display: flex; align-items: center; gap: 12px;
      background: #fef3c7; color: #92400e;
      padding: 14px 20px; border-radius: var(--nt-radius);
      margin-bottom: 16px;
      mat-icon { color: #b45309; }
      .small { color: #78350f; font-size: 0.85rem; }
    }
    .pricing-hero {
      text-align: center; padding: 60px 20px 24px;
      background: var(--nt-hero-bg);
      border-radius: var(--nt-radius);
      margin-bottom: 24px; position: relative;
    }
    .brand-icon {
      width: 64px; height: 64px;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff; border-radius: 20px;
      display: inline-flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 20px rgba(13, 148, 136, 0.35);
      mat-icon { font-size: 32px; width: 32px; height: 32px; }
    }
    h1 { font-size: 2rem; margin: 16px 0 8px; font-weight: 700; letter-spacing: -0.02em; color: var(--nt-text); }
    .pricing-hero p { color: var(--nt-text-muted); margin: 0 auto 20px; max-width: 640px; }
    .filters {
      display: flex; justify-content: center; align-items: center; gap: 12px; flex-wrap: wrap;
      margin-top: 20px;
    }
    .currency { width: 140px; }

    .plans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 20px;
    }
    .plan-card {
      padding: 24px !important; display: flex; flex-direction: column; gap: 16px;
      border-top: 6px solid var(--nt-border) !important;
      &.tier-free { border-top-color: #9ca3af !important; }
      &.tier-starter { border-top-color: #3b82f6 !important; }
      &.tier-growth { border-top-color: var(--nt-primary) !important; }
      &.tier-business { border-top-color: #f97316 !important; }
      &.tier-pro, &.tier-plus { border-top-color: #8b5cf6 !important; }
    }
    .plan-head { display: flex; align-items: center; gap: 12px;
      mat-icon { color: var(--nt-primary); font-size: 28px; width: 28px; height: 28px; }
    }
    .tier { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--nt-text-muted); }
    .plan-name { font-size: 1.25rem; font-weight: 700; }

    .plan-price {
      display: flex; align-items: baseline; gap: 4px;
      padding: 12px 0; border-bottom: 1px solid var(--nt-border);
      .free { font-size: 1.5rem; font-weight: 700; color: var(--nt-primary-dark); }
      .amount { font-size: 2rem; font-weight: 700; letter-spacing: -0.02em; }
      .period { color: var(--nt-text-muted); font-size: 0.9rem; }
    }

    .feature-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px;
      li { display: flex; align-items: center; gap: 8px; font-size: 0.9rem; }
      mat-icon { font-size: 20px; width: 20px; height: 20px; color: var(--nt-primary); }
    }
    .cta { width: 100%; height: 42px; font-weight: 600; }
    .soon-note { font-size: 0.75rem; text-align: center; margin-top: 6px; }
  `]
})
export class PricingComponent implements OnInit {
  private api = inject(ApiService);
  auth = inject(AuthService);
  private translate = inject(TranslateService);
  private snack = inject(MatSnackBar);
  private router = inject(Router);

  private route = inject(ActivatedRoute);
  pricing = signal<Pricing | null>(null);
  currency = 'BRL';
  target: PlanTarget = 'NUTRITIONIST';
  loading = signal(false);
  limitReached = signal(false);
  limitFeature = signal<string | null>(null);
  private reqSeq = 0;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    if (params.get('feature')) {
      this.limitReached.set(true);
      this.limitFeature.set(params.get('feature'));
    }
    const u = this.auth.currentUser();
    if (u?.role === 'ADMIN') this.target = 'USER';
    else if (u?.role === 'NUTRITIONIST') this.target = 'NUTRITIONIST';
    else if (u?.role === 'COMPANY') this.target = 'COMPANY';
    else this.target = 'USER';

    this.api.detectCurrency().subscribe(r => {
      this.currency = r.currency || 'BRL';
      this.reload();
    });
  }

  onTargetChange(value: PlanTarget) {
    if (value === this.target) return;
    this.target = value;
    this.reload();
  }

  onCurrencyChange(value: string) {
    if (value === this.currency) return;
    this.currency = value;
    this.reload();
  }

  reload() {
    const seq = ++this.reqSeq;
    this.pricing.set(null);
    this.api.pricing(this.currency, this.target).subscribe(p => {
      if (seq === this.reqSeq) this.pricing.set(p);
    });
  }

  upgrade(p: Plan) {
    if (p.amountCents === 0) return;
    if (!this.auth.isAuthenticated()) { this.router.navigate(['/login']); return; }
    this.loading.set(true);
    this.api.createCheckout(p.code, p.currency).subscribe({
      next: r => window.location.href = r.checkoutUrl,
      error: err => {
        this.loading.set(false);
        this.snack.open(err?.error?.error ?? 'Falha ao iniciar checkout', 'OK', { duration: 4000 });
      }
    });
  }

  listFeatures(f: Record<string, any>) {
    return Object.entries(f || {}).map(([key, value]) => ({ key, value }));
  }

  featureIconOk(f: { key: string; value: any }): string {
    if (f.value === null || f.value === undefined) return 'check_circle';
    if (typeof f.value === 'boolean') return f.value ? 'check_circle' : 'cancel';
    return 'check_circle';
  }

  formatFeature(f: { key: string; value: any }): string {
    const label = this.translate.instant('billing.features.' + f.key);
    if (f.value === null || f.value === undefined) {
      return `${this.translate.instant('billing.unlimited')} · ${label}`;
    }
    if (typeof f.value === 'boolean') {
      return f.value ? label : `${label} (${this.translate.instant('billing.notIncluded')})`;
    }
    return `${f.value} · ${label}`;
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
}
