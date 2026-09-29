import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AdminCompany, AdminMetricsOverview, AdminUser, AppAlert, AppNotification,
  Appointment, AppointmentRequest, AssessmentSelfFill, AssessmentSnapshot, ClientAssessment,
  ClientLink, Company, CompanyClient, CompanyInvite, CompanyMember, CompanyMemberRole,
  ClientChecklist,
  Consultation,
  PdfChangeRequest, PdfChangeRequestStatus,
  PdfCustomization, PdfTemplateView,
  PdfVerification, PhotoAnalysisItem, PrescriptionIssuance, StreakInfo, WaterIntake,
  CompanyMetricsOverview, CompanyNutriLink, CompanyOverview, DailySummary, Food,
  HealthProfile, Meal, MenuPlan, NutriCompany, NutriMetricsOverview, NutriProfileMe,
  NutriPublic, NutritionistLink,
  OnboardingInfo, Page, PendingInvite, PlanTarget, Practice, Pricing,
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

  aiLookupFoods(q: string): Observable<Food[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Food[]>(`${this.base}/foods/ai-lookup`, { params });
  }

  createFood(food: Food): Observable<Food> {
    return this.http.post<Food>(`${this.base}/foods`, food);
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

  /** Fase 10A: envia foto do prato pra IA identificar alimentos. */
  analyzeMealPhoto(imageBase64: string, mimeType: string): Observable<PhotoAnalysisItem[]> {
    return this.http.post<PhotoAnalysisItem[]>(
      `${this.base}/meals/analyze-photo`, { imageBase64, mimeType });
  }

  dashboard(date?: string): Observable<DailySummary> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<DailySummary>(`${this.base}/dashboard`, { params });
  }

  /** Fase 10B: hidratacao. */
  listWaterIntakes(date?: string): Observable<WaterIntake[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<WaterIntake[]>(`${this.base}/me/water`, { params });
  }

  logWater(amountMl: number): Observable<WaterIntake> {
    return this.http.post<WaterIntake>(`${this.base}/me/water`, { amountMl });
  }

  deleteWater(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/me/water/${id}`);
  }

  /** Fase 10C: streak + badges. */
  streakInfo(): Observable<StreakInfo> {
    return this.http.get<StreakInfo>(`${this.base}/me/streaks`);
  }

  /** Fase 10D: onboarding checklist do cliente. */
  myChecklist(): Observable<ClientChecklist> {
    return this.http.get<ClientChecklist>(`${this.base}/me/checklist`);
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

  nutriListClients(practiceId?: number | null, q?: string, sort?: string,
                    page = 0, size = 10): Observable<Page<ClientLink>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (practiceId != null) params = params.set('practiceId', practiceId);
    if (q) params = params.set('q', q);
    if (sort) params = params.set('sort', sort);
    return this.http.get<Page<ClientLink>>(`${this.base}/nutritionist/clients`, { params });
  }
  nutriInviteClient(clientEmail: string, practiceId?: number | null): Observable<ClientLink> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.post<ClientLink>(`${this.base}/nutritionist/clients/invite`, { clientEmail }, { params });
  }
  nutriRevokeClient(linkId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/nutritionist/clients/${linkId}`);
  }

  // Nutricionista <-> empresa
  nutriMyCompanies(): Observable<NutriCompany[]> {
    return this.http.get<NutriCompany[]>(`${this.base}/nutritionist/companies`);
  }
  nutriMyPractices(): Observable<Practice[]> {
    return this.http.get<Practice[]>(`${this.base}/nutritionist/practices`);
  }
  nutriUpdatePractice(id: number, dto: { bio?: string; specialties?: string; acceptsNewClients: boolean }): Observable<Practice> {
    return this.http.put<Practice>(`${this.base}/nutritionist/practices/${id}`, dto);
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
  companyNutritionists(q?: string, sort?: string, page = 0, size = 10): Observable<Page<CompanyNutriLink>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    if (sort) params = params.set('sort', sort);
    return this.http.get<Page<CompanyNutriLink>>(`${this.base}/company/nutritionists`, { params });
  }
  companyInviteNutri(email: string): Observable<CompanyNutriLink> {
    return this.http.post<CompanyNutriLink>(`${this.base}/company/nutritionists/invite`, { clientEmail: email });
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
  adminListCompanies(q?: string, sort?: string, page = 0, size = 10): Observable<Page<AdminCompany>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    if (sort) params = params.set('sort', sort);
    return this.http.get<Page<AdminCompany>>(`${this.base}/admin/companies`, { params });
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
  pricing(currency?: string, target?: PlanTarget | null): Observable<Pricing> {
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

  adminListUsers(q?: string, sort?: string, page = 0, size = 10): Observable<Page<AdminUser>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    if (sort) params = params.set('sort', sort);
    return this.http.get<Page<AdminUser>>(`${this.base}/admin/users`, { params });
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

  nutriMetricsOverview(practiceId?: number | null): Observable<NutriMetricsOverview> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<NutriMetricsOverview>(`${this.base}/nutritionist/metrics/overview`, { params });
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

  nutriPendingMenus(practiceId?: number | null): Observable<MenuPlan[]> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<MenuPlan[]>(`${this.base}/nutritionist/menu-plans/pending`, { params });
  }

  approveMenu(id: number): Observable<MenuPlan> {
    return this.http.post<MenuPlan>(`${this.base}/nutritionist/menu-plans/${id}/approve`, null);
  }

  rejectMenu(id: number, reason: string): Observable<MenuPlan> {
    return this.http.post<MenuPlan>(`${this.base}/nutritionist/menu-plans/${id}/reject`, { reason });
  }

  nutriListClientMenus(clientId: number, practiceId?: number | null): Observable<MenuPlan[]> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<MenuPlan[]>(`${this.base}/nutritionist/menu-plans/client/${clientId}`, { params });
  }

  nutriCreateMenuForClient(clientId: number, dto: MenuPlan, practiceId?: number | null): Observable<MenuPlan> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.post<MenuPlan>(`${this.base}/nutritionist/menu-plans/for/${clientId}`, dto, { params });
  }

  nutriUpdateMenu(id: number, dto: MenuPlan, practiceId?: number | null): Observable<MenuPlan> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.put<MenuPlan>(`${this.base}/nutritionist/menu-plans/${id}`, dto, { params });
  }

  nutriDeleteMenu(id: number, practiceId?: number | null): Observable<void> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.delete<void>(`${this.base}/nutritionist/menu-plans/${id}`, { params });
  }

  /** Nutri gera cardapio via IA para o cliente. Retorna sugestao em markdown. */
  nutriGenerateMenuForClient(clientId: number, opts: { extraNotes?: string; scope?: 'DAY' | 'WEEK' } = {},
                              practiceId?: number | null): Observable<{ menu: string }> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.post<{ menu: string }>(
      `${this.base}/nutritionist/menu-plans/generate/${clientId}`,
      { extraNotes: opts.extraNotes ?? null, scope: opts.scope ?? 'WEEK' },
      { params });
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

  clientRequests(practiceId?: number | null): Observable<ClientLink[]> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<ClientLink[]>(`${this.base}/nutritionist/client-requests`, { params });
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

  companyListClients(q?: string, sort?: string, page = 0, size = 10): Observable<Page<CompanyClient>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q) params = params.set('q', q);
    if (sort) params = params.set('sort', sort);
    return this.http.get<Page<CompanyClient>>(`${this.base}/company/clients`, { params });
  }

  companyTransferClient(clientId: number, targetNutriId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/company/clients/${clientId}/nutri`,
        { targetNutriId });
  }

  // ============ Appointments (Fase 8) ============

  clientListAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.base}/appointments`);
  }
  clientRequestAppointment(dto: AppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/appointments`, dto);
  }
  clientConfirmAppointment(id: number): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/appointments/${id}/confirm`, null);
  }
  clientCancelAppointment(id: number, reason?: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/appointments/${id}/cancel`, { reason });
  }
  clientRescheduleAppointment(id: number, scheduledAt: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/appointments/${id}/reschedule`, { scheduledAt });
  }

  nutriListAppointments(practiceId?: number | null): Observable<Appointment[]> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<Appointment[]>(`${this.base}/nutritionist/appointments`, { params });
  }
  nutriSuggestAppointment(dto: AppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/nutritionist/appointments`, dto);
  }
  nutriConfirmAppointment(id: number): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/nutritionist/appointments/${id}/confirm`, null);
  }
  nutriCancelAppointment(id: number, reason?: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/nutritionist/appointments/${id}/cancel`, { reason });
  }
  nutriRescheduleAppointment(id: number, scheduledAt: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/nutritionist/appointments/${id}/reschedule`, { scheduledAt });
  }
  nutriCompleteAppointment(id: number): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/nutritionist/appointments/${id}/complete`, null);
  }
  nutriNoShowAppointment(id: number): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.base}/nutritionist/appointments/${id}/no-show`, null);
  }

  companyListAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.base}/company/appointments`);
  }

  // ============ Ficha de análise (Fase 8.2) ============

  nutriGetAssessment(clientId: number, practiceId?: number | null): Observable<ClientAssessment | null> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<ClientAssessment | null>(
      `${this.base}/nutritionist/assessments/client/${clientId}`, { params });
  }

  nutriUpsertAssessment(clientId: number, dto: ClientAssessment,
                         practiceId?: number | null): Observable<ClientAssessment> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.put<ClientAssessment>(
      `${this.base}/nutritionist/assessments/client/${clientId}`, dto, { params });
  }

  myAssessment(): Observable<ClientAssessment | null> {
    return this.http.get<ClientAssessment | null>(`${this.base}/me/assessment`);
  }

  // Snapshots (Fase 9A) — timeline imutavel de "consultas"
  nutriCreateAssessmentSnapshot(clientId: number, label: string | null,
                                 practiceId?: number | null): Observable<AssessmentSnapshot> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.post<AssessmentSnapshot>(
      `${this.base}/nutritionist/assessments/client/${clientId}/snapshots`,
      { label }, { params });
  }
  nutriListAssessmentSnapshots(clientId: number,
                                practiceId?: number | null): Observable<AssessmentSnapshot[]> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<AssessmentSnapshot[]>(
      `${this.base}/nutritionist/assessments/client/${clientId}/snapshots`, { params });
  }
  myAssessmentSnapshots(): Observable<AssessmentSnapshot[]> {
    return this.http.get<AssessmentSnapshot[]>(`${this.base}/me/assessment/snapshots`);
  }

  // Fase 9D-3: log de emissoes de PDF + verificacao publica
  nutriListIssuances(clientId: number, practiceId?: number | null): Observable<PrescriptionIssuance[]> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<PrescriptionIssuance[]>(
      `${this.base}/nutritionist/assessments/client/${clientId}/issuances`, { params });
  }
  myIssuances(): Observable<PrescriptionIssuance[]> {
    return this.http.get<PrescriptionIssuance[]>(`${this.base}/me/assessment/issuances`);
  }
  verifyPdf(hash: string): Observable<PdfVerification> {
    return this.http.get<PdfVerification>(`${this.base}/pdf-verify/${hash}`);
  }

  // Self-fill (Fase 9A) — cliente atualiza campos safe da propria ficha
  myAssessmentSelfFill(dto: AssessmentSelfFill): Observable<ClientAssessment> {
    return this.http.put<ClientAssessment>(`${this.base}/me/assessment/self-fill`, dto);
  }

  // Fase 9D: PDF de prescricao (nutri + cliente) + assinatura do nutri
  // Uso responseType:'blob' + HttpClient pra que o interceptor de auth inclua o JWT.
  nutriPrescriptionPdf(clientId: number, practiceId?: number | null): Observable<Blob> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get(
      `${this.base}/nutritionist/assessments/client/${clientId}/prescription.pdf`,
      { params, responseType: 'blob' });
  }
  myPrescriptionPdf(): Observable<Blob> {
    return this.http.get(`${this.base}/me/assessment/prescription.pdf`, { responseType: 'blob' });
  }
  nutriGetSignature(): Observable<{ signatureBase64: string }> {
    return this.http.get<{ signatureBase64: string }>(`${this.base}/nutritionist/me/signature`);
  }
  nutriPutSignature(signatureBase64: string | null): Observable<void> {
    return this.http.put<void>(`${this.base}/nutritionist/me/signature`, { signatureBase64 });
  }

  // Fase 9H-1: template PDF resolvido por dono (COMPANY ou PRACTICE)
  nutriGetPdfTemplate(practiceId: number): Observable<PdfTemplateView> {
    return this.http.get<PdfTemplateView>(
      `${this.base}/nutritionist/practices/${practiceId}/pdf-template`);
  }
  nutriPutPdfTemplate(practiceId: number, dto: PdfCustomization): Observable<void> {
    return this.http.put<void>(
      `${this.base}/nutritionist/practices/${practiceId}/pdf-template`, dto);
  }
  nutriPdfPreview(practiceId: number): Observable<Blob> {
    return this.http.get(
      `${this.base}/nutritionist/practices/${practiceId}/pdf-preview`,
      { responseType: 'blob' });
  }
  companyGetPdfTemplate(): Observable<PdfTemplateView> {
    return this.http.get<PdfTemplateView>(`${this.base}/company/pdf-template`);
  }
  companyPutPdfTemplate(dto: PdfCustomization): Observable<void> {
    return this.http.put<void>(`${this.base}/company/pdf-template`, dto);
  }
  companyPdfPreview(): Observable<Blob> {
    return this.http.get(`${this.base}/company/pdf-template/preview`, { responseType: 'blob' });
  }

  // Fase 9H-3: fluxo de aprovacao
  nutriCreatePdfChangeRequest(practiceId: number, proposed: PdfCustomization,
                                note: string | null): Observable<PdfChangeRequest> {
    return this.http.post<PdfChangeRequest>(
      `${this.base}/nutritionist/pdf-change-requests`,
      { practiceId, proposed, note });
  }
  nutriMyPendingPdfChangeRequests(): Observable<PdfChangeRequest[]> {
    return this.http.get<PdfChangeRequest[]>(
      `${this.base}/nutritionist/pdf-change-requests/mine-pending`);
  }
  companyListPdfChangeRequests(status?: PdfChangeRequestStatus): Observable<PdfChangeRequest[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<PdfChangeRequest[]>(
      `${this.base}/company/pdf-change-requests`, { params });
  }
  companyApprovePdfChangeRequest(id: number, note: string | null): Observable<PdfChangeRequest> {
    return this.http.post<PdfChangeRequest>(
      `${this.base}/company/pdf-change-requests/${id}/approve`, { note });
  }
  companyRejectPdfChangeRequest(id: number, note: string | null): Observable<PdfChangeRequest> {
    return this.http.post<PdfChangeRequest>(
      `${this.base}/company/pdf-change-requests/${id}/reject`, { note });
  }

  // Fase 9E: consultas (prontuário)
  nutriListConsultations(clientId: number, practiceId?: number | null): Observable<Consultation[]> {
    let params = new HttpParams();
    if (practiceId != null) params = params.set('practiceId', practiceId);
    return this.http.get<Consultation[]>(
      `${this.base}/nutritionist/consultations/client/${clientId}`, { params });
  }
  nutriCreateConsultation(clientId: number, dto: Consultation,
                           opts: { snapshot?: boolean; practiceId?: number | null } = {})
                           : Observable<Consultation> {
    let params = new HttpParams();
    if (opts.snapshot === false) params = params.set('snapshot', 'false');
    if (opts.practiceId != null) params = params.set('practiceId', opts.practiceId);
    return this.http.post<Consultation>(
      `${this.base}/nutritionist/consultations/client/${clientId}`, dto, { params });
  }
  nutriUpdateConsultation(id: number, dto: Consultation): Observable<Consultation> {
    return this.http.put<Consultation>(`${this.base}/nutritionist/consultations/${id}`, dto);
  }
  nutriDeleteConsultation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/nutritionist/consultations/${id}`);
  }
  myConsultations(): Observable<Consultation[]> {
    return this.http.get<Consultation[]>(`${this.base}/me/assessment/consultations`);
  }
}
