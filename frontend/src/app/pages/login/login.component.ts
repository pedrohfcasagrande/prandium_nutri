import { AfterViewInit, Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { GoogleSigninService } from '../../services/google-signin.service';
import { LanguageSwitcherComponent } from '../../components/language-switcher.component';
import { ThemeSwitcherComponent } from '../../components/theme-switcher.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatDividerModule, MatIconModule, MatSnackBarModule,
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
          <h1>{{ 'app.brand' | translate }}</h1>
          <p>{{ 'app.tagline' | translate }}</p>
        </div>

        <mat-card class="auth-card">
          <div class="card-title-block">
            <h2>{{ 'login.welcome' | translate }}</h2>
            <p class="text-muted">{{ 'login.subtitle' | translate }}</p>
          </div>

          <div class="form">
            <mat-form-field appearance="outline">
              <mat-label>{{ 'login.email' | translate }}</mat-label>
              <input matInput type="email" [(ngModel)]="email" autocomplete="email">
              <mat-icon matPrefix>mail</mat-icon>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ 'login.password' | translate }}</mat-label>
              <input matInput [type]="showPwd ? 'text' : 'password'"
                     [(ngModel)]="password" autocomplete="current-password"
                     (keydown.enter)="login()">
              <mat-icon matPrefix>lock</mat-icon>
              <button mat-icon-button matSuffix type="button" (click)="showPwd = !showPwd" tabindex="-1"
                      [attr.aria-label]="(showPwd ? 'login.hidePassword' : 'login.showPassword') | translate">
                <mat-icon>{{ showPwd ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            @if (error()) {
              <div class="alert-error">
                <mat-icon>error_outline</mat-icon> {{ error() }}
              </div>
            }

            <div class="forgot-row">
              <a routerLink="/forgot-password" class="forgot-link">
                {{ 'login.forgotPassword' | translate }}
              </a>
            </div>

            <button mat-flat-button color="primary" class="submit-btn"
                    (click)="login()" [disabled]="loading()">
              {{ 'login.submit' | translate }}
            </button>
          </div>

          <div class="divider-text">
            <mat-divider></mat-divider>
            <span>{{ 'login.or' | translate }}</span>
            <mat-divider></mat-divider>
          </div>

          <div class="google-slot"><div #googleBtn></div></div>

          <div class="footer-link">
            {{ 'login.noAccount' | translate }}
            <a routerLink="/register">{{ 'login.registerLink' | translate }}</a>
          </div>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .auth-shell {
      min-height: 100vh;
      display: grid;
      place-items: center;
      background: var(--nt-hero-bg);
      padding: 24px;
      position: relative;
    }
    .auth-topbar {
      position: absolute;
      top: 16px;
      right: 16px;
      display: flex;
      gap: 8px;
    }
    .auth-panel { width: 100%; max-width: 440px; }

    .brand-hero {
      text-align: center;
      margin-bottom: 28px;
      h1 { font-size: 2rem; margin: 12px 0 4px; font-weight: 700; letter-spacing: -0.02em; color: var(--nt-text); }
      p { margin: 0; color: var(--nt-text-muted); }
    }
    .brand-icon {
      width: 56px; height: 56px;
      background: var(--nt-primary);
      color: #fff;
      border-radius: 16px;
      display: inline-flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 20px rgba(13, 148, 136, 0.35);
      mat-icon { font-size: 28px; width: 28px; height: 28px; }
    }

    .auth-card {
      padding: 32px !important;
      box-shadow: var(--nt-shadow-lg) !important;
      border: none !important;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .card-title-block {
      text-align: center;
      h2 { margin: 0 0 6px; font-size: 1.4rem; font-weight: 600; color: var(--nt-text); }
      p { margin: 0; font-size: 0.9rem; }
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .form mat-form-field { width: 100%; }

    .submit-btn {
      width: 100%;
      height: 46px;
      font-size: 0.95rem;
      font-weight: 600;
      letter-spacing: 0.01em;
      margin-top: 4px;
    }

    .divider-text {
      display: flex; align-items: center; gap: 14px;
      color: var(--nt-text-muted);
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      mat-divider { flex: 1; }
    }

    .google-slot {
      display: flex;
      justify-content: center;
      padding: 4px 0;
      background: transparent;
    }
    .google-slot > div,
    .google-slot iframe {
      background: transparent !important;
      color-scheme: normal;
    }

    .footer-link {
      text-align: center;
      color: var(--nt-text-muted);
      font-size: 0.9rem;
      a { color: var(--nt-primary-dark); font-weight: 600; text-decoration: none; margin-left: 4px; }
      a:hover { text-decoration: underline; }
    }
    .forgot-row {
      display: flex; justify-content: flex-end;
      margin-top: -8px;
    }
    .forgot-link {
      color: var(--nt-primary-dark);
      font-size: 0.85rem;
      text-decoration: none;
    }
    .forgot-link:hover { text-decoration: underline; }
  `]
})
export class LoginComponent implements AfterViewInit {
  @ViewChild('googleBtn') googleBtn!: ElementRef<HTMLElement>;
  private auth = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);
  private googleSignin = inject(GoogleSigninService);
  private translate = inject(TranslateService);

  email = '';
  password = '';
  showPwd = false;
  loading = signal(false);
  error = signal<string | null>(null);

  ngAfterViewInit(): void {
    this.googleSignin.renderButton(this.googleBtn.nativeElement, idToken => this.googleLogin(idToken));
  }

  login() {
    this.error.set(null);
    if (!this.email || !this.password) {
      this.error.set(this.translate.instant('login.errors.emptyFields')); return;
    }
    this.loading.set(true);
    this.auth.login({ email: this.email, password: this.password }).subscribe({
      next: r => this.afterLogin(r.profileCompleted, r.role),
      error: () => {
        this.loading.set(false);
        this.error.set(this.translate.instant('login.errors.invalid'));
      }
    });
  }

  googleLogin(idToken: string) {
    this.loading.set(true);
    this.auth.loginWithGoogle(idToken, true).subscribe({
      next: r => this.afterLogin(r.profileCompleted, r.role),
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.error ?? this.translate.instant('login.errors.googleFail'));
      }
    });
  }

  private afterLogin(profileCompleted: boolean, role: string) {
    this.loading.set(false);
    if (role === 'ADMIN') this.router.navigate(['/admin/users']);
    else if (role === 'NUTRITIONIST') this.router.navigate(['/nutritionist/clients']);
    else if (role === 'COMPANY') this.router.navigate(['/company/dashboard']);
    else this.router.navigate([profileCompleted ? '/dashboard' : '/profile-setup']);
  }
}
