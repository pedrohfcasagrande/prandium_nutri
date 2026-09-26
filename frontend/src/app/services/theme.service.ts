import { Injectable, computed, effect, signal } from '@angular/core';

export type ThemeMode = 'auto' | 'light' | 'dark';
const KEY = 'prandiumnutri.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  private systemPrefersDark = signal<boolean>(this.mediaQuery.matches);

  readonly mode = signal<ThemeMode>((localStorage.getItem(KEY) as ThemeMode) ?? 'auto');
  readonly effectiveDark = computed(() => {
    const m = this.mode();
    if (m === 'auto') return this.systemPrefersDark();
    return m === 'dark';
  });

  constructor() {
    this.mediaQuery.addEventListener('change', e => this.systemPrefersDark.set(e.matches));
    effect(() => this.apply());
  }

  setMode(m: ThemeMode) {
    localStorage.setItem(KEY, m);
    this.mode.set(m);
  }

  // Em 'auto', remove data-theme e deixa o CSS decidir via @media prefers-color-scheme.
  // Em 'light'/'dark', seta explicitamente para sobrepor o @media.
  private apply() {
    const root = document.documentElement;
    const m = this.mode();
    if (m === 'auto') root.removeAttribute('data-theme');
    else root.setAttribute('data-theme', m);
  }
}
