export type Sex = 'MALE' | 'FEMALE';
export type ActivityLevel = 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE';
export type Goal = 'LOSE' | 'MAINTAIN' | 'GAIN';
export type MealType = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER' | 'OTHER';
export type Role = 'USER' | 'NUTRITIONIST' | 'COMPANY' | 'ADMIN';

/** Envelope de paginação (Fase 7B). page/size 0-based. */
export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
export type LinkStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'REVOKED_BY_CLIENT' | 'REVOKED_BY_NUTRI' | 'REVOKED_BY_COMPANY' | 'EXPIRED';
export type CompanyMemberRole = 'OWNER' | 'ADMIN';

export interface AdminCompany {
  id: number;
  name: string;
  tradeName?: string;
  taxId?: string;
  countryCode: string;
  contactEmail: string;
  contactPhone?: string;
  active: boolean;
  createdAt: string;
  ownerUserId?: number;
  ownerName?: string;
  ownerEmail?: string;
  activeMembersCount: number;
  pendingInvitesCount: number;
  nutritionistsCount: number;
}

export interface CompanyMember {
  id: number;
  userId?: number;
  userName: string;
  userEmail: string;
  roleInCompany: CompanyMemberRole;
  status: LinkStatus;
  invitedAt: string;
  acceptedAt?: string;
}

export interface OnboardingInfo {
  companyId: number;
  companyName: string;
  invitedEmail: string;
  role: CompanyMemberRole;
  userExists: boolean;
}

export type PlanTarget = 'COMPANY' | 'NUTRITIONIST' | 'USER';
export type PaymentProvider = 'MERCADO_PAGO' | 'STRIPE' | 'MANUAL';
export type SubscriptionStatus = 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED' | 'SUSPENDED';

export interface Plan {
  code: string;
  target: PlanTarget;
  tier: string;
  name: string;
  features: Record<string, any>;
  currency: string;
  amountCents: number;
  provider?: PaymentProvider;
}

export interface Pricing {
  currency: string;
  plans: Plan[];
}

export interface Subscription {
  id: number;
  payerType: PlanTarget;
  payerId: number;
  planCode: string;
  planName: string;
  planTier: string;
  features: Record<string, any>;
  currency: string;
  amountCents: number;
  provider: PaymentProvider;
  status: SubscriptionStatus;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
  currentMonthUsage: Record<string, number>;
}

export interface Company {
  id?: number;
  name: string;
  tradeName?: string;
  taxId?: string;
  countryCode: string;
  contactEmail: string;
  contactPhone?: string;
  active: boolean;
}

export interface CompanyOverview {
  nutritionistsActive: number;
  nutritionistsPending: number;
  clientsTotal: number;
}

export interface CompanyNutriLink {
  id: number;
  nutritionistId: number | null;
  nutritionistName: string;
  nutritionistEmail: string;
  nutritionistCpf?: string;
  nutritionistCrn?: string;
  status: LinkStatus;
  invitedAt: string;
  acceptedAt?: string;
  clientsCount: number;
}

export interface NutriCompany {
  linkId: number;
  companyId: number;
  companyName: string;
  status: LinkStatus;
  acceptedAt?: string;
}

export interface CompanyInvite {
  linkId: number;
  token: string;
  companyId: number;
  companyName: string;
  invitedAt: string;
  expiresAt: string;
}

export type PracticeStatus = 'ACTIVE' | 'SUSPENDED';
export interface Practice {
  id: number;
  companyId: number | null;
  companyName: string | null;
  bio: string | null;
  specialties: string | null;
  acceptsNewClients: boolean;
  status: PracticeStatus;
}

export interface ClientLink {
  id: number;
  clientId: number | null;
  clientName: string;
  clientEmail: string;
  clientCpf?: string;
  status: LinkStatus;
  invitedAt: string;
  acceptedAt?: string;
  revokedAt?: string;
  subscriptionStatus?: SubscriptionStatus | null;
  subscriptionPlanCode?: string | null;
  subscriptionPlanName?: string | null;
  subscriptionRenewsAt?: string | null;
}

export interface NutritionistLink {
  id: number;
  nutritionistId: number;
  nutritionistName: string;
  nutritionistEmail: string;
  status: LinkStatus;
  invitedAt: string;
  acceptedAt?: string;
}

export interface PendingInvite {
  linkId: number;
  token: string;
  nutritionistId: number;
  nutritionistName: string;
  nutritionistEmail: string;
  invitedAt: string;
  expiresAt: string;
}
export type AuthProvider = 'LOCAL' | 'GOOGLE';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  userId: number;
  email: string;
  name: string;
  role: Role;
  profileCompleted: boolean;
}

export interface CurrentUser {
  userId: number;
  email: string;
  name: string;
  role: Role;
  profileCompleted: boolean;
  token: string;
  refreshToken: string;
}

export interface User {
  id?: number;
  email: string;
  /** CPF já mascarado ("123.***.***-45"). API nunca retorna CPF completo. */
  cpf?: string;
  /** CRN (nutricionista). */
  crn?: string;
  name: string;
  sex?: Sex;
  birthDate?: string;
  weightKg?: number;
  heightCm?: number;
  activityLevel?: ActivityLevel;
  goal?: Goal;
  preferences?: string;
  medications?: string;
  hasBariatricSurgery?: boolean;
  preExistingConditions?: string;
  allergies?: string;
}

export interface HealthProfile {
  sex: Sex;
  birthDate: string;
  weightKg: number;
  heightCm: number;
  activityLevel: ActivityLevel;
  goal: Goal;
  preferences?: string;
  medications?: string;
  hasBariatricSurgery?: boolean;
  preExistingConditions?: string;
  allergies?: string;
}

export interface AdminUser {
  id: number;
  email: string;
  cpf?: string;
  crn?: string;
  name: string;
  role: Role;
  provider: AuthProvider;
  profileCompleted: boolean;
  active: boolean;
}

export interface Food {
  id?: number | null;
  name: string;
  category?: string;
  caloriesPer100g: number;
  proteinPer100g: number;
  carbsPer100g: number;
  fatPer100g: number;
  aiSuggested?: boolean;
}

export interface MealItem {
  id?: number;
  foodId: number;
  foodName?: string;
  grams: number;
  calories?: number;
  protein?: number;
  carbs?: number;
  fat?: number;
}

export interface Meal {
  id?: number;
  userId?: number;
  date: string;
  time?: string;
  type: MealType;
  items: MealItem[];
  totalCalories?: number;
  totalProtein?: number;
  totalCarbs?: number;
  totalFat?: number;
}

export interface DailySummary {
  date: string;
  calorieGoal: number;
  proteinGoal: number;
  caloriesConsumed: number;
  proteinConsumed: number;
  carbsConsumed: number;
  fatConsumed: number;
  calorieRemaining: number;
  proteinRemaining: number;
  meals: Meal[];
}

export interface ProgressEntry {
  id?: number;
  date: string;
  weightKg: number;
  bodyFatPercent?: number;
  waistCm?: number;
  hipCm?: number;
  armCm?: number;
  chestCm?: number;
  note?: string;
}

export interface ProgressSummary {
  currentWeight: number | null;
  startingWeight: number | null;
  weightDelta: number | null;
  lastEntryDate: string | null;
  entriesCount: number;
  series: { date: string; weightKg: number }[];
}

export type MenuPlanSource = 'MANUAL' | 'AI';
export type MenuPlanStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface MenuPlan {
  id?: number;
  name: string;
  source: MenuPlanSource;
  content: string;
  startDate?: string;
  endDate?: string;
  active: boolean;
  createdAt?: string;
  status?: MenuPlanStatus;
  submittedToNutriId?: number | null;
  submittedAt?: string | null;
  reviewedAt?: string | null;
  reviewNote?: string | null;
  ownerUserId?: number | null;
  ownerName?: string | null;
  ownerEmail?: string | null;
}

// Appointments (Fase 8)
export type AppointmentStatus = 'REQUESTED' | 'CONFIRMED' | 'RESCHEDULE_REQUESTED' | 'CANCELED' | 'COMPLETED' | 'NO_SHOW';
export type AppointmentLocation = 'ONLINE' | 'IN_PERSON';
export type AppointmentActor = 'CLIENT' | 'NUTRITIONIST';

export interface Appointment {
  id: number;
  clientId: number;
  clientName: string;
  clientEmail: string;
  nutritionistId: number;
  nutritionistName: string;
  practiceId?: number | null;
  companyId?: number | null;
  companyName?: string | null;
  scheduledAt: string;
  durationMinutes: number;
  location: AppointmentLocation;
  locationDetails?: string | null;
  notes?: string | null;
  status: AppointmentStatus;
  requestedBy: AppointmentActor;
  lastActionBy: AppointmentActor;
  proposedScheduledAt?: string | null;
  proposedBy?: AppointmentActor | null;
  cancelReason?: string | null;
  canceledAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentRequest {
  nutritionistId?: number | null;
  clientId?: number | null;
  practiceId?: number | null;
  scheduledAt: string;
  durationMinutes?: number | null;
  location?: AppointmentLocation;
  locationDetails?: string | null;
  notes?: string | null;
}

export type NotificationType = 'MENU_SUBMITTED' | 'MENU_APPROVED' | 'MENU_REJECTED' | 'GENERIC';

export interface AppNotification {
  id: number;
  type: NotificationType;
  title: string;
  body: string | null;
  entityType: string | null;
  entityId: number | null;
  readAt: string | null;
  createdAt: string;
}

export interface NutriPublic {
  id: number;              // practiceId (Fase 5B)
  nutritionistId: number;  // userId do nutri
  name: string;
  bio: string | null;
  specialties: string | null;
  companyId?: number | null;
  companyName?: string | null;
}

export interface CompanyClient {
  linkId: number;
  clientId: number;
  clientName: string;
  clientEmail: string;
  clientCpf?: string;
  nutriId: number;
  nutriName: string;
  status: LinkStatus;
  lastMealDate: string | null;
}

export interface NutriProfileMe {
  acceptsNewClients: boolean;
  bio: string | null;
  specialties: string | null;
}

export interface CompanyMetricsOverview {
  nutritionistsActive: number;
  nutritionistsPending: number;
  clientsTotal: number;
  aiSuggestionsMonth: number;
  mealsLoggedLast30d: number;
  activeSubscriptionCents: number;
  currency: string;
  nutritionists: CompanyNutriRow[];
}

export interface CompanyNutriRow {
  nutriId: number;
  name: string;
  email: string;
  activeClients: number;
  aiSuggestionsMonth: number;
  lastActivityDate: string | null;
}

export interface NutriMetricsOverview {
  activeClients: number;
  aiSuggestionsMonth: number;
  aiSuggestionsLimit: number;   // -1 = unlimited
  menusActive: number;
  avgAdherence7d: number;       // 0..1
  clientsNeedingAttention: number;
  clients: NutriClientRow[];
}

export interface NutriClientRow {
  clientId: number;
  name: string;
  email: string;
  adherence7d: number;
  lastMealDate: string | null;
  needsAttention: boolean;
  hasActivePlan: boolean;
}

export interface AppAlert {
  id: number;
  scope: 'ADMIN' | 'COMPANY' | 'NUTRITIONIST';
  scopeId: number | null;
  severity: 'INFO' | 'WARN' | 'CRITICAL';
  kind: string;
  title: string;
  message: string | null;
  metadata: string | null;
  createdAt: string;
  dismissedAt: string | null;
}

export interface AdminMetricsOverview {
  totalUsers: number;
  totalNutritionists: number;
  totalCompanies: number;
  activeSubscriptions: number;
  dau: number;
  wau: number;
  mau: number;
  newUsers30d: number;
  mrrBrlCents: number;
  mrrByCurrency: { [currency: string]: number };
  planDistribution: { [planCode: string]: number };
  suggestionsLast30d: { action: string; count: number }[];
  topCompanies: { companyId: number; name: string; amountCents: number; currency: string }[];
  topNutris: { nutriId: number; name: string; activeClients: number }[];
}
