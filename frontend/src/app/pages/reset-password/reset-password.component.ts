import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';
import { ThemeSwitcherComponent } from '../../components/theme-switcher.component';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, TranslateModule, LanguageSwitcherComponent,
    ThemeSwitcherComponent
  ],
  template: `
    <div class="auth-shell">
      <div class="auth-topbar">
        <app-language-switcher></app-language-switcher>
        <app-theme-switcher></app-theme-switcher>
      </div>
      <div class="auth-panel">
        <div class="brand-hero">
          <div class="brand-icon"><mat-icon>password</mat-icon></div>
          <h1>{{ 'resetPassword.title' | translate }}</h1>
          <p>{{ 'resetPassword.subtitle' | translate }}</p>
        </div>

        <mat-card class="auth-card">
          @if (!token()) {
            <div class="alert-error">
              <mat-icon>error_outline</mat-icon>
              {{ 'resetPassword.errors.missingToken' | translate }}
            </div>
          } @else if (!done()) {
            <div class="form">
              <mat-form-field appearance="outline">
                <mat-label>{{ 'resetPassword.newPassword' | translate }}</mat-label>
                <input matInput [type]="show ? 'text' : 'password'"
                       [(ngModel)]="newPassword" autocomplete="new-password">
                <mat-icon matPrefix>lock</mat-icon>
                <button mat-icon-button matSuffix type="button" (click)="show = !show" tabindex="-1"
                        [attr.aria-label]="(show ? 'login.hidePassword' : 'login.showPassword') | translate">
                  <mat-icon>{{ show ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>{{ 'resetPassword.confirmPassword' | translate }}</mat-label>
                <input matInput [type]="show ? 'text' : 'password'"
                       [(ngModel)]="confirmPassword" autocomplete="new-password"
                       (keydown.enter)="submit()">
                <mat-icon matPrefix>lock</mat-icon>
              </mat-form-field>

              @if (error()) {
                <div class="alert-error"><mat-icon>error_outline</mat-icon> {{ error() }}</div>
              }

              <button mat-flat-button color="primary" class="submit-btn"
                      (click)="submit()" [disabled]="loading()">
                {{ 'resetPassword.submit' | translate }}
              </button>
            </div>
          } @else {
            <div class="success">
              <mat-icon>check_circle</mat-icon>
              <p>{{ 'resetPassword.successMessage' | translate }}</p>
              <a mat-flat-button color="primary" routerLink="/login">
                {{ 'resetPassword.goToLogin' | translate }}
              </a>
            </div>
          }

          @if (!done()) {
            <div class="footer-link">
              <a routerLink="/login">
                <mat-icon>arrow_back</mat-icon> {{ 'forgotPassword.backToLogin' | translate }}
              </a>
            </div>
          }
        </mat-card>

        <div class="auth-trust">
          <span><mat-icon>lock</mat-icon> {{ 'auth.trust.ssl' | translate }}</span>
          <span><mat-icon>shield</mat-icon> {{ 'auth.trust.lgpd' | translate }}</span>
          <span><mat-icon>verified</mat-icon> {{ 'auth.trust.encrypted' | translate }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .form { display: flex; flex-direction: column; gap: 16px; }
    .submit-btn { width: 100%; height: 46px; font-weight: 600; }

    .success {
      display: flex; flex-direction: column; align-items: center; gap: 12px;
      text-align: center; padding: 16px 0;
      color: var(--nt-primary-dark);
      mat-icon { font-size: 56px; width: 56px; height: 56px; color: #16a34a; }
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
export class ResetPasswordComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private translate = inject(TranslateService);

  token = signal<string | null>(null);
  newPassword = '';
  confirmPassword = '';
  show = false;
  loading = signal(false);
  done = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('token');
    this.token.set(t);
  }

  submit() {
    this.error.set(null);
    if (!this.newPassword || this.newPassword.length < 6) {
      this.error.set(this.translate.instant('resetPassword.errors.shortPassword')); return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.error.set(this.translate.instant('resetPassword.errors.mismatch')); return;
    }
    this.loading.set(true);
    this.http.post(`${environment.apiUrl}/auth/reset-password`, {
      token: this.token(),
      newPassword: this.newPassword
    }).subscribe({
      next: () => { this.loading.set(false); this.done.set(true); },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.error ?? this.translate.instant('resetPassword.errors.invalid'));
      }
    });
  }
}
