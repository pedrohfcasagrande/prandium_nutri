import { Injectable, inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { ThemeService } from './theme.service';

declare const google: any;

@Injectable({ providedIn: 'root' })
export class GoogleSigninService {
  private theme = inject(ThemeService);

  renderButton(element: HTMLElement, callback: (idToken: string) => void) {
    if (!environment.googleClientId) {
      element.innerText = 'Google Sign-In não configurado';
      return;
    }
    const doRender = () => {
      if (typeof google === 'undefined' || !google.accounts) {
        setTimeout(doRender, 200);
        return;
      }
      google.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: (resp: any) => callback(resp.credential)
      });
      const draw = () => {
        element.innerHTML = '';
        google.accounts.id.renderButton(element, {
          type: 'standard',
          theme: this.theme.effectiveDark() ? 'filled_black' : 'outline',
          size: 'large',
          width: 280,
          text: 'signin_with',
          shape: 'rectangular',
          logo_alignment: 'left'
        });
      };
      draw();
      let last = this.theme.effectiveDark();
      const observer = new MutationObserver(() => {
        const current = this.theme.effectiveDark();
        if (current !== last) { last = current; draw(); }
      });
      observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    };
    doRender();
  }
}
