import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { TranslateModule } from '@ngx-translate/core';
import { map, shareReplay } from 'rxjs';
import { AuthService } from './services/auth.service';
import { ThemeService, ThemeMode } from './services/theme.service';
import { LanguageService, LanguageMode } from './services/language.service';
import { ImpersonationService } from './services/impersonation.service';
import { NotificationPollService } from './services/notification-poll.service';
import { NotificationBellComponent } from './components/notification-bell.component';
import { PracticeSwitcherComponent } from './components/practice-switcher.component';
import { PracticeService } from './services/practice.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { effect } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatSidenavModule, MatListModule, MatMenuModule, MatDividerModule, MatTooltipModule,
    TranslateModule, NotificationBellComponent, PracticeSwitcherComponent
  ],
  template: `
    @if (auth.isAuthenticated() && !isAuthRoute()) {
      <mat-sidenav-container class="app-container" autosize>
        <mat-sidenav #drawer class="sidenav"
                     [mode]="isHandset() ? 'over' : 'side'"
                     [opened]="!isHandset()"
                     [fixedInViewport]="isHandset()">
          <div class="sidenav-header">
            <div class="brand">
              <span class="brand-icon"><mat-icon>eco</mat-icon></span>
              <span>{{ 'app.brand' | translate }}</span>
            </div>
          </div>

          <mat-nav-list class="sidenav-list">
            @if (showUserNav()) {
              <a mat-list-item routerLink="/dashboard" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>space_dashboard</mat-icon>
                <span matListItemTitle>{{ 'app.nav.dashboard' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/meals" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>restaurant_menu</mat-icon>
                <span matListItemTitle>{{ 'app.nav.meals' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/suggestions" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>auto_awesome</mat-icon>
                <span matListItemTitle>{{ 'app.nav.suggestions' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/menus" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>menu_book</mat-icon>
                <span matListItemTitle>{{ 'app.nav.menus' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/progress" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>monitor_weight</mat-icon>
                <span matListItemTitle>{{ 'app.nav.progress' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/profile" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>person</mat-icon>
                <span matListItemTitle>{{ 'app.nav.profile' | translate }}</span>
              </a>
              @if (isEffectiveUser()) {
                <a mat-list-item routerLink="/invitations" routerLinkActive="active" (click)="onNavClick(drawer)">
                  <mat-icon matListItemIcon>mail</mat-icon>
                  <span matListItemTitle>{{ 'app.nav.invitations' | translate }}</span>
                </a>
                <a mat-list-item routerLink="/find-nutritionist" routerLinkActive="active" (click)="onNavClick(drawer)">
                  <mat-icon matListItemIcon>search</mat-icon>
                  <span matListItemTitle>{{ 'app.nav.findNutri' | translate }}</span>
                </a>
              }
            }
            @if (auth.isNutritionist()) {
              @if (showUserNav()) { <mat-divider></mat-divider> }
              <a mat-list-item routerLink="/nutritionist/dashboard" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>dashboard</mat-icon>
                <span matListItemTitle>{{ 'app.nav.nutriDashboard' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/nutritionist/clients" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>groups</mat-icon>
                <span matListItemTitle>{{ 'app.nav.nutriClients' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/nutritionist/metrics" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>insights</mat-icon>
                <span matListItemTitle>{{ 'app.nav.nutriMetrics' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/nutritionist/pending-menus" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>assignment</mat-icon>
                <span matListItemTitle>{{ 'app.nav.pendingMenus' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/nutritionist/public-profile" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>badge</mat-icon>
                <span matListItemTitle>{{ 'app.nav.nutriPublicProfile' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/nutritionist/company-invitations" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>corporate_fare</mat-icon>
                <span matListItemTitle>{{ 'app.nav.nutriCompanyInvites' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/nutritionist/practices" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>manage_accounts</mat-icon>
                <span matListItemTitle>{{ 'app.nav.nutriPractices' | translate }}</span>
              </a>
            }
            @if (auth.isCompany()) {
              @if (showUserNav()) { <mat-divider></mat-divider> }
              <a mat-list-item routerLink="/company/dashboard" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>corporate_fare</mat-icon>
                <span matListItemTitle>{{ 'app.nav.companyDashboard' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/company/metrics" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>insights</mat-icon>
                <span matListItemTitle>{{ 'app.nav.companyMetrics' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/company/nutritionists" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>medical_services</mat-icon>
                <span matListItemTitle>{{ 'app.nav.companyNutris' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/company/clients" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>groups</mat-icon>
                <span matListItemTitle>{{ 'app.nav.companyClients' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/company/members" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>group</mat-icon>
                <span matListItemTitle>{{ 'app.nav.companyMembers' | translate }}</span>
              </a>
            }
            @if (auth.isAdmin()) {
              @if (showUserNav()) { <mat-divider></mat-divider> }
              <a mat-list-item routerLink="/admin/metrics" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>insights</mat-icon>
                <span matListItemTitle>{{ 'app.nav.adminMetrics' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/pricing" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>workspace_premium</mat-icon>
                <span matListItemTitle>{{ 'app.nav.pricing' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/admin/users" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
                <span matListItemTitle>{{ 'app.nav.admin' | translate }}</span>
              </a>
              <a mat-list-item routerLink="/admin/companies" routerLinkActive="active" (click)="onNavClick(drawer)">
                <mat-icon matListItemIcon>corporate_fare</mat-icon>
                <span matListItemTitle>{{ 'app.nav.adminCompanies' | translate }}</span>
              </a>
            }
          </mat-nav-list>

          <div class="sidenav-footer">
            <button mat-stroked-button class="logout-btn" (click)="logout()">
              <mat-icon>logout</mat-icon> {{ 'app.user.logout' | translate }}
            </button>
          </div>
        </mat-sidenav>

        <mat-sidenav-content class="app-content">
          @if (imp.target()) {
            <div class="imp-banner">
              <mat-icon>manage_accounts</mat-icon>
              <span>
                {{ 'impersonation.banner' | translate }}
                <strong>{{ imp.target()!.name }}</strong>
                <span class="imp-email">({{ imp.target()!.email }})</span>
              </span>
              <span class="spacer"></span>
              <button mat-flat-button color="warn" (click)="stopImpersonation()">
                <mat-icon>logout</mat-icon> {{ 'impersonation.exit' | translate }}
              </button>
            </div>
          }
          <mat-toolbar class="app-toolbar">
            <button mat-icon-button (click)="drawer.toggle()" aria-label="Menu">
              <mat-icon>menu</mat-icon>
            </button>
            <span class="toolbar-title">{{ pageTitle() | translate }}</span>
            <span class="spacer"></span>

            <app-practice-switcher></app-practice-switcher>
            <app-notification-bell></app-notification-bell>

            <button mat-icon-button [matMenuTriggerFor]="langMenu"
                    [matTooltip]="'app.language.tooltip' | translate">
              <mat-icon>language</mat-icon>
            </button>
            <mat-menu #langMenu="matMenu">
              <button mat-menu-item (click)="lang.setMode('auto')">
                <mat-icon>{{ lang.mode() === 'auto' ? 'check' : 'translate' }}</mat-icon>
                <span>{{ 'app.theme.auto' | translate }}</span>
              </button>
              <mat-divider></mat-divider>
              <button mat-menu-item (click)="lang.setMode('pt')">
                <mat-icon>{{ lang.mode() === 'pt' ? 'check' : 'flag' }}</mat-icon>
                <span>{{ 'app.language.pt' | translate }}</span>
              </button>
              <button mat-menu-item (click)="lang.setMode('en')">
                <mat-icon>{{ lang.mode() === 'en' ? 'check' : 'flag' }}</mat-icon>
                <span>{{ 'app.language.en' | translate }}</span>
              </button>
              <button mat-menu-item (click)="lang.setMode('fr')">
                <mat-icon>{{ lang.mode() === 'fr' ? 'check' : 'flag' }}</mat-icon>
                <span>{{ 'app.language.fr' | translate }}</span>
              </button>
            </mat-menu>

            <button mat-icon-button [matMenuTriggerFor]="themeMenu"
                    [matTooltip]="'app.theme.tooltip' | translate:{ value: themeLabel() | translate }">
              <mat-icon>{{ theme.effectiveDark() ? 'dark_mode' : 'light_mode' }}</mat-icon>
            </button>
            <mat-menu #themeMenu="matMenu">
              <button mat-menu-item (click)="theme.setMode('auto')">
                <mat-icon>{{ theme.mode() === 'auto' ? 'check' : 'brightness_auto' }}</mat-icon>
                <span>{{ 'app.theme.auto' | translate }}</span>
              </button>
              <button mat-menu-item (click)="theme.setMode('light')">
                <mat-icon>{{ theme.mode() === 'light' ? 'check' : 'light_mode' }}</mat-icon>
                <span>{{ 'app.theme.light' | translate }}</span>
              </button>
              <button mat-menu-item (click)="theme.setMode('dark')">
                <mat-icon>{{ theme.mode() === 'dark' ? 'check' : 'dark_mode' }}</mat-icon>
                <span>{{ 'app.theme.dark' | translate }}</span>
              </button>
            </mat-menu>

            <button mat-button [matMenuTriggerFor]="userMenu" class="user-btn">
              <mat-icon>account_circle</mat-icon>
              <span class="user-name">{{ auth.currentUser()?.name }}</span>
            </button>
            <mat-menu #userMenu="matMenu">
              <button mat-menu-item routerLink="/profile">
                <mat-icon>settings</mat-icon> {{ 'app.user.settings' | translate }}
              </button>
              <button mat-menu-item routerLink="/billing">
                <mat-icon>credit_card</mat-icon> {{ 'app.user.billing' | translate }}
              </button>
              <mat-divider></mat-divider>
              <button mat-menu-item (click)="logout()">
                <mat-icon>logout</mat-icon> {{ 'app.user.logout' | translate }}
              </button>
            </mat-menu>
          </mat-toolbar>

          <div class="page-outlet">
            <router-outlet />
          </div>
        </mat-sidenav-content>
      </mat-sidenav-container>
    } @else {
      <router-outlet />
    }
  `,
  styles: [`
    .app-container { height: 100vh; background: var(--nt-bg); }

    .sidenav {
      width: 260px;
      border-right: 1px solid var(--nt-border);
      background: var(--nt-surface);
      display: flex;
      flex-direction: column;
    }

    .sidenav-header {
      padding: 20px;
      border-bottom: 1px solid var(--nt-border);
      .brand { color: var(--nt-primary-dark); font-size: 1.1rem; }
      .brand-icon {
        background: var(--nt-primary);
        color: #fff;
        border-radius: 10px;
        padding: 6px;
        display: flex;
      }
    }

    .sidenav-list { padding: 12px 8px !important; flex: 1; }
    .sidenav-list a { border-radius: 10px !important; margin-bottom: 4px; }

    .sidenav-footer { padding: 16px; border-top: 1px solid var(--nt-border); }
    .logout-btn { width: 100%; }

    .app-content { display: flex; flex-direction: column; }

    .app-toolbar {
      position: sticky; top: 0; z-index: 10;
      box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    }
    .toolbar-title { font-weight: 600; font-size: 1.05rem; margin-left: 8px; }
    .user-btn { display: flex; align-items: center; gap: 6px; }
    .user-name {
      max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
      @media (max-width: 600px) { display: none; }
    }
    .page-outlet { flex: 1; overflow: auto; }

    .imp-banner {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 20px;
      background: linear-gradient(90deg, #fbbf24, #f59e0b);
      color: #422006;
      font-weight: 500;
      box-shadow: 0 2px 6px rgba(0,0,0,0.1);
      mat-icon { color: #78350f; }
      .imp-email { opacity: 0.7; font-weight: 400; margin-left: 6px; }
      .spacer { flex: 1; }
      button {
        background: #422006 !important;
        color: #fff !important;
      }
    }
    @media (max-width: 600px) {
      .imp-banner { flex-wrap: wrap; font-size: 0.9rem; }
      .imp-banner .imp-email { display: none; }
    }
  `]
})
export class AppComponent {
  auth = inject(AuthService);
  theme = inject(ThemeService);
  lang = inject(LanguageService);
  imp = inject(ImpersonationService);
  private breakpointObserver = inject(BreakpointObserver);
  private router = inject(Router);
  private notif = inject(NotificationPollService);
  private practices = inject(PracticeService);

  constructor() {
    // liga o polling de notificacoes assim que o user esta autenticado
    effect(() => {
      if (this.auth.isAuthenticated()) this.notif.start();
      else this.notif.stop();
    });
    // carrega practices quando o efetivo e nutri
    effect(() => {
      const user = this.auth.currentUser();
      if (user?.role === 'NUTRITIONIST') this.practices.loadForCurrentNutri();
      else this.practices.clear();
    });
  }

  isHandset = toSignal(
    this.breakpointObserver.observe(Breakpoints.Handset).pipe(map(r => r.matches), shareReplay()),
    { initialValue: false }
  );

  isAuthRoute = toSignal(
    this.router.events.pipe(
      map(() => this.router.url.startsWith('/login')
                 || this.router.url.startsWith('/register')
                 || this.router.url.startsWith('/forgot-password')
                 || this.router.url.startsWith('/reset-password')
                 || this.router.url.startsWith('/company-onboarding')
                 || this.router.url.startsWith('/profile-setup'))
    ),
    { initialValue: false }
  );

  pageTitle = toSignal(
    this.router.events.pipe(map(() => {
      const url = this.router.url;
      if (url.startsWith('/dashboard')) return 'app.pageTitle.dashboard';
      if (url.startsWith('/meals')) return 'app.pageTitle.meals';
      if (url.startsWith('/suggestions')) return 'app.pageTitle.suggestions';
      if (url.startsWith('/menus')) return 'app.pageTitle.menus';
      if (url.startsWith('/progress')) return 'app.pageTitle.progress';
      if (url.startsWith('/profile')) return 'app.pageTitle.profile';
      if (url.startsWith('/pricing')) return 'app.pageTitle.pricing';
      if (url.startsWith('/billing')) return 'app.pageTitle.billing';
      if (url.startsWith('/admin/metrics')) return 'app.pageTitle.adminMetrics';
      if (url.startsWith('/admin/users')) return 'app.pageTitle.admin';
      if (url.startsWith('/nutritionist/clients')) return 'app.pageTitle.nutriClients';
      if (url.startsWith('/nutritionist/metrics')) return 'app.pageTitle.nutriMetrics';
      if (url.startsWith('/nutritionist/pending-menus')) return 'app.pageTitle.pendingMenus';
      if (url.startsWith('/nutritionist/public-profile')) return 'app.pageTitle.nutriPublicProfile';
      if (url.startsWith('/nutritionist/company-invitations')) return 'app.pageTitle.nutriCompanyInvites';
      if (url.startsWith('/nutritionist/practices')) return 'app.pageTitle.nutriPractices';
      if (url.startsWith('/nutritionist/dashboard')) return 'app.pageTitle.nutriDashboard';
      if (url.startsWith('/notifications')) return 'app.pageTitle.notifications';
      if (url.startsWith('/find-nutritionist')) return 'app.pageTitle.findNutri';
      if (url.startsWith('/company/dashboard')) return 'app.pageTitle.companyDashboard';
      if (url.startsWith('/company/metrics')) return 'app.pageTitle.companyMetrics';
      if (url.startsWith('/company/nutritionists')) return 'app.pageTitle.companyNutris';
      if (url.startsWith('/company/members')) return 'app.pageTitle.companyMembers';
      if (url.startsWith('/company/clients')) return 'app.pageTitle.companyClients';
      if (url.startsWith('/admin/companies')) return 'app.pageTitle.adminCompanies';
      if (url.startsWith('/invitations')) return 'app.pageTitle.invitations';
      return 'app.brand';
    })),
    { initialValue: 'app.brand' }
  );

  themeLabel(): string {
    const m: ThemeMode = this.theme.mode();
    return m === 'auto' ? 'app.theme.auto' : m === 'dark' ? 'app.theme.dark' : 'app.theme.light';
  }

  showUserNav(): boolean {
    // Usuario comum com perfil, ou admin/nutricionista em modo impersonation
    if (this.auth.isAdmin() || this.auth.isNutritionist() || this.auth.isCompany()) return !!this.imp.target();
    return !!this.auth.currentUser()?.profileCompleted;
  }

  isEffectiveUser(): boolean {
    // Contexto USER: cliente comum logado direto, OU admin impersonando alguem
    if (this.auth.currentUser()?.role === 'USER') return true;
    return this.auth.isAdmin() && !!this.imp.target();
  }

  onNavClick(drawer: any) { if (this.isHandset()) drawer.close(); }

  logout() {
    this.imp.stop();
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  stopImpersonation() {
    this.imp.stop();
    const role = this.auth.currentUser()?.role;
    const target =
      role === 'NUTRITIONIST' ? '/nutritionist/dashboard' :
      role === 'COMPANY'      ? '/company/dashboard' :
      role === 'ADMIN'        ? '/admin/users' :
                                '/dashboard';
    this.router.navigate([target]);
  }
}
