import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AdminCompany, AdminMetricsOverview, AdminUser, AppAlert, AppNotification,
  ClientLink, Company, CompanyClient, CompanyInvite, CompanyMember, CompanyMemberRole,
  CompanyMetricsOverview, CompanyNutriLink, CompanyOverview, DailySummary, Food,
  HealthProfile, Meal, MenuPlan, NutriCompany, NutriMetricsOverview, NutriProfileMe,
  NutriPublic, NutritionistLink,
  OnboardingInfo, PendingInvite, PlanTarget, Pricing,
  ProgressEntry, ProgressSummary, Subscription, User
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  me(): Observable<User> {
    return this.http.get<User>(`${this.base}/users/me`);
  }

  saveHealthProfile(profile: HealthProfile): Observable<User> {
    return this.http.put<User>(`${this.base}/users/me/health-profile`, profile);
  }

  searchFoods(q?: string): Observable<Food[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    return this.http.get<Food[]>(`${this.base}/foods`, { params });
  }

  listMeals(date: string): Observable<Meal[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<Meal[]>(`${this.base}/meals`, { params });
  }

  saveMeal(meal: Meal): Observable<Meal> {
    return meal.id
      ? this.http.put<Meal>(`${this.base}/meals/${meal.id}`, meal)
      : this.http.post<Meal>(`${this.base}/meals`, meal);
  }

  deleteMeal(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/meals/${id}`);
  }

  dashboard(date?: string): Observable<DailySummary> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<DailySummary>(`${this.base}/dashboard`, { params });
  }

  suggest(extraNotes?: string, scope: 'DAY' | 'WEEK' = 'DAY'): Observable<{ menu: string }> {
    return this.http.post<{ menu: string }>(`${this.base}/suggestions`, { extraNotes, scope });
  }

  listProgress(days?: number): Observable<ProgressEntry[]> {
    let params = new HttpParams();
    if (days) params = params.set('days', days);
    return this.http.get<ProgressEntry[]>(`${this.base}/progress`, { params });
  }

  progressSummary(days = 30): Observable<ProgressSummary> {
    const params = new HttpParams().set('days', days);
    return this.http.get<ProgressSummary>(`${this.base}/progress/summary`, { params });
  }

  saveProgress(entry: ProgressEntry): Observable<ProgressEntry> {
    return entry.id
      ? this.http.put<ProgressEntry>(`${this.base}/progress/${entry.id}`, entry)
      : this.http.post<ProgressEntry>(`${this.base}/progress`, entry);
  }

  deleteProgress(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/progress/${id}`);
  }

  listMenuPlans(): Observable<MenuPlan[]> {
    return this.http.get<MenuPlan[]>(`${this.base}/menu-plans`);
  }

  activeMenuPlan(): Observable<MenuPlan | null> {
    return this.http.get<MenuPlan | null>(`${this.base}/menu-plans/active`);
  }

  saveMenuPlan(plan: MenuPlan): Observable<MenuPlan> {
    return plan.id
      ? this.http.put<MenuPlan>(`${this.base}/menu-plans/${plan.id}`, plan)
      : this.http.post<MenuPlan>(`${this.base}/menu-plans`, plan);
  }

  setMenuPlanActive(id: number, value: boolean): Observable<MenuPlan> {
    const params = new HttpParams().set('value', value);
    return this.http.put<MenuPlan>(`${this.base}/menu-plans/${id}/active`, null, { params });
  }

  deleteMenuPlan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/menu-plans/${id}`);
  }

  nutriListClients(): Observable<ClientLink[]> {
    return this.http.get<ClientLink[]>(`${this.base}/nutritionist/clients`);
  }
  nutriInviteClient(clientEmail: string): Observable<ClientLink> {
    return this.http.post<ClientLink>(`${this.base}/nutritionist/clients/invite`, { clientEmail });
  }
  nutriRevokeClient(linkId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/nutritionist/clients/${linkId}`);
  }

  // Nutricionista <-> empresa
  nutriMyCompanies(): Observable<NutriCompany[]> {
    return this.http.get<NutriCompany[]>(`${this.base}/nutritionist/companies`);
  }
  nutriCompanyInvitesPending(): Observable<CompanyInvite[]> {
    return this.http.get<CompanyInvite[]>(`${this.base}/nutritionist/company-invitations`);
  }
  nutriAcceptCompany(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/nutritionist/company-invitations/${token}/accept`, null);
  }
  nutriRejectCompany(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/nutritionist/company-invitations/${token}/reject`, null);
  }
  nutriLeaveCompany(linkId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/nutritionist/companies/${linkId}`);
  }

  // Company
  companyMe(): Observable<Company> {
    return this.http.get<Company>(`${this.base}/company/me`);
  }
  companyUpdate(c: Company): Observable<Company> {
    return this.http.put<Company>(`${this.base}/company/me`, c);
  }
  companyOverview(): Observable<CompanyOverview> {
    return this.http.get<CompanyOverview>(`${this.base}/company/overview`);
  }
  companyNutritionists(): Observable<CompanyNutriLink[]> {
    return this.http.get<CompanyNutriLink[]>(`${this.base}/company/nutritionists`);
  }
  companyInviteNutri(email: string): Observable<CompanyNutriLink> {
    return this.http.post<CompanyNutriLink>(`${this.base}/company/nutritionists/invite`, { clientEmail: email });
  }
  companyRevokeNutri(linkId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/company/nutritionists/${linkId}`);
  }

  // Multi-admin da empresa
  companyListMembers(): Observable<CompanyMember[]> {
    return this.http.get<CompanyMember[]>(`${this.base}/company/members`);
  }
  companyInviteMember(email: string, role: CompanyMemberRole): Observable<CompanyMember> {
    return this.http.post<CompanyMember>(`${this.base}/company/members/invite`, { email, role });
  }
  companyRevokeMember(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/company/members/${id}`);
  }

  // Admin -> Companies (sales-led)
  adminListCompanies(): Observable<AdminCompany[]> {
    return this.http.get<AdminCompany[]>(`${this.base}/admin/companies`);
  }
  adminGetCompany(id: number): Observable<AdminCompany> {
    return this.http.get<AdminCompany>(`${this.base}/admin/companies/${id}`);
  }
  adminCreateCompany(payload: {
    name: string; tradeName?: string; taxId?: string; countryCode: string;
    contactEmail: string; contactPhone?: string; ownerEmail: string;
  }): Observable<AdminCompany> {
    return this.http.post<AdminCompany>(`${this.base}/admin/companies`, payload);
  }
  adminSetCompanyActive(id: number, value: boolean): Observable<AdminCompany> {
    const params = new HttpParams().set('value', value);
    return this.http.put<AdminCompany>(`${this.base}/admin/companies/${id}/active`, null, { params });
  }
  adminResendOwnerInvite(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/admin/companies/${id}/resend-owner-invite`, null);
  }
  adminDeleteCompany(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/companies/${id}`);
  }

  // Onboarding (link publico via token)
  onboardingLookup(token: string): Observable<OnboardingInfo> {
    return this.http.get<OnboardingInfo>(`${this.base}/onboarding/company/${token}`);
  }
  onboardingAccept(token: string, payload: { name: string; password: string; dataConsent: boolean }): Observable<any> {
    return this.http.post<any>(`${this.base}/onboarding/company/${token}/accept`, payload);
  }

  // Planos e pricing (publicos)
  pricing(currency?: string, target?: PlanTarget): Observable<Pricing> {
    let params = new HttpParams();
    if (currency) params = params.set('currency', currency);
    if (target) params = params.set('target', target);
    return this.http.get<Pricing>(`${this.base}/pricing`, { params });
  }
  detectCurrency(): Observable<{ currency: string }> {
    return this.http.get<{ currency: string }>(`${this.base}/pricing/detect-currency`);
  }

  // Subscription do usuario logado
  mySubscription(): Observable<Subscription | null> {
    return this.http.get<Subscription | null>(`${this.base}/subscription/me`);
  }

  // Checkout / cancelamento
  createCheckout(planCode: string, currency: string): Observable<{ checkoutUrl: string; sessionId: string }> {
    return this.http.post<{ checkoutUrl: string; sessionId: string }>(
      `${this.base}/checkout`, { planCode, currency });
  }
  cancelSubscription(atPeriodEnd = true): Observable<{ message: string }> {
    const params = new HttpParams().set('atPeriodEnd', atPeriodEnd);
    return this.http.post<{ message: string }>(`${this.base}/checkout/cancel`, null, { params });
  }

  myNutritionists(): Observable<NutritionistLink[]> {
    return this.http.get<NutritionistLink[]>(`${this.base}/me/nutritionists`);
  }
  revokeNutritionist(linkId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/me/nutritionists/${linkId}`);
  }

  pendingInvites(): Observable<PendingInvite[]> {
    return this.http.get<PendingInvite[]>(`${this.base}/me/invitations`);
  }
  acceptInvite(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/me/invitations/${token}/accept`, null);
  }
  rejectInvite(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/me/invitations/${token}/reject`, null);
  }

  adminListUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${this.base}/admin/users`);
  }

  adminSetActive(id: number, value: boolean): Observable<AdminUser> {
    const params = new HttpParams().set('value', value);
    return this.http.put<AdminUser>(`${this.base}/admin/users/${id}/active`, null, { params });
  }

  adminDelete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/users/${id}`);
  }

  adminMetricsOverview(): Observable<AdminMetricsOverview> {
    return this.http.get<AdminMetricsOverview>(`${this.base}/admin/metrics/overview`);
  }

  companyMetricsOverview(): Observable<CompanyMetricsOverview> {
    return this.http.get<CompanyMetricsOverview>(`${this.base}/company/metrics/overview`);
  }

  nutriMetricsOverview(): Observable<NutriMetricsOverview> {
    return this.http.get<NutriMetricsOverview>(`${this.base}/nutritionist/metrics/overview`);
  }

  listAlerts(scope: 'admin' | 'company' | 'nutritionist'): Observable<AppAlert[]> {
    return this.http.get<AppAlert[]>(`${this.base}/alerts/${scope}`);
  }

  dismissAlert(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/alerts/${id}/dismiss`, null);
  }

  // Notifications
  listNotifications(unreadOnly = false, limit = 20): Observable<AppNotification[]> {
    const params = new HttpParams().set('unreadOnly', unreadOnly).set('limit', limit);
    return this.http.get<AppNotification[]>(`${this.base}/notifications`, { params });
  }

  unreadNotificationCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.base}/notifications/unread-count`);
  }

  markNotificationRead(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/notifications/${id}/read`, null);
  }

  markAllNotificationsRead(): Observable<{ marked: number }> {
    return this.http.post<{ marked: number }>(`${this.base}/notifications/read-all`, null);
  }

  // Menu approval
  submitMenuPlan(id: number): Observable<MenuPlan> {
    return this.http.post<MenuPlan>(`${this.base}/menu-plans/${id}/submit`, null);
  }

  nutriPendingMenus(): Observable<MenuPlan[]> {
    return this.http.get<MenuPlan[]>(`${this.base}/nutritionist/menu-plans/pending`);
  }

  approveMenu(id: number): Observable<MenuPlan> {
    return this.http.post<MenuPlan>(`${this.base}/nutritionist/menu-plans/${id}/approve`, null);
  }

  rejectMenu(id: number, reason: string): Observable<MenuPlan> {
    return this.http.post<MenuPlan>(`${this.base}/nutritionist/menu-plans/${id}/reject`, { reason });
  }

  // 4D — Nutri discovery
  availableNutris(): Observable<NutriPublic[]> {
    return this.http.get<NutriPublic[]>(`${this.base}/nutritionists/available`);
  }

  nutriPublic(id: number): Observable<NutriPublic> {
    return this.http.get<NutriPublic>(`${this.base}/nutritionists/${id}`);
  }

  nutriProfileMe(): Observable<NutriProfileMe> {
    return this.http.get<NutriProfileMe>(`${this.base}/nutritionists/me/profile`);
  }

  updateNutriProfile(dto: NutriProfileMe): Observable<NutriPublic> {
    return this.http.put<NutriPublic>(`${this.base}/nutritionists/me/profile`, dto);
  }

  requestNutriLink(nutriId: number, message?: string): Observable<ClientLink> {
    return this.http.post<ClientLink>(`${this.base}/nutritionists/${nutriId}/request-link`, { message });
  }

  clientRequests(): Observable<ClientLink[]> {
    return this.http.get<ClientLink[]>(`${this.base}/nutritionist/client-requests`);
  }

  acceptClientRequest(linkId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/nutritionist/client-requests/${linkId}/accept`, null);
  }

  rejectClientRequest(linkId: number, note?: string): Observable<void> {
    return this.http.post<void>(`${this.base}/nutritionist/client-requests/${linkId}/reject`, { note });
  }

  canFindNutri(): Observable<{ allowed: boolean }> {
    return this.http.get<{ allowed: boolean }>(`${this.base}/nutritionists/can-find`);
  }

  // 4E — company gerencia clientes/nutris
  companyRevokeNutriWithAction(linkId: number, action: 'REVOKE_CLIENTS' | 'TRANSFER',
                                targetNutriId?: number): Observable<void> {
    return this.http.post<void>(`${this.base}/company/nutritionists/${linkId}/revoke`,
        { action, targetNutriId });
  }

  companyListClients(): Observable<CompanyClient[]> {
    return this.http.get<CompanyClient[]>(`${this.base}/company/clients`);
  }

  companyTransferClient(clientId: number, targetNutriId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/company/clients/${clientId}/nutri`,
        { targetNutriId });
  }
}
