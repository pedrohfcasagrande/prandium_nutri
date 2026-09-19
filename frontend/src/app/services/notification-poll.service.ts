import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { AppNotification } from '../models/models';

/**
 * Faz polling a cada 30s da contagem de nao-lidas e mantem lista local.
 * Componentes assinam via signal `unreadCount` e `recent`.
 */
@Injectable({ providedIn: 'root' })
export class NotificationPollService {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  unreadCount = signal(0);
  recent = signal<AppNotification[]>([]);
  hasUnread = computed(() => this.unreadCount() > 0);

  private intervalId: number | null = null;

  start() {
    if (this.intervalId != null) return;
    this.tick();
    this.intervalId = window.setInterval(() => this.tick(), 30_000);
  }

  stop() {
    if (this.intervalId != null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  refresh() {
    this.tick();
  }

  markRead(id: number) {
    this.api.markNotificationRead(id).subscribe(() => {
      this.recent.update(list => list.map(n => n.id === id ? { ...n, readAt: new Date().toISOString() } : n));
      this.unreadCount.update(c => Math.max(0, c - 1));
    });
  }

  markAllRead() {
    this.api.markAllNotificationsRead().subscribe(() => {
      const now = new Date().toISOString();
      this.recent.update(list => list.map(n => n.readAt ? n : { ...n, readAt: now }));
      this.unreadCount.set(0);
    });
  }

  private tick() {
    if (!this.auth.isAuthenticated()) return;
    this.api.unreadNotificationCount().subscribe({
      next: r => this.unreadCount.set(r.count),
      error: () => {}
    });
    this.api.listNotifications(false, 10).subscribe({
      next: list => this.recent.set(list),
      error: () => {}
    });
  }
}
