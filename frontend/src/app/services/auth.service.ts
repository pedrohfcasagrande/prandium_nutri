import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, CurrentUser } from '../models/models';

const KEY = 'prandiumnutri.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  readonly currentUser = signal<CurrentUser | null>(this.load());
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly isNutritionist = computed(() => this.currentUser()?.role === 'NUTRITIONIST');
  readonly isCompany = computed(() => this.currentUser()?.role === 'COMPANY');

  register(payload: { name: string; email: string; password: string; dataConsent: boolean; role?: 'USER' | 'NUTRITIONIST' }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/auth/register`, payload).pipe(tap(r => this.store(r)));
  }

  login(payload: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/auth/login`, payload).pipe(tap(r => this.store(r)));
  }

  loginWithGoogle(idToken: string, dataConsent: boolean): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/auth/oauth/google`, { idToken, dataConsent })
      .pipe(tap(r => this.store(r)));
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.base}/auth/change-password`, { currentPassword, newPassword });
  }

  logout() {
    localStorage.removeItem(KEY);
    sessionStorage.removeItem('prandiumnutri.impersonate');
    this.currentUser.set(null);
  }

  markProfileCompleted() {
    const u = this.currentUser();
    if (u) {
      const updated = { ...u, profileCompleted: true };
      localStorage.setItem(KEY, JSON.stringify(updated));
      this.currentUser.set(updated);
    }
  }

  token(): string | null {
    return this.currentUser()?.token ?? null;
  }

  private store(r: AuthResponse) {
    const user: CurrentUser = {
      userId: r.userId, email: r.email, name: r.name,
      role: r.role, profileCompleted: r.profileCompleted, token: r.token
    };
    localStorage.setItem(KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private load(): CurrentUser | null {
    const raw = localStorage.getItem(KEY);
    return raw ? JSON.parse(raw) as CurrentUser : null;
  }
}
