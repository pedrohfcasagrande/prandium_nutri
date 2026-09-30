import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Fase 9G-4: registra service worker + gerencia push subscription.
 * Silencia (no-op) quando o browser nao suporta ou o backend nao tem VAPID.
 */
@Injectable({ providedIn: 'root' })
export class PushService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  readonly supported = signal<boolean>(this.detectSupport());
  readonly permission = signal<NotificationPermission>(
    this.detectSupport() ? Notification.permission : 'denied'
  );
  readonly subscribed = signal<boolean>(false);
  private vapidPublicKey: string | null = null;

  private detectSupport(): boolean {
    return typeof window !== 'undefined'
      && 'serviceWorker' in navigator
      && 'PushManager' in window
      && 'Notification' in window;
  }

  /** Registra o SW e atualiza {@link subscribed} conforme estado atual. */
  async init(): Promise<void> {
    if (!this.supported()) return;
    try {
      const reg = await navigator.serviceWorker.register('/sw.js');
      const existing = await reg.pushManager.getSubscription();
      this.subscribed.set(!!existing);
    } catch (e) {
      console.warn('Falha ao registrar SW:', e);
      this.supported.set(false);
    }
  }

  /**
   * Pede permissao (se ainda nao concedida) e cria/registra a subscription.
   * Retorna true se ficou subscribed, false caso contrario.
   */
  async subscribe(): Promise<boolean> {
    if (!this.supported()) return false;
    try {
      const config = await firstValueFrom(
        this.http.get<{ vapidPublicKey: string; enabled: boolean }>(
          `${this.base}/me/push-subscriptions/config`
        )
      );
      if (!config.enabled || !config.vapidPublicKey) {
        console.warn('Push desabilitado no backend (sem VAPID).');
        return false;
      }
      this.vapidPublicKey = config.vapidPublicKey;

      if (Notification.permission !== 'granted') {
        const perm = await Notification.requestPermission();
        this.permission.set(perm);
        if (perm !== 'granted') return false;
      }

      const reg = await navigator.serviceWorker.ready;
      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(this.vapidPublicKey)
      });

      const raw = sub.toJSON() as any;
      await firstValueFrom(this.http.post(`${this.base}/me/push-subscriptions`, {
        endpoint: sub.endpoint,
        p256dh: raw.keys?.p256dh,
        auth: raw.keys?.auth,
        userAgent: navigator.userAgent
      }));
      this.subscribed.set(true);
      return true;
    } catch (e) {
      console.warn('Falha ao subscribe push:', e);
      return false;
    }
  }

  /** Cancela subscription local e remove do backend. */
  async unsubscribe(): Promise<void> {
    if (!this.supported()) return;
    try {
      const reg = await navigator.serviceWorker.ready;
      const sub = await reg.pushManager.getSubscription();
      if (sub) {
        const endpoint = sub.endpoint;
        await sub.unsubscribe();
        await firstValueFrom(this.http.delete(
          `${this.base}/me/push-subscriptions?endpoint=${encodeURIComponent(endpoint)}`
        ));
      }
      this.subscribed.set(false);
    } catch (e) {
      console.warn('Falha ao unsubscribe push:', e);
    }
  }
}

/** Converte VAPID public key (base64url) para Uint8Array esperado pelo PushManager. */
function urlBase64ToUint8Array(base64: string): Uint8Array {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4);
  const b64 = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(b64);
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out;
}
