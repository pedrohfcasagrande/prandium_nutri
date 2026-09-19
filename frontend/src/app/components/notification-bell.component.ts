import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { NotificationPollService } from '../services/notification-poll.service';
import { AuthService } from '../services/auth.service';
import { AppNotification } from '../models/models';
import { notificationTarget } from '../services/notification-target';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule, MatButtonModule, MatMenuModule, MatBadgeModule, MatDividerModule,
    TranslateModule, RouterLink
  ],
  template: `
    <button mat-icon-button [matMenuTriggerFor]="menu"
            [matBadge]="badge()" matBadgeColor="warn" [matBadgeHidden]="!polls.hasUnread()"
            [attr.aria-label]="'notifications.aria' | translate">
      <mat-icon>notifications</mat-icon>
    </button>
    <mat-menu #menu="matMenu" class="notif-menu" xPosition="before">
      <div class="notif-header">
        <strong>{{ 'notifications.title' | translate }}</strong>
        @if (polls.hasUnread()) {
          <button mat-button color="primary" (click)="markAll($event)">
            {{ 'notifications.markAll' | translate }}
          </button>
        }
      </div>
      <mat-divider></mat-divider>
      @if (polls.recent().length === 0) {
        <div class="notif-empty">
          <mat-icon>notifications_none</mat-icon>
          <span>{{ 'notifications.empty' | translate }}</span>
        </div>
      } @else {
        @for (n of polls.recent(); track n.id) {
          <button mat-menu-item class="notif-item"
                  [class.unread]="!n.readAt"
                  (click)="open(n)">
            <mat-icon [class]="iconClass(n)">{{ iconFor(n) }}</mat-icon>
            <div class="notif-body">
              <div class="notif-title">{{ n.title }}</div>
              @if (n.body) { <div class="notif-msg">{{ n.body }}</div> }
              <div class="notif-time text-muted">{{ n.createdAt | date:'short' }}</div>
            </div>
          </button>
        }
        <mat-divider></mat-divider>
        <a mat-menu-item routerLink="/notifications">
          <mat-icon>chevron_right</mat-icon>
          <span>{{ 'notifications.viewAll' | translate }}</span>
        </a>
      }
    </mat-menu>
  `,
  styles: [`
    ::ng-deep .notif-menu { min-width: 340px !important; max-width: 400px !important; }
    .notif-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; }
    .notif-empty {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      padding: 24px; color: var(--nt-text-muted);
      mat-icon { font-size: 32px; width: 32px; height: 32px; opacity: 0.4; }
    }
    .notif-item {
      height: auto !important; line-height: normal !important; white-space: normal !important;
      padding: 10px 16px !important; align-items: flex-start !important;
      display: flex; gap: 12px;
    }
    .notif-item.unread { background: rgba(13, 148, 136, 0.06); }
    .notif-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
    .notif-title { font-weight: 600; font-size: 0.9rem; }
    .notif-msg { font-size: 0.82rem; color: var(--nt-text); white-space: normal; overflow: hidden; text-overflow: ellipsis; }
    .notif-time { font-size: 0.7rem; }
    .icon-approved { color: #10b981; }
    .icon-rejected { color: #ef4444; }
    .icon-submitted { color: #3b82f6; }
    .icon-generic { color: var(--nt-text-muted); }
  `]
})
export class NotificationBellComponent {
  polls = inject(NotificationPollService);
  private router = inject(Router);
  private auth = inject(AuthService);

  badge(): string { return this.polls.unreadCount() > 9 ? '9+' : String(this.polls.unreadCount()); }

  markAll(ev: Event) {
    ev.stopPropagation();
    this.polls.markAllRead();
  }

  open(n: AppNotification) {
    if (!n.readAt) this.polls.markRead(n.id);
    const target = notificationTarget(n, this.auth.currentUser()?.role);
    if (target) this.router.navigate([target]);
  }

  iconFor(n: AppNotification): string {
    switch (n.type) {
      case 'MENU_APPROVED': return 'check_circle';
      case 'MENU_REJECTED': return 'cancel';
      case 'MENU_SUBMITTED': return 'assignment';
    }
    switch (n.entityType) {
      case 'CompanyNutritionist': return 'corporate_fare';
      case 'NutritionistClientLink': return 'person_add';
      default: return 'notifications';
    }
  }

  iconClass(n: AppNotification): string {
    switch (n.type) {
      case 'MENU_APPROVED': return 'icon-approved';
      case 'MENU_REJECTED': return 'icon-rejected';
      case 'MENU_SUBMITTED': return 'icon-submitted';
    }
    switch (n.entityType) {
      case 'CompanyNutritionist': return 'icon-submitted';
      case 'NutritionistClientLink': return 'icon-approved';
      default: return 'icon-generic';
    }
  }
}
