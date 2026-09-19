import { APP_INITIALIZER, ApplicationConfig, LOCALE_ID, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import localeEn from '@angular/common/locales/en';
import localeFr from '@angular/common/locales/fr';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { authInterceptor } from './services/auth.interceptor';
import { LanguageService } from './services/language.service';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { I18nPaginatorIntl } from './services/i18n-paginator';

registerLocaleData(localePt, 'pt-BR');
registerLocaleData(localeEn, 'en-US');
registerLocaleData(localeFr, 'fr-FR');

/** Resolve o LOCALE_ID BCP-47 a partir da preferencia atual do LanguageService. */
function resolveLocale(): string {
  const raw = localStorage.getItem('prandiumnutri.lang') ?? 'auto';
  const nav = (navigator.language || 'pt').toLowerCase().split('-')[0];
  const lang = raw === 'auto' ? nav : raw;
  switch (lang) {
    case 'en': return 'en-US';
    case 'fr': return 'fr-FR';
    default:   return 'pt-BR';
  }
}

export function httpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

export function initTranslate(translate: TranslateService, lang: LanguageService) {
  return () => firstValueFrom(translate.use(lang.effective()));
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimations(),
    provideNativeDateAdapter(),
    { provide: LOCALE_ID, useFactory: resolveLocale },
    { provide: MAT_DATE_LOCALE, useFactory: resolveLocale },
    { provide: MatPaginatorIntl, useClass: I18nPaginatorIntl },
    importProvidersFrom(
      TranslateModule.forRoot({
        defaultLanguage: 'pt',
        loader: {
          provide: TranslateLoader,
          useFactory: httpLoaderFactory,
          deps: [HttpClient]
        }
      })
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: initTranslate,
      deps: [TranslateService, LanguageService],
      multi: true
    }
  ]
};
