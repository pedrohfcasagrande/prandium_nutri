import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ImpersonationService } from './impersonation.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const imp = inject(ImpersonationService);
  const router = inject(Router);
  const token = auth.token();

  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const impHeader = imp.header();
  // Envia header quando ha impersonation ativa (backend valida se o role atual pode fazer)
  if (impHeader) headers['X-Impersonate-User-Id'] = impHeader;

  const cloned = Object.keys(headers).length ? req.clone({ setHeaders: headers }) : req;

  return next(cloned).pipe(
    catchError(err => {
      if (err.status === 401 && !req.url.includes('/auth/')) {
        auth.logout();
        router.navigate(['/login']);
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
