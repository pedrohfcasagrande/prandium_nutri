import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type LanguageMode = 'auto' | 'pt' | 'en' | 'fr';
export type Lang = 'pt' | 'en' | 'fr';
const KEY = 'prandiumnutri.lang';
const SUPPORTED: Lang[] = ['pt', 'en', 'fr'];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private translate = inject(TranslateService);

  readonly mode = signal<LanguageMode>((localStorage.getItem(KEY) as LanguageMode) ?? 'auto');
  readonly effective = computed<Lang>(() => {
    const m = this.mode();
    return m === 'auto' ? this.detectBrowser() : m;
  });

  constructor() {
    this.translate.addLangs(SUPPORTED);
    this.translate.setDefaultLang('pt');
    effect(() => {
      const lang = this.effective();
      this.translate.use(lang);
      document.documentElement.setAttribute('lang', lang);
    });
  }

  setMode(m: LanguageMode) {
    const before = this.effective();
    localStorage.setItem(KEY, m);
    this.mode.set(m);
    // LOCALE_ID/MAT_DATE_LOCALE sao lidos no bootstrap. Se o idioma efetivo mudou,
    // recarrega pra o pipe `date` e datepicker respeitarem o novo formato.
    if (this.effective() !== before) window.location.reload();
  }

  private detectBrowser(): Lang {
    const nav = (navigator.language || 'pt').toLowerCase().split('-')[0];
    return SUPPORTED.includes(nav as Lang) ? (nav as Lang) : 'pt';
  }
}
