import { Injectable, computed, effect, signal } from '@angular/core';

export type ThemeMode = 'auto' | 'light' | 'dark';
const KEY = 'nutritracker.theme';

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
    effect(() => this.apply(this.effectiveDark()));
  }

  setMode(m: ThemeMode) {
    localStorage.setItem(KEY, m);
    this.mode.set(m);
  }

  private apply(dark: boolean) {
    const root = document.documentElement;
    if (dark) root.setAttribute('data-theme', 'dark');
    else root.removeAttribute('data-theme');
  }
}
