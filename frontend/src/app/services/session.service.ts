import { Injectable, signal } from '@angular/core';

const KEY = 'prandiumnutri.currentUserId';

@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly currentUserId = signal<number | null>(this.load());

  setUser(id: number) {
    localStorage.setItem(KEY, String(id));
    this.currentUserId.set(id);
  }

  clear() {
    localStorage.removeItem(KEY);
    this.currentUserId.set(null);
  }

  private load(): number | null {
    const v = localStorage.getItem(KEY);
    return v ? Number(v) : null;
  }
}
