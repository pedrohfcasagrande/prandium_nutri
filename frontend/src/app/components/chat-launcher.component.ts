import { Component, OnDestroy, OnInit, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { TranslateModule } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { ApiService } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { PracticeService } from '../services/practice.service';
import { ConversationSummary } from '../models/models';

/**
 * Fase 9G-2 refino: chat launcher flutuante (FAB) tipo Messenger.
 * <ul>
 *   <li>FAB bottom-right com badge do total de nao lidas.</li>
 *   <li>Popover ao clicar mostra ate 10 conversas ordenadas por unread + lastMessage.</li>
 *   <li>Nutri: lista filtrada pela practice ativa (respeita PracticeService).</li>
 *   <li>Some em telas de auth e no proprio /chat (redundante).</li>
 *   <li>Polling 8s enquanto o FAB esta visivel — leve o suficiente.</li>
 * </ul>
 */
@Component({
  selector: 'app-chat-launcher',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatButtonModule, MatIconModule, MatMenuModule, MatBadgeModule, TranslateModule
  ],
  template: `
    @if (visible()) {
      <div class="fab-container">
        <button mat-fab color="primary"
                [matMenuTriggerFor]="menu"
                (menuOpened)="onOpen()"
                [attr.aria-label]="'chat.launcher.title' | translate">
          <mat-icon [matBadge]="totalUnread()"
                    [matBadgeHidden]="totalUnread() === 0"
                    matBadgeColor="warn"
                    matBadgeSize="small">
            chat
          </mat-icon>
        </button>

        <mat-menu #menu="matMenu" xPosition="before" yPosition="above" class="chat-menu">
          <div class="menu-header" (click)="$event.stopPropagation()">
            <mat-icon>forum</mat-icon>
            <strong>{{ 'chat.launcher.title' | translate }}</strong>
            @if (totalUnread() > 0) {
              <span class="unread-total">{{ totalUnread() }}</span>
            }
          </div>

          @if (loading()) {
            <div class="menu-empty">
              <mat-icon>hourglass_empty</mat-icon>
              <span>{{ 'chat.loading' | translate }}</span>
            </div>
          } @else if (!conversations().length) {
            <div class="menu-empty">
              <mat-icon>chat_bubble_outline</mat-icon>
              <span>{{ 'chat.launcher.empty' | translate }}</span>
            </div>
          } @else {
            @for (c of conversations(); track c.linkId) {
              <a mat-menu-item [routerLink]="['/chat', c.linkId]" class="conv-item"
                 [class.has-unread]="c.unreadCount > 0">
                <div class="avatar" [class.online]="c.counterpartyOnline">
                  <mat-icon>
                    {{ c.counterpartyRole === 'NUTRITIONIST' ? 'medical_services' : 'person' }}
                  </mat-icon>
                  @if (c.counterpartyOnline) { <span class="online-dot"></span> }
                </div>
                <div class="conv-body">
                  <div class="conv-top">
                    <span class="conv-name">{{ c.counterpartyName }}</span>
                    @if (c.unreadCount > 0) {
                      <span class="conv-badge">{{ c.unreadCount }}</span>
                    }
                  </div>
                  <div class="conv-preview">
                    {{ c.lastMessagePreview || ('chat.launcher.noMessages' | translate) }}
                  </div>
                </div>
              </a>
            }
          }
        </mat-menu>
      </div>
    }
  `,
  styles: [`
    .fab-container {
      position: fixed; right: 24px; bottom: 24px; z-index: 1000;
    }
    .fab-container button {
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.22) !important;
    }
    :host ::ng-deep .chat-menu.mat-mdc-menu-panel {
      min-width: 320px !important;
      max-width: 360px !important;
      max-height: 480px;
      overflow: hidden;
    }
    .menu-header {
      display: flex; align-items: center; gap: 8px;
      padding: 12px 16px;
      border-bottom: 1px solid var(--nt-border);
      background: var(--nt-primary-soft);
      color: var(--nt-primary-dark);
      font-size: 0.95rem;
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
      .unread-total {
        margin-left: auto;
        background: #ef4444; color: #fff;
        border-radius: 999px;
        padding: 2px 8px;
        font-size: 0.75rem; font-weight: 700;
      }
    }
    .menu-empty {
      padding: 20px; text-align: center;
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      color: var(--nt-text-muted);
      mat-icon { font-size: 32px; width: 32px; height: 32px; opacity: 0.5; }
    }
    .conv-item {
      display: flex !important; align-items: center; gap: 12px;
      padding: 10px 14px !important;
      height: auto !important;
      &.has-unread { background: var(--nt-surface-2); }
    }
    .avatar {
      position: relative;
      width: 36px; height: 36px; border-radius: 50%;
      background: linear-gradient(135deg, var(--nt-primary), var(--nt-primary-dark));
      color: #fff;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .online-dot {
      position: absolute; bottom: -1px; right: -1px;
      width: 10px; height: 10px; border-radius: 50%;
      background: #22c55e; border: 2px solid var(--nt-surface);
    }
    .conv-body { flex: 1; min-width: 0; }
    .conv-top {
      display: flex; align-items: center; gap: 8px;
    }
    .conv-name { font-weight: 600; font-size: 0.9rem; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .conv-badge {
      background: #ef4444; color: #fff;
      border-radius: 999px; padding: 1px 7px;
      font-size: 0.7rem; font-weight: 700; line-height: 1.4;
    }
    .conv-preview {
      font-size: 0.8rem; color: var(--nt-text-muted);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
      margin-top: 2px;
    }
  `]
})
export class ChatLauncherComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private practices = inject(PracticeService);
  private router = inject(Router);

  conversations = signal<ConversationSummary[]>([]);
  loading = signal<boolean>(false);
  private routeUrl = signal<string>(this.router.url);
  private pollHandle?: number;

  totalUnread = computed(() =>
    this.conversations().reduce((sum, c) => sum + c.unreadCount, 0)
  );

  visible = computed(() => {
    if (!this.auth.isAuthenticated()) return false;
    const url = this.routeUrl();
    // Some em auth flows e no proprio /chat pra evitar redundancia
    if (/^\/(login|register|forgot-password|reset-password|invite|verify|profile-setup|company-onboarding|pricing|billing)/.test(url)) {
      return false;
    }
    if (url.startsWith('/chat/')) return false;
    // Se nao ha conversas ativas, esconde
    return this.conversations().length > 0;
  });

  constructor() {
    // Atualiza a URL corrente quando muda a rota
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.routeUrl.set(this.router.url));

    // Re-carrega quando pratice ativa muda (nutri) ou user muda
    effect(() => {
      this.practices.activePracticeId();
      this.auth.currentUser();
      if (this.auth.isAuthenticated()) this.load();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.load();
      this.startPolling();
    }
  }

  ngOnDestroy(): void {
    if (this.pollHandle) window.clearInterval(this.pollHandle);
  }

  onOpen() {
    this.load();
  }

  private load() {
    this.loading.set(true);
    const practiceId = this.auth.currentUser()?.role === 'NUTRITIONIST'
      ? this.practices.activePracticeId() : null;
    this.api.chatConversations(practiceId).subscribe({
      next: list => {
        this.conversations.set(list.slice(0, 10));
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); this.conversations.set([]); }
    });
  }

  private startPolling() {
    if (this.pollHandle) return;
    this.pollHandle = window.setInterval(() => {
      if (this.auth.isAuthenticated()) this.load();
    }, 8000);
  }
}
