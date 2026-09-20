import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ImpersonationService } from './impersonation.service';

// Single-flight: uma unica chamada /refresh por vez, demais aguardam o resultado.
let refreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const imp = inject(ImpersonationService);
  const router = inject(Router);

  return next(withAuthHeaders(req, auth, imp)).pipe(
    catchError((err: HttpErrorResponse) => {
      // 401: tenta refresh (exceto se ja for uma chamada de auth)
      if (err.status === 401 && !req.url.includes('/auth/')) {
        return handle401(req, next, auth, imp, router);
      }
      // 402 = plan limit reached -> redireciona para pricing com hint
      if (err.status === 402 && err.error?.error === 'plan_limit_reached') {
        const params = new URLSearchParams({
          feature: err.error.feature || '',
          plan: err.error.planCode || ''
        });
        router.navigateByUrl('/pricing?' + params.toString());
      }
      return throwError(() => err);
    })
  );
};

function withAuthHeaders(req: HttpRequest<unknown>, auth: AuthService, imp: ImpersonationService): HttpRequest<unknown> {
  const headers: Record<string, string> = {};
  const token = auth.token();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const impHeader = imp.header();
  if (impHeader) headers['X-Impersonate-User-Id'] = impHeader;
  return Object.keys(headers).length ? req.clone({ setHeaders: headers }) : req;
}

function handle401(
  req: HttpRequest<unknown>, next: HttpHandlerFn,
  auth: AuthService, imp: ImpersonationService, router: Router
): Observable<any> {
  if (!auth.currentUser()?.refreshToken) {
    auth.logout();
    router.navigate(['/login']);
    return throwError(() => new HttpErrorResponse({ status: 401 }));
  }

  if (refreshing) {
    // Espera o refresh em andamento e depois retenta a request original com o novo token.
    return refreshedToken$.pipe(
      filter(t => t !== null),
      take(1),
      switchMap(() => next(withAuthHeaders(req, auth, imp)))
    );
  }

  refreshing = true;
  refreshedToken$.next(null);
  return auth.refresh().pipe(
    switchMap(r => {
      refreshing = false;
      refreshedToken$.next(r.accessToken);
      return next(withAuthHeaders(req, auth, imp));
    }),
    catchError(err => {
      refreshing = false;
      refreshedToken$.next(null);
      auth.logout();
      router.navigate(['/login']);
      return throwError(() => err);
    })
  );
}
