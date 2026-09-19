import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { TranslateModule } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { NotificationPollService } from '../../services/notification-poll.service';
import { AuthService } from '../../services/auth.service';
import { AppNotification } from '../../models/models';
import { notificationTarget } from '../../services/notification-target';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatIconModule, MatButtonModule, MatChipsModule, TranslateModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'notifications.pageTitle' | translate }}</h1>
          <p>{{ 'notifications.pageSubtitle' | translate }}</p>
        </div>
        @if (polls.hasUnread()) {
          <button mat-flat-button color="primary" (click)="polls.markAllRead()">
            <mat-icon>done_all</mat-icon>
            {{ 'notifications.markAll' | translate }}
          </button>
        }
      </div>

      @if (items().length === 0) {
        <div class="empty card-surface">
          <mat-icon>notifications_none</mat-icon>
          <p>{{ 'notifications.empty' | translate }}</p>
        </div>
      } @else {
        <div class="list">
          @for (n of items(); track n.id) {
            <mat-card class="item" [class.unread]="!n.readAt" (click)="open(n)">
              <mat-icon [class]="iconClass(n)">{{ iconFor(n) }}</mat-icon>
              <div class="body">
                <div class="row">
                  <strong>{{ n.title }}</strong>
                  @if (!n.readAt) { <mat-chip class="unread-chip">{{ 'notifications.new' | translate }}</mat-chip> }
                </div>
                @if (n.body) { <div class="msg">{{ n.body }}</div> }
                <div class="time text-muted">{{ n.createdAt | date:'medium' }}</div>
              </div>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .list { display: flex; flex-direction: column; gap: 10px; }
    .item {
      display: flex; align-items: flex-start; gap: 14px; padding: 16px 20px !important;
      cursor: pointer; transition: transform 0.15s ease;
      &:hover { transform: translateX(2px); }
      mat-icon { font-size: 28px; width: 28px; height: 28px; flex-shrink: 0; }
    }
    .item.unread { border-left: 4px solid var(--nt-primary); }
    .body { flex: 1; display: flex; flex-direction: column; gap: 4px; }
    .row { display: flex; align-items: center; gap: 10px; }
    .msg { color: var(--nt-text); }
    .time { font-size: 0.8rem; }
    .unread-chip {
      background: var(--nt-primary) !important; color: #fff !important;
      font-size: 0.7rem !important; height: 20px !important;
    }
    .icon-approved { color: #10b981; }
    .icon-rejected { color: #ef4444; }
    .icon-submitted { color: #3b82f6; }
    .icon-generic { color: var(--nt-text-muted); }
  `]
})
export class NotificationsComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private auth = inject(AuthService);
  polls = inject(NotificationPollService);

  items = signal<AppNotification[]>([]);

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.listNotifications(false, 100).subscribe(list => this.items.set(list));
  }

  open(n: AppNotification) {
    if (!n.readAt) {
      this.polls.markRead(n.id);
      this.items.update(list => list.map(x => x.id === n.id ? { ...x, readAt: new Date().toISOString() } : x));
    }
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
