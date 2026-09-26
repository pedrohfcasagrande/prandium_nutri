import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';
import { ThemeSwitcherComponent } from '../../components/theme-switcher.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatCheckboxModule, MatIconModule, MatSnackBarModule,
    TranslateModule, LanguageSwitcherComponent, ThemeSwitcherComponent
  ],
  template: `
    <div class="auth-shell">
      <div class="auth-topbar">
        <app-language-switcher></app-language-switcher>
        <app-theme-switcher></app-theme-switcher>
      </div>
      <div class="auth-panel">
        <div class="brand-hero">
          <div class="brand-icon"><mat-icon>eco</mat-icon></div>
          <h1>{{ 'register.title' | translate }}</h1>
          <p>{{ 'register.subtitle' | translate }}</p>
        </div>

        <mat-card class="auth-card">
          <div class="form">
            <div class="type-toggle">
              <div class="type-label">{{ 'register.iAm' | translate }}</div>
              <div class="type-buttons">
                <button type="button" mat-stroked-button
                        [class.active]="role === 'USER'"
                        (click)="role = 'USER'">
                  <mat-icon>person</mat-icon> {{ 'register.iAmClient' | translate }}
                </button>
                <button type="button" mat-stroked-button
                        [class.active]="role === 'NUTRITIONIST'"
                        (click)="role = 'NUTRITIONIST'">
                  <mat-icon>medical_services</mat-icon> {{ 'register.iAmNutri' | translate }}
                </button>
              </div>
            </div>

            <mat-form-field appearance="outline">
              <mat-label>{{ 'register.name' | translate }}</mat-label>
              <input matInput [(ngModel)]="name" autocomplete="name">
              <mat-icon matPrefix>person</mat-icon>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ 'register.email' | translate }}</mat-label>
              <input matInput type="email" [(ngModel)]="email" autocomplete="email">
              <mat-icon matPrefix>mail</mat-icon>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ 'register.cpf' | translate }}</mat-label>
              <input matInput inputmode="numeric" [(ngModel)]="cpf"
                     [placeholder]="'register.cpfHint' | translate"
                     (input)="onCpfInput($event)" maxlength="14">
              <mat-icon matPrefix>badge</mat-icon>
            </mat-form-field>

            @if (role === 'NUTRITIONIST') {
              <mat-form-field appearance="outline">
                <mat-label>{{ 'register.crn' | translate }}</mat-label>
                <input matInput [(ngModel)]="crn"
                       [placeholder]="'register.crnHint' | translate">
                <mat-icon matPrefix>medical_information</mat-icon>
              </mat-form-field>
            }

            <mat-form-field appearance="outline">
              <mat-label>{{ 'register.password' | translate }}</mat-label>
              <input matInput [type]="showPwd ? 'text' : 'password'"
                     [(ngModel)]="password" autocomplete="new-password">
              <mat-icon matPrefix>lock</mat-icon>
              <button mat-icon-button matSuffix type="button" (click)="showPwd = !showPwd" tabindex="-1"
                      [attr.aria-label]="(showPwd ? 'login.hidePassword' : 'login.showPassword') | translate">
                <mat-icon>{{ showPwd ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <div class="consent-box">
              <div class="consent-header">
                <mat-icon>privacy_tip</mat-icon>
                <strong>{{ 'register.consent.title' | translate }}</strong>
              </div>
              <p>{{ 'register.consent.text' | translate }}</p>
              <mat-checkbox [(ngModel)]="dataConsent" color="primary">
                {{ 'register.consent.checkbox' | translate }}
              </mat-checkbox>
            </div>

            @if (error()) {
              <div class="alert-error">
                <mat-icon>error_outline</mat-icon> {{ error() }}
              </div>
            }

            <button mat-flat-button color="primary" class="submit-btn"
                    (click)="submit()" [disabled]="loading()">
              {{ 'register.submit' | translate }}
            </button>
          </div>

          <div class="footer-link">
            {{ 'register.haveAccount' | translate }}
            <a routerLink="/login">{{ 'register.loginLink' | translate }}</a>
          </div>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .auth-shell {
      min-height: 100vh;
      display: grid; place-items: center;
      background: var(--nt-hero-bg);
      padding: 24px;
      position: relative;
    }
    .auth-topbar {
      position: absolute; top: 16px; right: 16px;
      display: flex; gap: 8px;
    }
    .auth-panel { width: 100%; max-width: 480px; }

    .brand-hero {
      text-align: center; margin-bottom: 28px;
      h1 { font-size: 1.75rem; margin: 12px 0 4px; font-weight: 700; letter-spacing: -0.02em; }
      p { margin: 0; color: var(--nt-text-muted); }
    }
    .brand-icon {
      width: 56px; height: 56px;
      background: var(--nt-primary); color: #fff;
      border-radius: 16px;
      display: inline-flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 20px rgba(13, 148, 136, 0.35);
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }

    .auth-card {
      padding: 32px !important;
      box-shadow: var(--nt-shadow-lg) !important;
      border: none !important;
      display: flex; flex-direction: column; gap: 20px;
    }

    .form { display: flex; flex-direction: column; gap: 16px; }
    .form mat-form-field { width: 100%; }

    .type-toggle { display: flex; flex-direction: column; gap: 8px; }
    .type-label {
      font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--nt-text-muted);
    }
    .type-buttons {
      display: flex; gap: 8px;
      button { flex: 1; display: inline-flex; align-items: center; justify-content: center; gap: 6px; }
      button.active {
        background: var(--nt-primary) !important;
        color: #fff !important;
        border-color: var(--nt-primary) !important;
      }
    }

    .consent-box {
      background: var(--nt-surface-2);
      border: 1px solid var(--nt-border);
      border-radius: 12px;
      padding: 16px;
      display: flex; flex-direction: column; gap: 10px;
      .consent-header { display: flex; align-items: center; gap: 8px; color: var(--nt-primary-dark); }
      p { font-size: 0.85rem; margin: 0; line-height: 1.5; color: var(--nt-text-muted); }
    }

    .submit-btn {
      width: 100%;
      height: 46px;
      font-weight: 600;
      margin-top: 4px;
    }

    .footer-link {
      text-align: center; color: var(--nt-text-muted); font-size: 0.9rem;
      a { color: var(--nt-primary-dark); font-weight: 600; text-decoration: none; margin-left: 4px; }
      a:hover { text-decoration: underline; }
    }
  `]
})
export class RegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);
  private translate = inject(TranslateService);

  name = '';
  email = '';
  cpf = '';
  crn = '';
  password = '';
  showPwd = false;
  dataConsent = false;
  role: 'USER' | 'NUTRITIONIST' = 'USER';
  loading = signal(false);
  error = signal<string | null>(null);

  onCpfInput(ev: Event) {
    const raw = (ev.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 11);
    // Formata "123.456.789-01" enquanto digita
    let formatted = raw;
    if (raw.length > 9) formatted = `${raw.slice(0,3)}.${raw.slice(3,6)}.${raw.slice(6,9)}-${raw.slice(9)}`;
    else if (raw.length > 6) formatted = `${raw.slice(0,3)}.${raw.slice(3,6)}.${raw.slice(6)}`;
    else if (raw.length > 3) formatted = `${raw.slice(0,3)}.${raw.slice(3)}`;
    this.cpf = formatted;
  }

  submit() {
    this.error.set(null);
    const cpfDigits = this.cpf.replace(/\D/g, '');
    if (!this.name || !this.email || !this.password || !cpfDigits) {
      this.error.set(this.translate.instant('register.errors.emptyFields')); return;
    }
    if (cpfDigits.length !== 11) {
      this.error.set(this.translate.instant('register.errors.invalidCpf')); return;
    }
    if (this.password.length < 6) {
      this.error.set(this.translate.instant('register.errors.shortPassword')); return;
    }
    if (!this.dataConsent) {
      this.error.set(this.translate.instant('register.errors.noConsent')); return;
    }
    this.loading.set(true);
    this.auth.register({
      name: this.name, email: this.email,
      cpf: cpfDigits,
      crn: this.role === 'NUTRITIONIST' && this.crn?.trim() ? this.crn.trim() : undefined,
      password: this.password,
      dataConsent: this.dataConsent, role: this.role
    }).subscribe({
      next: r => {
        this.loading.set(false);
        this.snack.open(this.translate.instant('register.created'), 'OK', { duration: 2000 });
        if (r.role === 'NUTRITIONIST') this.router.navigate(['/nutritionist/clients']);
        else this.router.navigate(['/profile-setup']);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.error ?? this.translate.instant('register.errors.createFail'));
      }
    });
  }
}
