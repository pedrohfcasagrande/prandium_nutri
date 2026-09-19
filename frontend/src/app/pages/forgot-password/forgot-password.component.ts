import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, TranslateModule, LanguageSwitcherComponent
  ],
  template: `
    <div class="auth-shell">
      <div class="auth-topbar"><app-language-switcher></app-language-switcher></div>
      <div class="auth-panel">
        <div class="brand-hero">
          <div class="brand-icon"><mat-icon>lock_reset</mat-icon></div>
          <h1>{{ 'forgotPassword.title' | translate }}</h1>
          <p>{{ 'forgotPassword.subtitle' | translate }}</p>
        </div>

        <mat-card class="auth-card">
          @if (!sent()) {
            <div class="form">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'login.email' | translate }}</mat-label>
                <input matInput type="email" [(ngModel)]="email" autocomplete="email"
                       (keydown.enter)="submit()">
                <mat-icon matPrefix>mail</mat-icon>
              </mat-form-field>

              <button mat-flat-button color="primary" class="submit-btn"
                      (click)="submit()" [disabled]="loading()">
                {{ 'forgotPassword.submit' | translate }}
              </button>
            </div>
          } @else {
            <div class="success">
              <mat-icon>mark_email_read</mat-icon>
              <p>{{ 'forgotPassword.sentMessage' | translate }}</p>
            </div>
          }

          <div class="footer-link">
            <a routerLink="/login">
              <mat-icon>arrow_back</mat-icon> {{ 'forgotPassword.backToLogin' | translate }}
            </a>
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
      padding: 24px; position: relative;
    }
    .auth-topbar { position: absolute; top: 16px; right: 16px; }
    .auth-panel { width: 100%; max-width: 440px; }

    .brand-hero { text-align: center; margin-bottom: 28px; }
    .brand-hero h1 { font-size: 1.75rem; margin: 12px 0 4px; font-weight: 700; letter-spacing: -0.02em; }
    .brand-hero p { margin: 0; color: var(--nt-text-muted); }
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
    .submit-btn { width: 100%; height: 46px; font-weight: 600; }

    .success {
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      color: var(--nt-primary-dark);
      text-align: center;
      padding: 16px 0;
      mat-icon { font-size: 48px; width: 48px; height: 48px; }
      p { margin: 0; color: var(--nt-text); }
    }

    .footer-link {
      text-align: center;
      a {
        color: var(--nt-primary-dark); font-weight: 500;
        text-decoration: none;
        display: inline-flex; align-items: center; gap: 4px;
        mat-icon { font-size: 18px; width: 18px; height: 18px; }
      }
      a:hover { text-decoration: underline; }
    }
  `]
})
export class ForgotPasswordComponent {
  private http = inject(HttpClient);
  email = '';
  loading = signal(false);
  sent = signal(false);

  submit() {
    if (!this.email) return;
    this.loading.set(true);
    this.http.post(`${environment.apiUrl}/auth/forgot-password`, { email: this.email })
      .subscribe({
        next: () => { this.loading.set(false); this.sent.set(true); },
        error: () => { this.loading.set(false); this.sent.set(true); } // sempre confirma para nao vazar
      });
  }
}
