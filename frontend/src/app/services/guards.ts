import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { ImpersonationService } from './impersonation.service';

/**
 * Guard para features de usuario (dashboard, refeicoes, sugestoes, evolucao, meu perfil).
 * - Usuario comum: precisa estar autenticado e com perfil completo.
 * - Admin/Nutri/Company: so entra se estiver no modo "Gerenciar" (impersonation ativa).
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const imp = inject(ImpersonationService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) { router.navigate(['/login']); return false; }

  if (auth.isAdmin()) {
    if (!imp.target()) { router.navigate(['/admin/users']); return false; }
    return true;
  }
  if (auth.isNutritionist()) {
    if (!imp.target()) { router.navigate(['/nutritionist/clients']); return false; }
    return true;
  }
  if (auth.isCompany()) {
    if (!imp.target()) { router.navigate(['/company/dashboard']); return false; }
    return true;
  }
  const user = auth.currentUser()!;
  if (!user.profileCompleted) { router.navigate(['/profile-setup']); return false; }
  return true;
};

/**
 * Guard "puro" — só exige autenticação. Não gata por role nem force impersonation.
 * Usado por features cross-role como o chat, onde nutri e cliente compartilham a
 * mesma tela (thread por linkId).
 */
export const loggedInGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) { router.navigate(['/login']); return false; }
  return true;
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    const u = auth.currentUser()!;
    if (u.role === 'ADMIN') router.navigate(['/admin/users']);
    else if (u.role === 'NUTRITIONIST') router.navigate(['/nutritionist/clients']);
    else if (u.role === 'COMPANY') router.navigate(['/company/dashboard']);
    else router.navigate([u.profileCompleted ? '/dashboard' : '/profile-setup']);
    return false;
  }
  return true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) { router.navigate(['/login']); return false; }
  if (!auth.isAdmin()) { router.navigate(['/dashboard']); return false; }
  return true;
};

export const nutritionistGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) { router.navigate(['/login']); return false; }
  if (!auth.isNutritionist()) { router.navigate(['/dashboard']); return false; }
  return true;
};

export const companyGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) { router.navigate(['/login']); return false; }
  if (!auth.isCompany()) { router.navigate(['/dashboard']); return false; }
  return true;
};

export const profileSetupGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const imp = inject(ImpersonationService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) { router.navigate(['/login']); return false; }
  if (auth.isAdmin() || auth.isNutritionist() || auth.isCompany()) return !!imp.target();
  const u = auth.currentUser()!;
  if (u.profileCompleted) { router.navigate(['/dashboard']); return false; }
  return true;
};
