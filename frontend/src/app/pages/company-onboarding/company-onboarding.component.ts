import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { OnboardingInfo } from '../../models/models';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';

@Component({
  selector: 'app-company-onboarding',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatCheckboxModule, MatIconModule, MatProgressSpinnerModule,
    TranslateModule, LanguageSwitcherComponent
  ],
  template: `
    <div class="auth-shell">
      <div class="auth-topbar"><app-language-switcher></app-language-switcher></div>
      <div class="auth-panel">
        <div class="brand-hero">
          <div class="brand-icon"><mat-icon>corporate_fare</mat-icon></div>
          <h1>{{ 'onboarding.title' | translate }}</h1>
          <p>{{ 'onboarding.subtitle' | translate }}</p>
        </div>

        <mat-card class="auth-card">
          @if (loadingLookup()) {
            <div class="center"><mat-spinner diameter="36"></mat-spinner></div>
          } @else if (lookupError()) {
            <div class="alert-error">
              <mat-icon>error_outline</mat-icon> {{ lookupError() }}
            </div>
            <div class="footer-link">
              <a routerLink="/login">{{ 'forgotPassword.backToLogin' | translate }}</a>
            </div>
          } @else if (info()) {
            <div class="company-info">
              <div class="company-name">
                <mat-icon>corporate_fare</mat-icon>
                <strong>{{ info()!.companyName }}</strong>
              </div>
              <div class="text-muted">
                {{ 'onboarding.invitedAs' | translate:{ role: (info()!.role === 'OWNER' ? 'onboarding.roleOwner' : 'onboarding.roleAdmin') | translate } }}
              </div>
              <div class="text-muted small">
                <mat-icon>mail</mat-icon> {{ info()!.invitedEmail }}
              </div>
            </div>

            @if (info()!.userExists) {
              <div class="alert-info">
                <mat-icon>info</mat-icon>
                {{ 'onboarding.userExists' | translate }}
              </div>
            }

            <div class="form">
              @if (!info()!.userExists) {
                <mat-form-field appearance="outline">
                  <mat-label>{{ 'onboarding.name' | translate }}</mat-label>
                  <input matInput [(ngModel)]="name">
                  <mat-icon matPrefix>person</mat-icon>
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ 'onboarding.password' | translate }}</mat-label>
                  <input matInput [type]="showPwd ? 'text' : 'password'" [(ngModel)]="password">
                  <mat-icon matPrefix>lock</mat-icon>
                  <button mat-icon-button matSuffix type="button" (click)="showPwd = !showPwd" tabindex="-1">
                    <mat-icon>{{ showPwd ? 'visibility_off' : 'visibility' }}</mat-icon>
                  </button>
                </mat-form-field>
              }

              <mat-checkbox [(ngModel)]="dataConsent" color="primary">
                {{ 'register.consent.checkbox' | translate }}
              </mat-checkbox>

              @if (error()) {
                <div class="alert-error"><mat-icon>error_outline</mat-icon> {{ error() }}</div>
              }

              <button mat-flat-button color="primary" class="submit-btn"
                      (click)="accept()" [disabled]="loading()">
                <mat-icon>check</mat-icon> {{ 'onboarding.accept' | translate }}
              </button>
            </div>
          }
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .auth-shell {
      min-height: 100vh; display: grid; place-items: center;
      background: var(--nt-hero-bg); padding: 24px; position: relative;
    }
    .auth-topbar { position: absolute; top: 16px; right: 16px; }
    .auth-panel { width: 100%; max-width: 480px; }

    .brand-hero { text-align: center; margin-bottom: 28px; }
    .brand-hero h1 { font-size: 1.75rem; margin: 12px 0 4px; font-weight: 700; letter-spacing: -0.02em; }
    .brand-hero p { margin: 0; color: var(--nt-text-muted); }
    .brand-icon {
      width: 56px; height: 56px;
      background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff;
      border-radius: 16px;
      display: inline-flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35);
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }
    .auth-card {
      padding: 32px !important; box-shadow: var(--nt-shadow-lg) !important; border: none !important;
      display: flex; flex-direction: column; gap: 20px;
    }
    .center { display: flex; justify-content: center; padding: 40px 0; }

    .company-info {
      background: var(--nt-surface-2);
      border-radius: 12px; padding: 16px;
      display: flex; flex-direction: column; gap: 4px;
    }
    .company-name {
      display: flex; align-items: center; gap: 8px;
      font-size: 1.2rem;
      mat-icon { color: var(--nt-primary); }
    }
    .small { font-size: 0.85rem; display: flex; align-items: center; gap: 4px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    .alert-info {
      display: flex; align-items: center; gap: 8px;
      background: rgba(13, 148, 136, 0.1);
      color: var(--nt-primary-dark);
      padding: 10px 14px; border-radius: 10px; font-size: 0.9rem;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }

    .form { display: flex; flex-direction: column; gap: 14px; }
    .form mat-form-field { width: 100%; }
    .submit-btn { width: 100%; height: 46px; font-weight: 600; }

    .footer-link { text-align: center;
      a { color: var(--nt-primary-dark); font-weight: 500; text-decoration: none; }
      a:hover { text-decoration: underline; }
    }
  `]
})
export class CompanyOnboardingComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private translate = inject(TranslateService);

  info = signal<OnboardingInfo | null>(null);
  loadingLookup = signal(true);
  lookupError = signal<string | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  name = '';
  password = '';
  showPwd = false;
  dataConsent = false;
  token = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.loadingLookup.set(false);
      this.lookupError.set(this.translate.instant('onboarding.errors.missingToken'));
      return;
    }
    this.api.onboardingLookup(this.token).subscribe({
      next: i => {
        this.loadingLookup.set(false);
        this.info.set(i);
        // Se user ja existe, nao precisa de name/password
      },
      error: err => {
        this.loadingLookup.set(false);
        this.lookupError.set(err?.error?.error ?? this.translate.instant('onboarding.errors.invalidToken'));
      }
    });
  }

  accept() {
    this.error.set(null);
    if (!this.dataConsent) {
      this.error.set(this.translate.instant('register.errors.noConsent')); return;
    }
    if (!this.info()!.userExists) {
      if (!this.name || !this.password || this.password.length < 6) {
        this.error.set(this.translate.instant('onboarding.errors.fillNamePassword')); return;
      }
    }
    this.loading.set(true);
    this.api.onboardingAccept(this.token, {
      name: this.name || 'Owner',
      password: this.password || 'PlaceholderPass1!',   // ignorado se user ja existe
      dataConsent: this.dataConsent
    }).subscribe({
      next: r => {
        // Login automatico
        localStorage.setItem('prandiumnutri.session', JSON.stringify({
          userId: r.userId, email: r.email, name: r.name,
          role: r.role, profileCompleted: r.profileCompleted, token: r.token
        }));
        window.location.href = '/company/dashboard';
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.error ?? this.translate.instant('onboarding.errors.acceptFail'));
      }
    });
  }
}
