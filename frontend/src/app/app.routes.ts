import { Routes } from '@angular/router';
import { adminGuard, authGuard, companyGuard, guestGuard, nutritionistGuard, profileSetupGuard } from './services/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'company-onboarding',
    loadComponent: () => import('./pages/company-onboarding/company-onboarding.component').then(m => m.CompanyOnboardingComponent)
  },
  {
    path: 'pricing',
    loadComponent: () => import('./pages/pricing/pricing.component').then(m => m.PricingComponent)
  },
  {
    path: 'billing',
    loadComponent: () => import('./pages/billing/billing.component').then(m => m.BillingComponent)
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./pages/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: 'profile-setup',
    canActivate: [profileSetupGuard],
    loadComponent: () => import('./pages/profile-setup/profile-setup.component').then(m => m.ProfileSetupComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'meals',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/meals/meals.component').then(m => m.MealsComponent)
  },
  {
    path: 'suggestions',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/suggestions/suggestions.component').then(m => m.SuggestionsComponent)
  },
  {
    path: 'menus',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/menus/menus.component').then(m => m.MenusComponent)
  },
  {
    path: 'progress',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/progress/progress.component').then(m => m.ProgressComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: 'nutritionist/dashboard',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/nutri-dashboard/nutri-dashboard.component').then(m => m.NutriDashboardComponent)
  },
  {
    path: 'nutritionist/clients',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/nutri-clients/nutri-clients.component').then(m => m.NutriClientsComponent)
  },
  {
    path: 'nutritionist/metrics',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/nutri-metrics/nutri-metrics.component').then(m => m.NutriMetricsComponent)
  },
  {
    path: 'nutritionist/pending-menus',
    redirectTo: 'nutritionist/menu-plans'
  },
  {
    path: 'nutritionist/menu-plans',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/nutri-menu-plans/nutri-menu-plans.component').then(m => m.NutriMenuPlansComponent)
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent)
  },
  {
    path: 'find-nutritionist',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/find-nutritionist/find-nutritionist.component').then(m => m.FindNutritionistComponent)
  },
  {
    path: 'nutritionist/public-profile',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/nutri-profile/nutri-profile.component').then(m => m.NutriProfileComponent)
  },
  {
    path: 'nutritionist/company-invitations',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/nutri-company-invites/nutri-company-invites.component').then(m => m.NutriCompanyInvitesComponent)
  },
  {
    path: 'nutritionist/practices',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/nutri-practices/nutri-practices.component').then(m => m.NutriPracticesComponent)
  },
  {
    path: 'invitations',
    loadComponent: () => import('./pages/invitations/invitations.component').then(m => m.InvitationsComponent)
  },
  {
    path: 'company/dashboard',
    canActivate: [companyGuard],
    loadComponent: () => import('./pages/company-dashboard/company-dashboard.component').then(m => m.CompanyDashboardComponent)
  },
  {
    path: 'company/metrics',
    canActivate: [companyGuard],
    loadComponent: () => import('./pages/company-metrics/company-metrics.component').then(m => m.CompanyMetricsComponent)
  },
  {
    path: 'company/nutritionists',
    canActivate: [companyGuard],
    loadComponent: () => import('./pages/company-nutritionists/company-nutritionists.component').then(m => m.CompanyNutritionistsComponent)
  },
  {
    path: 'company/clients',
    canActivate: [companyGuard],
    loadComponent: () => import('./pages/company-clients/company-clients.component').then(m => m.CompanyClientsComponent)
  },
  {
    path: 'company/members',
    canActivate: [companyGuard],
    loadComponent: () => import('./pages/company-members/company-members.component').then(m => m.CompanyMembersComponent)
  },
  {
    path: 'admin/metrics',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin-metrics/admin-metrics.component').then(m => m.AdminMetricsComponent)
  },
  {
    path: 'admin/users',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin-users/admin-users.component').then(m => m.AdminUsersComponent)
  },
  {
    path: 'admin/companies',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin-companies/admin-companies.component').then(m => m.AdminCompaniesComponent)
  },
  {
    path: 'appointments',
    canActivate: [authGuard],
    data: { mode: 'CLIENT' },
    loadComponent: () => import('./pages/appointments/appointments.component').then(m => m.AppointmentsComponent)
  },
  {
    path: 'nutritionist/appointments',
    canActivate: [nutritionistGuard],
    data: { mode: 'NUTRI' },
    loadComponent: () => import('./pages/appointments/appointments.component').then(m => m.AppointmentsComponent)
  },
  {
    path: 'company/appointments',
    canActivate: [companyGuard],
    data: { mode: 'COMPANY' },
    loadComponent: () => import('./pages/appointments/appointments.component').then(m => m.AppointmentsComponent)
  },
  {
    path: 'nutritionist/clients/:clientId/assessment',
    canActivate: [nutritionistGuard],
    loadComponent: () => import('./pages/assessment/nutri-assessment.component').then(m => m.NutriAssessmentComponent)
  },
  {
    path: 'my-assessment',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/assessment/my-assessment.component').then(m => m.MyAssessmentComponent)
  }
];
