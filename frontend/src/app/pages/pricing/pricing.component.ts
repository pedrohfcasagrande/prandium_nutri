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
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Plan, PlanTarget, Pricing } from '../../models/models';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';

@Component({
  selector: 'app-all-plans-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, TranslateModule],
  template: `
    <div class="dlg-head">
      <div class="title-row">
        <mat-icon>view_list</mat-icon>
        <h2>{{ 'pricing.allPlansTitle' | translate }}</h2>
      </div>
      <button mat-icon-button (click)="ref.close()" aria-label="Fechar" class="close">
        <mat-icon>close</mat-icon>
      </button>
    </div>
    <mat-dialog-content class="all">
      @if (plans()) {
        @for (group of groups; track group.target) {
          <section class="group">
            <div class="group-head">
              <mat-icon>{{ group.icon }}</mat-icon>
              <h3>{{ ('pricing.for' + group.target) | translate }}</h3>
            </div>
            <div class="grid">
              @for (p of byTarget(group.target); track p.code) {
                <div class="plan" [class]="'tier-' + p.tier.toLowerCase()">
                  <div class="tier-badge">{{ p.tier }}</div>
                  <div class="name">{{ p.name }}</div>
                  <div class="price">
                    @if (p.amountCents === 0) {
                      <strong>{{ 'pricing.free' | translate }}</strong>
                    } @else {
                      <strong>{{ formatPrice(p.amountCents, p.currency) }}</strong>
                      <span class="per">/ {{ 'pricing.perMonth' | translate }}</span>
                    }
                  </div>
                  <ul class="feat">
                    @for (f of topFeatures(p); track f) {
                      <li><mat-icon>check</mat-icon> {{ f }}</li>
                    }
                  </ul>
                </div>
              }
            </div>
          </section>
        }
      } @else {
        <div class="loading"><mat-icon>hourglass_empty</mat-icon></div>
      }
    </mat-dialog-content>
  `,
  styles: [`
    .dlg-head { display: flex; align-items: center; justify-content: space-between;
      padding: 20px 24px 8px; }
    .title-row { display: flex; align-items: center; gap: 10px;
      h2 { margin: 0; font-size: 1.15rem; font-weight: 600; }
      mat-icon { color: var(--nt-primary); }
    }
    .close mat-icon { color: var(--nt-text-muted); }

    .all {
      width: min(92vw, 960px); max-height: 75vh; padding: 8px 24px 20px !important;
    }

    .group { margin-bottom: 24px;
      &:last-child { margin-bottom: 0; }
    }
    .group-head { display: flex; align-items: center; gap: 8px; margin: 0 0 10px;
      h3 { margin: 0; font-size: 0.85rem; font-weight: 700; color: var(--nt-text-muted);
        text-transform: uppercase; letter-spacing: 0.06em; }
      mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--nt-text-muted); }
    }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
    .plan {
      padding: 14px 16px; border: 1px solid var(--nt-border); border-radius: 12px;
      background: var(--nt-surface); display: flex; flex-direction: column; gap: 8px;
      border-top-width: 3px;
    }
    .plan.tier-free { border-top-color: #9ca3af; }
    .plan.tier-starter { border-top-color: #3b82f6; }
    .plan.tier-growth { border-top-color: var(--nt-primary); }
    .plan.tier-business { border-top-color: #f97316; }
    .plan.tier-pro, .plan.tier-plus { border-top-color: #8b5cf6; }

    .tier-badge {
      align-self: flex-start;
      font-size: 0.62rem; font-weight: 700; letter-spacing: 0.08em;
      padding: 2px 8px; border-radius: 999px;
      background: var(--nt-surface-2); color: var(--nt-text-muted);
      text-transform: uppercase;
    }
    .name { font-weight: 600; font-size: 0.95rem; }
    .price { display: flex; align-items: baseline; gap: 4px;
      strong { font-size: 1.25rem; letter-spacing: -0.01em; }
      .per { font-size: 0.75rem; color: var(--nt-text-muted); }
    }

    .feat { list-style: none; margin: 4px 0 0; padding: 0;
      display: flex; flex-direction: column; gap: 4px;
      li { display: flex; align-items: center; gap: 6px; font-size: 0.78rem;
        color: var(--nt-text); line-height: 1.35; }
      mat-icon { font-size: 14px; width: 14px; height: 14px; color: var(--nt-primary); flex-shrink: 0; }
    }

    .loading { padding: 60px; text-align: center; opacity: 0.6; }
  `]
})
export class AllPlansDialogComponent {
  ref = inject(MatDialogRef<AllPlansDialogComponent>);
  private api = inject(ApiService);
  private translate = inject(TranslateService);
  plans = signal<Plan[] | null>(null);
  private currency: string;

  readonly groups = [
    { target: 'USER',         icon: 'person' },
    { target: 'NUTRITIONIST', icon: 'medical_services' },
    { target: 'COMPANY',      icon: 'corporate_fare' }
  ];

  constructor() {
    this.currency = navigator.language.startsWith('pt') ? 'BRL'
                  : navigator.language.startsWith('fr') ? 'EUR' : 'USD';
    this.api.pricing(this.currency).subscribe(p => this.plans.set(p.plans));
  }

  byTarget(target: string): Plan[] {
    return (this.plans() ?? []).filter(p => p.target === target);
  }

  /** Escolhe 3 features mais relevantes pra caber no card compacto. */
  topFeatures(p: Plan): string[] {
    const entries = Object.entries(p.features || {}).slice(0, 3);
    return entries.map(([k, v]) => {
      const label = this.translate.instant('billing.features.' + k);
      if (v === null || v === undefined) return `${this.translate.instant('billing.unlimited')} · ${label}`;
      if (typeof v === 'boolean') return v ? label : `${label} (${this.translate.instant('billing.notIncluded')})`;
      return `${v} · ${label}`;
    });
  }

  formatPrice(cents: number, currency: string): string {
    const val = cents / 100;
    const locale = currency === 'BRL' ? 'pt-BR' : currency === 'EUR' ? 'fr-FR' : 'en-US';
    return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(val);
  }
}

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatButtonToggleModule,
    MatSelectModule, MatFormFieldModule, MatChipsModule, MatSnackBarModule,
    MatDialogModule, TranslateModule,
    LanguageSwitcherComponent
  ],
  template: `
    <div class="page">
      @if (!auth.isAuthenticated()) {
        <div class="pricing-topbar"><app-language-switcher></app-language-switcher></div>
      }

      <div class="page-header">
        <div>
          <h1>{{ 'pricing.title' | translate }}</h1>
          <p>{{ 'pricing.subtitle' | translate }}</p>
        </div>
        <div class="filters">
          @if (!auth.isAuthenticated()) {
            <mat-button-toggle-group [value]="target" (change)="onTargetChange($event.value)" hideSingleSelectionIndicator>
              <mat-button-toggle value="USER">
                <mat-icon>person</mat-icon>
              </mat-button-toggle>
              <mat-button-toggle value="NUTRITIONIST">
                <mat-icon>medical_services</mat-icon>
              </mat-button-toggle>
              <mat-button-toggle value="COMPANY">
                <mat-icon>corporate_fare</mat-icon>
              </mat-button-toggle>
            </mat-button-toggle-group>
          } @else if (!auth.isAdmin()) {
            <button mat-stroked-button (click)="openAllPlans()">
              <mat-icon>view_list</mat-icon> {{ 'pricing.viewAll' | translate }}
            </button>
          }
          <mat-form-field appearance="outline" class="currency" subscriptSizing="dynamic">
            <mat-select [value]="currency" (selectionChange)="onCurrencyChange($event.value)">
              <mat-option value="BRL">🇧🇷 BRL</mat-option>
              <mat-option value="USD">🇺🇸 USD</mat-option>
              <mat-option value="EUR">🇪🇺 EUR</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </div>

      @if (auth.isAdmin()) {
        <div class="banner banner-info">
          <mat-icon>visibility</mat-icon>
          <div>
            <strong>{{ 'pricing.adminBannerTitle' | translate }}</strong>
            <div class="small">{{ 'pricing.adminBannerSub' | translate }}</div>
          </div>
        </div>
      }
      @if (limitReached()) {
        <div class="banner banner-warn">
          <mat-icon>lock</mat-icon>
          <div>
            <strong>{{ 'pricing.limitBannerTitle' | translate }}</strong>
            <div class="small">
              {{ 'pricing.limitBannerSub' | translate:{ feature: (limitFeature() ? ('billing.features.' + limitFeature() | translate) : '') } }}
            </div>
          </div>
        </div>
      }

      @if (pricing()) {
        @if (target === null) {
          <!-- Admin/visao consolidada: agrupa por tipo -->
          @for (group of targetGroups; track group.target) {
            @if (plansByTarget(group.target).length) {
              <section class="target-section">
                <div class="target-head">
                  <mat-icon>{{ group.icon }}</mat-icon>
                  <h2>{{ ('pricing.for' + group.target) | translate }}</h2>
                </div>
                <div class="plans-grid">
                  @for (p of plansByTarget(group.target); track p.code) {
                    <ng-container *ngTemplateOutlet="planCard; context: { $implicit: p }"></ng-container>
                  }
                </div>
              </section>
            }
          }
        } @else {
          <div class="plans-grid">
            @for (p of pricing()!.plans; track p.code) {
              <ng-container *ngTemplateOutlet="planCard; context: { $implicit: p }"></ng-container>
            }
          </div>
        }
      }

      <ng-template #planCard let-p>
        <mat-card class="plan-card" [class]="'tier-' + p.tier.toLowerCase()">
          <div class="plan-head">
            <div class="tier-badge">{{ p.tier }}</div>
            <div class="plan-name">{{ p.name }}</div>
          </div>
          <div class="plan-price">
            @if (p.amountCents === 0) {
              <span class="free">{{ 'billing.free' | translate }}</span>
            } @else {
              <span class="amount">{{ formatPrice(p.amountCents, p.currency) }}</span>
              <span class="period">/ {{ 'billing.perMonth' | translate }}</span>
            }
          </div>
          <ul class="feature-list">
            @for (f of listFeatures(p.features); track f.key) {
              <li>
                <mat-icon>{{ featureIconOk(f) }}</mat-icon>
                <span>{{ formatFeature(f) }}</span>
              </li>
            }
          </ul>
          @if (!auth.isAdmin()) {
            <button mat-flat-button color="primary" class="cta"
                    (click)="upgrade(p)"
                    [disabled]="isCurrentPlan(p) || p.amountCents === 0 || loading()">
              {{ (isCurrentPlan(p) ? 'pricing.currentPlan'
                   : p.amountCents === 0 ? 'pricing.freeCta'
                   : 'pricing.upgradeCta') | translate }}
            </button>
          }
        </mat-card>
      </ng-template>
    </div>
  `,
  styles: [`
    .pricing-topbar { position: absolute; top: 16px; right: 16px; z-index: 1; }

    .filters { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .currency { width: 120px; }

    .banner {
      display: flex; align-items: flex-start; gap: 12px;
      padding: 12px 16px; border-radius: 12px;
      mat-icon { flex-shrink: 0; margin-top: 2px; }
      strong { display: block; font-size: 0.9rem; }
      .small { font-size: 0.8rem; opacity: 0.85; margin-top: 2px; }
    }
    .banner-info { background: #dbeafe; color: #1e40af; mat-icon { color: #1d4ed8; } }
    .banner-warn { background: #fef3c7; color: #92400e; mat-icon { color: #b45309; } }

    .plans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }

    .target-section { display: flex; flex-direction: column; gap: 12px;
      & + .target-section { margin-top: 8px; }
    }
    .target-head { display: flex; align-items: center; gap: 8px;
      h2 { margin: 0; font-size: 0.85rem; font-weight: 700; color: var(--nt-text-muted);
        text-transform: uppercase; letter-spacing: 0.06em; }
      mat-icon { color: var(--nt-text-muted); font-size: 18px; width: 18px; height: 18px; }
    }
    .plan-card {
      padding: 20px !important; display: flex; flex-direction: column; gap: 14px;
      border-top: 3px solid var(--nt-border) !important;
      &.tier-free { border-top-color: #9ca3af !important; }
      &.tier-starter { border-top-color: #3b82f6 !important; }
      &.tier-growth { border-top-color: var(--nt-primary) !important; }
      &.tier-business { border-top-color: #f97316 !important; }
      &.tier-pro, &.tier-plus { border-top-color: #8b5cf6 !important; }
    }

    .plan-head { display: flex; align-items: center; gap: 10px; }
    .tier-badge {
      font-size: 0.65rem; font-weight: 700; letter-spacing: 0.08em;
      padding: 3px 8px; border-radius: 999px;
      background: var(--nt-surface-2); color: var(--nt-text-muted);
      text-transform: uppercase;
    }
    .plan-name { font-size: 1.05rem; font-weight: 600; }

    .plan-price {
      display: flex; align-items: baseline; gap: 4px;
      padding-bottom: 12px; border-bottom: 1px solid var(--nt-border);
      .free { font-size: 1.15rem; font-weight: 700; color: var(--nt-primary-dark); }
      .amount { font-size: 1.5rem; font-weight: 700; letter-spacing: -0.02em; }
      .period { color: var(--nt-text-muted); font-size: 0.8rem; }
    }

    .feature-list {
      list-style: none; margin: 0; padding: 0;
      display: flex; flex-direction: column; gap: 6px;
      flex: 1;
      li { display: flex; align-items: center; gap: 6px; font-size: 0.82rem; line-height: 1.35; }
      mat-icon { font-size: 16px; width: 16px; height: 16px; color: var(--nt-primary); flex-shrink: 0; }
    }
    .cta { width: 100%; height: 38px; font-weight: 600; font-size: 0.85rem; }
  `]
})
export class PricingComponent implements OnInit {
  private api = inject(ApiService);
  auth = inject(AuthService);
  private translate = inject(TranslateService);
  private snack = inject(MatSnackBar);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  openAllPlans() {
    this.dialog.open(AllPlansDialogComponent, { autoFocus: false, restoreFocus: true });
  }

  private route = inject(ActivatedRoute);
  pricing = signal<Pricing | null>(null);
  currency = 'BRL';
  /** null = todos os targets (Admin vê tudo; visitante seleciona no toggle). */
  target: PlanTarget | null = 'NUTRITIONIST';
  loading = signal(false);
  limitReached = signal(false);
  limitFeature = signal<string | null>(null);
  currentPlanCode = signal<string | null>(null);
  private reqSeq = 0;

  isCurrentPlan(p: Plan): boolean {
    return this.currentPlanCode() === p.code;
  }

  /** Grupos ordenados exibidos quando `target === null` (admin). */
  readonly targetGroups: { target: PlanTarget; icon: string }[] = [
    { target: 'USER',         icon: 'person' },
    { target: 'NUTRITIONIST', icon: 'medical_services' },
    { target: 'COMPANY',      icon: 'corporate_fare' }
  ];

  plansByTarget(t: PlanTarget): Plan[] {
    return (this.pricing()?.plans ?? []).filter(p => p.target === t);
  }

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    if (params.get('feature')) {
      this.limitReached.set(true);
      this.limitFeature.set(params.get('feature'));
    }
    const u = this.auth.currentUser();
    if (u?.role === 'ADMIN') this.target = null;               // vê todos
    else if (u?.role === 'NUTRITIONIST') this.target = 'NUTRITIONIST';
    else if (u?.role === 'COMPANY') this.target = 'COMPANY';
    else this.target = 'USER';

    this.api.detectCurrency().subscribe(r => {
      this.currency = r.currency || 'BRL';
      this.reload();
    });

    // Se logado, busca sub atual pra destacar "Plano atual" na CTA
    if (this.auth.isAuthenticated()) {
      this.api.mySubscription().subscribe({
        next: sub => this.currentPlanCode.set(sub?.planCode ?? null),
        error: () => {}
      });
    }
  }

  onTargetChange(value: PlanTarget | null) {
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
    // "1 cardapio salvo" em vez de "1 · Cardapios salvos" — mais legivel
    return `${f.value} ${label.toLowerCase()}`;
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
