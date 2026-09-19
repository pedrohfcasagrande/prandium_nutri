# Nutri Tracker

Plataforma multi-tenant de acompanhamento nutricional. B2B2C: empresas contratam, nutricionistas atendem clientes, clientes registram refeicoes e recebem sugestoes de cardapio via IA.

## Stack

- **Backend:** Java 21 + Spring Boot 3.3.4 (Maven) + Spring Security (JWT) + Flyway 10
- **Frontend:** Angular 18 + Angular Material (M3) + ng2-charts + ngx-translate v16 (PT/EN/FR), tema claro/escuro/automatico
- **Banco:** MySQL 8 (dev via docker na porta 3307). H2 in-memory nos testes.
- **IA:** Google Gemini (`gemini-flash-lite-latest`) para sugestoes de cardapio
- **SSO:** Google Sign-In (Google Identity Services)
- **Pagamentos:** Stripe (USD/EUR) + Mercado Pago (BRL) — abstraidos via `PaymentProviderClient`. Mock em `dev`/`test`.

## Arquitetura multi-tenant

Hierarquia de tenants e permissoes:

```
ADMIN (plataforma)
  |__ COMPANY (empresa cliente)
        |__ CompanyMember (multi-admin da empresa)
        |__ NUTRITIONIST (vinculada a company)
              |__ USER / CLIENT (vinculado ao nutri via NutritionistClientLink)
```

Permissoes via header `X-Impersonate-User-Id` (impersonation transitiva):
- **ADMIN** impersona qualquer usuario
- **COMPANY** impersona seus nutricionistas E os clientes desses nutris (transitivo)
- **NUTRITIONIST** impersona apenas seus clientes ativos
- **USER** so ve os proprios dados

Ver `security/CurrentUser.java` para a logica completa.

## Estrutura

```
nutri-tracker/
├── backend/                            Spring Boot API
│   ├── src/main/resources/
│   │   ├── application.yml             4 perfis: dev / sandbox / prod / test
│   │   └── db/migration/               Flyway V001..V010
│   └── src/test/java/                  JUnit 5 + Spring Boot Test + AssertJ
├── frontend/                           Angular 18 SPA
├── docker-compose.dev.yml              MySQL somente (dev, porta 3307)
├── docker-compose.yml                  MySQL + backend + frontend (prod local)
├── docs/                               Docs de fase (2, 3, 4)
└── README.md
```

## Perfis (`SPRING_PROFILES_ACTIVE`)

O `application.yml` define 4 perfis:

| Perfil    | Banco                              | Pagamentos               | Email       | Uso                          |
|-----------|------------------------------------|--------------------------|-------------|------------------------------|
| `dev`     | MySQL local (docker `3307`)        | **MOCK** (subscription ativa na hora) | Console log | Desenvolvimento local        |
| `sandbox` | MySQL sandbox (env `DB_*`)         | Reais em modo **teste** (Stripe test keys, MP test) | SMTP real   | QA / demo                    |
| `prod`    | MySQL prod (SSL)                   | Reais em modo **live**   | SMTP prod   | Producao                     |
| `test`    | H2 in-memory (`create-drop`, sem Flyway) | Mock (via `DevPaymentConfig`) | Console log | Suite JUnit                  |

Selecao do payment client por perfil:
- `dev` + `test`: `DevPaymentConfig` registra 2 beans `MockPaymentClient` (STRIPE, MERCADO_PAGO)
- `sandbox` + `prod`: `StripePaymentClient` e `MercadoPagoPaymentClient` sao ativados via `@Profile({"sandbox","prod"})`

## Rodando em dev

### 1. MySQL local
```bash
docker compose -f docker-compose.dev.yml up -d
```
Container `nutri-mysql-dev` na porta **3307**. Usuario `nutri` / senha `nutri` / banco `nutritracker`.

### 2. Backend
```bash
cd backend
export GOOGLE_CLIENT_ID=seu-google-client-id
export GEMINI_API_KEY=sua-chave-gemini    # opcional (sem ela, sugestoes retornam offline)
mvn spring-boot:run
```
- Perfil `dev` e o default. Flyway aplica migrations no startup.
- API em `http://localhost:8080/api`
- Admin seed: `admin@admin.com` / `admin@123` (override: `ADMIN_EMAIL` / `ADMIN_PASSWORD`)
- Pagamentos **mockados**: `createCheckout` ativa a subscription imediatamente e retorna uma URL `?mock=true`

### 3. Frontend
```bash
cd frontend
npm install
npm start
```
Abre em `http://localhost:4200`.

## Rodando em sandbox / prod

```bash
export SPRING_PROFILES_ACTIVE=sandbox   # ou prod
export DB_HOST=... DB_USER=... DB_PASS=... DB_NAME=...
export STRIPE_SECRET_KEY=sk_test_...      # sk_live_... em prod
export STRIPE_WEBHOOK_SECRET=whsec_...
export MP_ACCESS_TOKEN=TEST-...           # APP-USR-... em prod
export MP_WEBHOOK_SECRET=...
export MAIL_ENABLED=true
export SPRING_MAIL_HOST=smtp.provider.com SPRING_MAIL_USERNAME=... SPRING_MAIL_PASSWORD=...
export GEMINI_API_KEY=...
export GOOGLE_CLIENT_ID=...
export JWT_SECRET=... # >= 32 chars, valor forte
mvn spring-boot:run
```

Webhooks locais (test):
- Stripe: `stripe listen --forward-to localhost:8080/api/webhooks/stripe`
- MP: `ngrok http 8080` + configurar a URL publica no dashboard MP

## Testes

Backend (52 testes atualmente):
```bash
cd backend
mvn test
```

Frontend (Jest + jest-preset-angular; 30 testes atualmente):
```bash
cd frontend
npm test
```

Cobertura frontend:
- `services/api.service.spec.ts` — envio de `scope` em `suggest()`, params de pricing, endpoint admin/metrics
- `pages/suggestions/suggestions.component.spec.ts` — parser markdown -> sections, agrupamento por dia (`groupByDay` — inclui deteccao de `**bold**` e H3), `sectionIcon`, scope default
- `pages/pricing/pricing.component.spec.ts` — `onTargetChange` dispara/no-op, guard `reqSeq` descarta respostas obsoletas
- `pages/menus/menus.component.spec.ts` — `activePlan`/`otherPlans` computeds, `heroPreview` (6 linhas), `isTruncated`
- `pages/dashboard/dashboard.component.spec.ts` — `todaysMenuPreview` extrai dia atual do cardapio semanal
- `components/alert-banner.component.spec.ts` — load do endpoint por scope, dismiss remove local + POST
- `pages/company-clients/company-clients.component.spec.ts` — activeNutris filtra ACTIVE, query filtra, transfer PUT correto, no-op quando target igual

Cobertura backend:
- `NutriTrackerApplicationTests` — context load smoke test
- `service/SubscriptionServiceTest` — createFree, ensureSubscription, incrementUsage
- `service/AuthServiceTest` — register cria FREE subscription auto (USER e NUTRI)
- `service/FeatureGateServiceTest` — limite `aiSuggestionsMonthly`, plano unlimited
- `service/NutritionistLinkServiceTest` — invite dentro do limite / `maxClients` estourado
- `payment/MockPaymentClientTest` — mock ativo em `test`, ativa subscription instantaneamente
- `service/AuditServiceTest` — grava LOGIN async, count by action
- `service/AdminMetricsServiceTest` — overview base + reflete nova subscription
- `service/FxRateServiceTest` — fallback em memoria, ultima cotacao vence, conversao de cents
- `service/AlertServiceTest` — `PAYMENT_PAST_DUE` gera alerta ADMIN, dismiss marca timestamp
- `service/CompanyMetricsServiceTest` — company vazia zera contadores, conta nutris ativos + clientes
- `service/NutriMetricsServiceTest` — adesao calculada de meals distintos, flag `needsAttention` < 30%
- `service/MealServiceTest` — save novo dispara audit `MEAL_LOGGED` async
- `service/NotificationServiceTest` — notify grava + conta unread; markRead nao afeta outros users; markAllRead
- `service/MenuPlanApprovalTest` — submit vira PENDING e notifica; approve/reject notificam cliente; nutri diferente e negado; PENDING nao pode ativar
- `service/NutriDiscoveryFlowTest` — request-link falha sem opt-in; opt-in cria PENDING e notifica; accept/reject fecham fluxo com notificacao
- `service/CompanyClientTransferTest` — revoke com REVOKE_CLIENTS encerra client links; TRANSFER move para nutri alvo; alvo fora da empresa negado; transferClient move entre nutris; listClients consolida

Fixtures: `test/support/PlanTestFixtures.java` popula os planos manualmente porque o perfil `test` usa H2 sem Flyway.

**Regra:** ao adicionar/alterar servico, crie ou atualize o teste correspondente e rode `mvn test` antes de commitar.

## Migrations (Flyway)

Em `backend/src/main/resources/db/migration/`. Nomenclatura `V<n>__<descricao>.sql`.

| Migration | Conteudo                                                             |
|-----------|----------------------------------------------------------------------|
| V001      | users, foods, meals, meal_items                                      |
| V002      | seed de alimentos base                                               |
| V003      | perfil de saude no user, tabela `progress_entries`                   |
| V004      | password reset tokens                                                |
| V005      | menu_plans (cardapios salvos)                                        |
| V006      | nutritionist_client_links (convites nutri -> cliente)                |
| V007      | companies (multi-tenant B2B)                                         |
| V008      | company_members (multi-admin), company_nutritionists                 |
| V009      | plans, plan_prices (multi-currency), subscriptions, usage_counters   |
| V010      | webhook_events (idempotencia Stripe/MP)                              |
| V011      | users.created_at/last_seen_at + audit_log (Fase 4A)                  |
| V012      | daily_fx_rates (cotacoes por moeda, seed BRL/USD/EUR)                |
| V013      | alerts (scope, kind, severity, dismissed_at)                         |
| V014      | menu_plans + status/submitted_at/reviewed_at + notifications table   |
| V015      | users: accepts_new_clients/bio/specialties + link.initiated_by_client |

JPA em `ddl-auto: validate` (dev/sandbox/prod): drift entre entity e schema quebra o startup.

## Planos e feature gating

Planos seed: `NUTRI_FREE`, `NUTRI_PRO`, `CLIENT_FREE`, `CLIENT_PLUS`, `COMP_STARTER`. Precos por moeda em `plan_prices` (BRL/USD/EUR).

Gates aplicados via `FeatureGateService`:
- `maxClients` (nutri) — bloqueia `NutritionistLinkService.invite()` com `PlanLimitExceededException`
- `aiSuggestionsMonthly` (user/nutri) — retorna HTTP 402 quando estourado
- `menuLimit` (nutri) — limite de cardapios salvos

Provider por moeda:
- BRL -> Mercado Pago
- USD, EUR -> Stripe

## API REST (resumo)

**Auth** — `/api/auth/**` publico
- `POST /register` — cadastro (cria FREE subscription auto)
- `POST /login` — email + senha
- `POST /oauth/google` — ID token Google
- `POST /change-password`, `POST /forgot-password`, `POST /reset-password`

**User** — `/api/users/**`
- `GET  /me`, `PUT /me/health-profile`

**Meals / Foods / Dashboard**
- `GET  /api/foods?q=...`
- `POST /api/meals`, `GET /api/meals?date=YYYY-MM-DD`
- `GET  /api/dashboard?date=YYYY-MM-DD`

**Progress**
- `GET/POST/DELETE /api/progress`
- `GET  /api/progress/summary?days=30`

**Suggestions (Gemini)**
- `POST /api/suggestions` `{ extraNotes }` (sujeito a `aiSuggestionsMonthly`)

**Menu Plans**
- `GET/POST/DELETE /api/menu-plans`

**Nutritionist**
- `POST /api/nutritionist/invite` — envia convite (sujeito a `maxClients`)
- `GET  /api/nutritionist/clients`

**Company**
- `GET  /api/company/nutritionists`, `POST /api/company/nutritionists/invite`
- `GET/POST /api/company/members`

**Pricing / Billing** — `/api/pricing/**` publico, `/api/billing/**` autenticado
- `GET  /api/pricing/plans?target=NUTRITIONIST&currency=BRL`
- `POST /api/billing/checkout` — cria checkout Stripe/MP (ou mock em `dev`)
- `GET  /api/billing/subscription`

**Webhooks** — `/api/webhooks/**` publico
- `POST /api/webhooks/stripe`
- `POST /api/webhooks/mercadopago`

**Onboarding** (sales-led company) — `/api/onboarding/**` publico
- `POST /api/onboarding/accept` `{ token, password }`

**Admin** — `/api/admin/**` role ADMIN
- `GET/PUT/DELETE /api/admin/users`
- `POST /api/admin/companies` (cria empresa + owner + envia token)
- `GET /api/admin/metrics/overview` — MRR consolidado BRL (via `daily_fx_rates`), MAU/WAU/DAU, plan distribution, top empresas/nutris, contagem por audit action

**Company metrics** — `/api/company/metrics/**` role COMPANY
- `GET /overview` — KPIs (nutris ativos/pendentes, clientes, IA mensal, refeicoes 30d, MRR da assinatura) + tabela agregada por nutri (sem PII de cliente)

**Nutritionist metrics** — `/api/nutritionist/metrics/**` role NUTRITIONIST
- `GET /overview` — KPIs (clientes ativos, sugestoes IA/mes com limite do plano, cardapios ativos, adesao media 7d) + tabela por cliente com adesao (% de dias com refeicao nos ultimos 7d), flag `needsAttention` quando < 30%

**Alerts** — `/api/alerts/**`
- `GET /admin` (ADMIN), `GET /company` (COMPANY), `GET /nutritionist` (NUTRITIONIST)
- `POST /{id}/dismiss` — marca visto
- `POST /evaluate-now` (ADMIN) — dispara `AlertService.evaluateAllRules()` manualmente
- Job `@Scheduled(cron="0 0 3 * * *")` avalia regras diariamente: `PAYMENT_PAST_DUE`, `CLIENT_INACTIVE_7D`
- Dedup: nao cria alerta do mesmo `kind`+`scopeId` se houver um nas ultimas 24h

**Menu approval (Fase 4C)** — cardapio com aprovacao do nutri
- `POST /api/menu-plans/{id}/submit` — cliente envia cardapio para aprovacao do nutri vinculado (status vai DRAFT/APPROVED -> PENDING, notifica nutri via in-app + email)
- `GET  /api/nutritionist/menu-plans/pending` (NUTRITIONIST)
- `POST /api/nutritionist/menu-plans/{id}/approve` (NUTRITIONIST) — notifica cliente
- `POST /api/nutritionist/menu-plans/{id}/reject` (NUTRITIONIST) — body `{ reason }`, notifica cliente com motivo
- Restricao: se `MenuPlan.status = PENDING`, `setActive(true)` bloqueia (so ativa apos APPROVED)

**Nutri discovery (Fase 4D)** — cliente encontra e pede vinculo a um nutri
- `GET  /api/nutritionists/available` (autenticado) — lista publica de nutris com `acceptsNewClients=true`
- `GET  /api/nutritionists/{id}` — perfil publico (id, name, bio, specialties)
- `PUT  /api/nutritionists/me/profile` (NUTRITIONIST) — atualiza opt-in + bio + especialidades
- `POST /api/nutritionists/{id}/request-link` — cliente envia pedido com `{ message }` opcional. Cria `NutritionistClientLink` com `initiatedByClient=true`, `status=PENDING`, notifica nutri.
- `GET  /api/nutritionist/client-requests` (NUTRITIONIST) — lista pedidos pendentes iniciados por clientes
- `POST /api/nutritionist/client-requests/{id}/accept` — vira ACTIVE, notifica cliente
- `POST /api/nutritionist/client-requests/{id}/reject` — vira REJECTED com `note`, notifica cliente

**Notifications (Fase 4C)** — `/api/notifications/**`
- `GET /` (query `unreadOnly=true`, `limit`) — lista notificacoes do user autenticado
- `GET /unread-count` — contador para o sino do toolbar
- `POST /{id}/read`, `POST /read-all`
- `NotificationService.notify(...)` grava linha in-app + envia email via `MailService` (log em dev)
- Frontend faz polling de 30s via `NotificationPollService`

Impersonation: qualquer request pode carregar `X-Impersonate-User-Id: <id>`. Validacao em `CurrentUser`.

## Variaveis de ambiente

Ver `.env.example`. Principais:

| Var                        | Uso                                                     |
|----------------------------|---------------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`   | `dev` \| `sandbox` \| `prod` \| `test`                  |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASS`, `DB_NAME` | MySQL (dev default `localhost:3307`) |
| `GOOGLE_CLIENT_ID`         | OAuth Google                                            |
| `GEMINI_API_KEY`           | Sugestoes IA                                            |
| `JWT_SECRET`               | Segredo JWT (>= 32 chars)                               |
| `JWT_EXPIRATION_MS`        | Default `86400000` (24h)                                |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Override admin seed                                |
| `MAIL_ENABLED`             | `false` em dev; `true` em sandbox/prod                  |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe (ignorados em dev)             |
| `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET` | Mercado Pago (ignorados em dev)             |
| `CHECKOUT_SUCCESS_URL`, `CHECKOUT_CANCEL_URL` | URLs de retorno do checkout           |
| `CORS_ALLOWED_ORIGINS`     | Origens permitidas                                      |
| `FRONTEND_URL`             | Usado em emails (reset, convites)                       |

## Convencoes

- Ao alterar codigo, atualize migrations + testes + este README (mesma PR).
- `ddl-auto: validate` protege contra drift entity/schema.
- Novo endpoint que consome cota → adicionar guard via `FeatureGateService` e teste em `service/FeatureGateServiceTest` (ou test dedicado).
- Novo servico → criar `<Servico>Test` em `backend/src/test/java/com/nutritracker/service/` usando `PlanTestFixtures.seedAll()` no `@BeforeEach`.
- i18n: qualquer string exibida no frontend deve ter chave em `frontend/src/assets/i18n/{pt,en,fr}.json`.

## Observabilidade (Fases 4A + 4B)

**Audit log** (`audit_log`): trilha de acoes escrita de forma assincrona via `AuditService.record(...)` (thread pool `audit-*`, `@Async`).
Acoes instrumentadas: `LOGIN`, `REGISTER`, `MEAL_LOGGED`, `MEAL_DELETED`, `SUGGESTION_GENERATED`, `CLIENT_INVITED`, `CLIENT_ACCEPTED`, `SUBSCRIPTION_CREATED`. Novos endpoints devem adicionar `auditService.record(...)` na acao relevante e teste em `AuditServiceTest`.

**Presence tracking**: `JwtAuthFilter` atualiza `users.last_seen_at` no maximo 1x a cada 5min por user (throttle em-memoria). Base para metricas DAU/WAU/MAU.

**MRR consolidado (Fase 4B)**: `AdminMetricsService` usa `FxRateService` que le a cotacao mais recente por moeda da tabela `daily_fx_rates`. Fallback fixo em memoria caso a moeda nao tenha registro. Ainda nao ha job de atualizacao automatica das cotacoes — seed inicial via migration; alterar via `INSERT` manual ou UI (backlog).

**Alerts (Fase 4B)**: `AlertService` avalia regras diariamente via `@Scheduled` e insere linhas em `alerts`. Cada dashboard renderiza `AlertBannerComponent` (info/warn/critical, dismissable). Novo tipo de alerta = adicionar metodo `evaluateXxx()` em `AlertService.evaluateAllRules()`.

**UI**:
- `/admin/metrics` (ADMIN) — KPI cards + charts (MRR/moeda, plans) + tabelas + banner de alertas ADMIN
- `/company/metrics` (COMPANY) — KPI cards + tabela agregada de nutris + banner COMPANY
- `/nutritionist/metrics` (NUTRITIONIST) — KPI cards + tabela clientes com progress bar de adesao + destaque "precisa atencao" + banner NUTRITIONIST

## Roadmap

- Fase 2 (multi-tenant): OK
- Fase 3A (planos + subscriptions): OK
- Fase 3B (webhooks + billing UI): OK
- **Fase 4A (audit log + admin metrics dashboard): OK**
- **Fase 4B (Company + Nutritionist dashboards, MRR via `daily_fx_rates`, alertas automaticos): OK**
- **Fase 4C (notificacoes in-app + email + fluxo de aprovacao de cardapio pelo nutri): OK**
- **Fase 4D (discovery de nutricionista + pedido de vinculo iniciado pelo cliente): OK**
- **Fase 4E (empresa gerencia clientes: revoke com transferencia, reenvio de convite, pagina consolidada `/company/clients`): OK**
- Fase 5 (geolocalizacao restaurantes): backlog
