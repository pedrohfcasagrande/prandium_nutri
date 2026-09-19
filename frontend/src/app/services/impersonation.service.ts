import { Injectable, signal } from '@angular/core';

export interface ImpersonationTarget {
  id: number;
  name: string;
  email: string;
}

const KEY = 'nutritracker.impersonate';

@Injectable({ providedIn: 'root' })
export class ImpersonationService {
  readonly target = signal<ImpersonationTarget | null>(this.load());

  start(user: ImpersonationTarget) {
    sessionStorage.setItem(KEY, JSON.stringify(user));
    this.target.set(user);
  }

  stop() {
    sessionStorage.removeItem(KEY);
    this.target.set(null);
  }

  header(): string | null {
    const t = this.target();
    return t ? String(t.id) : null;
  }

  private load(): ImpersonationTarget | null {
    const raw = sessionStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
