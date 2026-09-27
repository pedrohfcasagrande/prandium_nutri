# Prandium Nutri

Plataforma multi-tenant de acompanhamento nutricional. B2B2C: empresas contratam, nutricionistas atendem clientes, clientes registram refeicoes e recebem sugestoes de cardapio via IA.

> **Nota de marca:** o repositorio ainda vive na pasta `nutri-tracker/` (nome legado) e o banco chama-se `nutritracker` — foram mantidos para evitar migracao de dados e rename de remote git. Package Java, artifactId Maven, name do Angular/npm e todas as strings visiveis usam **prandiumnutri / prandium-nutri**. Logos em `frontend/src/assets/brand/`.

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
prandium-nutri/
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

### 2.1. Seed de desenvolvimento (Fase 7A)

Se o banco estiver vazio (`users.count() < 3`), o `DevDataSeeder` (`@Profile("dev")`) popula automaticamente no startup:
- **2 empresas:** NutriPro Saude Ltda (12345678000190) e Vida Ativa Consultoria (98765432000110)
- **4 nutris:** Dra. Camila Rocha (NutriPro), Dr. Bruno Almeida (NutriPro), Dra. Larissa Ferreira (Vida Ativa) e Dra. Julia Mendes (independente) — cada um com CRN preenchido
- **10 clientes** com nomes de exemplo (`joao@cliente.com`, `maria@cliente.com`, etc.)
- Vinculos empresa-nutri e nutri-cliente prontos (2 clientes por nutri; ultimos 2 clientes sem vinculo pra testar discovery)
- Subscriptions FREE por default; clientes vinculados viram PLUS via sponsorship se o pagador estiver em tier pago (nao ocorre no seed default porque todos comecam FREE)

**Senha unificada de todos os usuarios seedados: `admin@123`** — trocar antes de subir pra sandbox/prod.

Se voce ja tem dados no banco, o seed nao roda (idempotencia via `count()`).

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
# opcional: timeouts das chamadas HTTP outbound (defaults 10s/30s)
export HTTP_CONNECT_TIMEOUT_SECONDS=10
export HTTP_READ_TIMEOUT_SECONDS=30
mvn spring-boot:run
```

Webhooks locais (test):
- Stripe: `stripe listen --forward-to localhost:8080/api/webhooks/stripe`
- MP: `ngrok http 8080` + configurar a URL publica no dashboard MP

Guias detalhados de setup e teste dos providers:
- [`docs/PAGAMENTOS-SETUP.md`](docs/PAGAMENTOS-SETUP.md) — env vars, eventos a assinar, troubleshooting
- [`docs/STRIPE-DRY-RUN.md`](docs/STRIPE-DRY-RUN.md) — runbook E2E via `stripe-cli` cobrindo golden path, race condition, PAST_DUE→ACTIVE, cancel, idempotência

## CI/CD

GitHub Actions em `.github/workflows/ci.yml`. Roda em push/PR nas branches `main`/`master`/`develop`, com concurrency cancel:
- **Backend**: `setup-java 21 (temurin)` → `mvn test` → `mvn -DskipTests package` (sanity build) → upload de surefire-reports em failure
- **Frontend**: `setup-node 20` (cache npm) → `npm ci` → `npm test -- --ci` → `npm run build --configuration production` (valida budgets 1MB/2MB)

Os dois jobs rodam em paralelo, sem depender um do outro.

## Testes

Backend (94 testes atualmente):
```bash
cd backend
mvn test
```

Frontend (Jest + jest-preset-angular; 60 testes atualmente):
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
- `pages/nutri-company-invites/nutri-company-invites.component.spec.ts` — carrega convites de empresa, accept/reject usam token correto e recarregam lista
- `pages/meals/meals.component.spec.ts` — fallback IA dispara so quando busca local retorna vazio (debounce 500ms); addItem persiste alimento IA antes de anexar ao meal
- `pages/nutri-dashboard/nutri-dashboard.component.spec.ts` — forkJoin de metrics + pending menus + client requests + subscription; loading vai a false mesmo em erro parcial
- `pages/nutri-practices/nutri-practices.component.spec.ts` — carrega listagem de practices; save envia PUT com bio/specialties/acceptsNewClients
- `pages/billing/billing.component.spec.ts` — estado vazio, plan card com nome/preço/tier class, feature rows, cancel button flow com timers, cancel button oculto quando provider MANUAL
- `pages/admin-metrics/admin-metrics.component.spec.ts` — skeletons enquanto carrega, cards KPI com valores formatados, tabelas top companies/nutris, resiliência a erro da API (usa `overrideComponent` + `NO_ERRORS_SCHEMA` pra evitar crash de Chart.js em jsdom)
- `pages/nutri-menu-plans/nutri-menu-plans.component.spec.ts` — ngOnInit dispara pending + clients GET, estado vazio, cards de menus pendentes, approve() → POST /approve + reload
- `pages/pricing/pricing.component.spec.ts` — (+) renderiza um `.plan-card` por plano, plano free mostra "billing.free" e pago mostra amount formatado
- `pages/meals/meals.component.spec.ts` — (+) estado vazio com `.empty`, um `.meal-card` por refeição com tier class + totais

Cobertura backend:
- `PrandiumNutriApplicationTests` — context load smoke test
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
- `service/CompanyClientTransferTest` — revoke com REVOKE_CLIENTS encerra client links; TRANSFER move para nutri alvo; alvo fora da empresa negado; transferClient move entre nutris; listClients consolida; TRANSFER notifica nutri revogado + clientes + nutri destino; inviteNutri notifica nutri existente
- `service/NutritionistLinkServiceTest` — invite dentro/fora do limite; `listClientsForNutri` expoe `subscriptionStatus`/`subscriptionPlanCode` do cliente vinculado
- `service/NutritionistPracticeServiceTest` — ensureParticular e idempotente e copia bio; ensureForCompany cria/reativa practice; suspendForCompany muda status; nutri pode ter particular + varias vinculadas (particular primeiro na listagem); availablePractices filtra por ACTIVE + opt-in; endLinkWithCompany converte em particular quando nao ha; apenas suspende quando ha
- `service/SponsorshipTest` — aceitar convite ativa CLIENT_PLUS patrocinado; revogar ultimo vinculo volta a CLIENT_FREE; revogar com outro vinculo ativo transfere sponsor pra outra practice
- `service/FoodAiLookupServiceTest` — parser aceita JSON valido, remove cercas markdown, filtra valores fora de faixa (kcal 0-900, macros 0-100), retorna vazio em lixo/nome vazio
- `service/PaymentWebhookServiceTest` — dedup por eventId (mesmo evento 2x = 1 linha), checkout.session.completed cria ACTIVE sub com plan/currency/amount, race (customer.subscription.created ANTES de checkout também cria via metadata), invoice.payment_succeeded flip PAST_DUE→ACTIVE avança period_end, customer.subscription.deleted marca CANCELED
- `payment/MercadoPagoSignatureVerifierTest` — fail-open sem secret configurado, rejeita sem header/dataId, rejeita assinatura errada, aceita HMAC-SHA256 válida (com e sem x-request-id)

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
| V016      | feature flag findNutritionist em CLIENT_PLUS                          |
| V017      | nutritionist_practices (Fase 5A) + backfill de particular/vinculadas  |
| V018      | nutritionist_clients.practice_id + subscriptions.sponsored_by_practice_id (Fase 5C) |
| V019      | backfill retroativo de sponsorship — clientes com NCL ACTIVE viram CLIENT_PLUS |
| V020      | rebalanceia sponsorship: cliente so fica PLUS se pagador (empresa/nutri) tem tier pago |
| V021      | users.cpf NOT NULL UNIQUE + users.crn UNIQUE (Fase 6A) — backfill sequencial `LPAD(id,11,'0')` |
| V022      | refresh_tokens                                                        |
| V023      | appointments (agenda de consultas)                                    |
| V024      | client_assessments (ficha clinica base — 1 por par nutri↔cliente)     |
| V025      | client_assessments.labs (exames) + assessment_snapshots (historico versionado — Fase 9A) |
| V026      | client_assessments: 7 dobras cutaneas (JP) + 5 perimetros faltantes + skinfold_protocol (Fase 9B) |

JPA em `ddl-auto: validate` (dev/sandbox/prod): drift entre entity e schema quebra o startup.

## Planos e feature gating

Planos seed: `NUTRI_FREE`, `NUTRI_PRO`, `CLIENT_FREE`, `CLIENT_PLUS`, `COMP_STARTER`. Precos por moeda em `plan_prices` (BRL/USD/EUR).

Gates aplicados via `FeatureGateService`:
- `maxClients` (nutri) — bloqueia `NutritionistLinkService.invite()` com `PlanLimitExceededException`
- `aiSuggestionsMonthly` (user/nutri) — retorna HTTP 402 quando estourado
- `menuLimit` (nutri) — limite de cardapios salvos

**PLUS automatico (Fase 5C + 5G):** quando um cliente aceita convite/pedido de nutri, `SubscriptionService.sponsorClientToPlus` promove a subscription dele para `CLIENT_PLUS` **somente se o pagador da practice (empresa ou nutri) tem plano com tier != FREE**. Grava `sponsored_by_practice_id` apontando pra practice. Regras:
1. Practice vinculada a empresa → pagador = subscription da empresa. Se COMP_STARTER (ou superior), concede PLUS. Se empresa em FREE (edge case), nao concede.
2. Practice particular → pagador = subscription pessoal do nutri. Se NUTRI_PRO, concede PLUS. Se NUTRI_FREE, cliente permanece em FREE.
3. Ao revogar vinculo, `revokeSponsorship` tenta transferir sponsor pra outra practice ativa do cliente (mantem PLUS). Sem outra → volta a FREE.
4. Cliente com subscription paga propria (nao sponsored) — nao mexe em nada.
5. Migration `V020__rebalance_sponsorship_by_tier.sql` corrige o estado historico (V019 promoveu clientes cujo sponsor era FREE).

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
- `GET  /api/foods?q=...` — busca no catalogo local
- `GET  /api/foods/ai-lookup?q=...` — fallback IA (Gemini) para o que nao esta no banco; retorna itens com `id=null` e `aiSuggested=true`, sem persistir. O frontend so dispara este endpoint quando a busca local retorna vazio e a query tem >=3 caracteres (debounce 500ms). Ao adicionar um item IA ao meal, o frontend faz `POST /api/foods` (com dedupe por nome case-insensitive) antes de anexar ao `MealItem`.
- `POST /api/foods` — cria/dedupe alimento no catalogo
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
- `GET  /api/nutritionist/clients` — cada `ClientLinkDto` inclui `subscriptionStatus`/`subscriptionPlanCode`/`subscriptionPlanName`/`subscriptionRenewsAt` do cliente vinculado, populados a partir de `SubscriptionService.effectiveOf(USER, clientId)`. O frontend renderiza uma pill colorida (verde=ACTIVE/TRIALING, vermelho=PAST_DUE, cinza=CANCELED/SUSPENDED) ao lado do vinculo.

**Company**
- `GET  /api/company/nutritionists`, `POST /api/company/nutritionists/invite`
- `POST /api/company/nutritionists/{linkId}/revoke` body `{action, targetNutriId?}` — action `REVOKE_CLIENTS` encerra tambem os vinculos nutri-cliente; `TRANSFER` move os clientes ativos para o nutri alvo (que deve ser outro nutri ativo da mesma empresa). Notifica nutri revogado, clientes afetados e nutri destino via in-app + email.
- `GET  /api/company/clients` — lista consolidada de clientes atendidos por qualquer nutri da empresa
- `PUT  /api/company/clients/{clientId}/nutri` body `{targetNutriId}` — transfere um cliente especifico entre nutris da empresa
- `GET/POST /api/company/members`

**Nutritionist — convites de empresa** (Fase 4E)
- `GET  /api/nutritionist/company-invitations` — lista convites pendentes (email casado ou vinculo direto)
- `POST /api/nutritionist/company-invitations/{token}/accept`
- `POST /api/nutritionist/company-invitations/{token}/reject`
- Quando a empresa faz `inviteNutri`, alem do email o backend cria uma `Notification` in-app para o nutri (se ja cadastrado) apontando para a tela `/nutritionist/company-invitations`.

**Nutritionist — perfis de trabalho / practices** (Fase 5D)
- `GET  /api/nutritionist/practices` — lista as practices do nutri (particular primeiro, depois vinculadas), cada uma com `companyName` (null quando particular), `status` (ACTIVE/SUSPENDED) e bio/specialties/acceptsNewClients.
- `PUT  /api/nutritionist/practices/{id}` — atualiza bio/specialties/acceptsNewClients. Valida ownership.
- **Filtro por practice ativa** — os seguintes endpoints agora aceitam `?practiceId=` opcional (usado pelo `PracticeSwitcher` do frontend). Quando omitido, retorna consolidado do nutri:
  - `GET /api/nutritionist/clients?practiceId=`
  - `POST /api/nutritionist/clients/invite?practiceId=` (vinculo criado na practice indicada)
  - `GET /api/nutritionist/metrics/overview?practiceId=`
- Frontend: novo `PracticeService` mantem `activePracticeId` em sessionStorage; `PracticeSwitcher` no toolbar aparece quando o nutri tem >=2 practices; a tela `/nutritionist/practices` permite editar bio/opt-in de cada perfil separadamente.

**Desvinculacao inteligente (Fase 5E)** — em `CompanyService.revokeWithAction` e `leaveCompany`, o hook `NutritionistPracticeService.endLinkWithCompany(nutriId, companyId)` decide:
- Se o nutri **nao tem** practice particular → a practice vinculada revogada e **convertida em particular** (`company_id = NULL`, mantem `ACTIVE`, mantem bio/specialties/opt-in). Nutri nao perde acesso. Clientes que ficam com essa practice (nao encerrados/transferidos pela empresa) migram junto.
- Se **tem** particular → apenas `SUSPENDED` (comportamento antigo).
- A notificacao enviada ao nutri no revoke informa se houve conversao ("criamos um perfil particular automaticamente com base nesta atuacao").

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
- `GET/PUT/DELETE /api/admin/users` — suporta `?q=&sort=&page=&size=` (paginado, 10 por default)
- `POST /api/admin/companies` (cria empresa + owner + envia token)
- `GET /api/admin/companies?q=&sort=&page=&size=` (paginado)
- `GET /api/admin/metrics/overview` — MRR consolidado BRL (via `daily_fx_rates`), MAU/WAU/DAU, plan distribution, top empresas/nutris, contagem por audit action

**Busca + ordenacao + paginacao (Fases 6B + 7B):**
Endpoints de listagem aceitam:
- `?q=<termo>` — busca case-insensitive em nome/email/CPF (so digitos)/CNPJ/CRN. CPF vem mascarado no response (LGPD).
- `?sort=name|created` — alfabetico ou data desc (default = mais recentes primeiro).
- `?page=<n>&size=<m>` — 0-based, default `page=0&size=10`. Resposta: `{ items, total, page, size }`.
- Enderecos com suporte: `/admin/users`, `/admin/companies`, `/company/nutritionists`, `/company/clients`, `/nutritionist/clients`.

**Company metrics** — `/api/company/metrics/**` role COMPANY
- `GET /overview` — KPIs (nutris ativos/pendentes, clientes, IA mensal, refeicoes 30d, MRR da assinatura) + tabela agregada por nutri (sem PII de cliente)

**Nutritionist metrics** — `/api/nutritionist/metrics/**` role NUTRITIONIST
- `GET /overview` — KPIs (clientes ativos, sugestoes IA/mes com limite do plano, cardapios ativos, adesao media 7d) + tabela por cliente com adesao (% de dias com refeicao nos ultimos 7d), flag `needsAttention` quando < 30%

**Nutritionist dashboard** — rota frontend `/nutritionist/dashboard` (nao adiciona endpoint novo, orquestra existentes)
- Compoe KPIs de `nutritionist/metrics/overview` + cards de proximas acoes (`nutritionist/menu-plans/pending`, `nutritionist/client-requests`, clientes em atencao) + card do proprio plano do nutri (`/api/subscription/me`).
- `DashboardComponent` (cliente) redireciona para esta rota quando o efetivo (sem impersonation) e NUTRITIONIST, e para `/company/dashboard` quando COMPANY. Ao sair da impersonation, `app.component#stopImpersonation` tambem roteia pelo papel do efetivo.

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

**Nutri discovery (Fase 4D + evolucao 5B)** — cliente encontra e pede vinculo a um nutri
- `GET  /api/nutritionists/available` — retorna 1 linha **por practice** ACTIVE + `acceptsNewClients=true`. Um nutri com particular + N empresas pode aparecer N+1 vezes, cada linha com sua propria `bio`/`specialties` e o `companyId`/`companyName` (null quando particular).
- `GET  /api/nutritionists/{id}` — id agora e `practiceId`. Fallback: se nao encontrar practice mas encontrar userId, retorna a practice particular do nutri (compat).
- `PUT  /api/nutritionists/me/profile` (NUTRITIONIST) — Fase 5B: escreve na practice **particular** (a "principal" enquanto nao ha seletor de practice). Fase 5D vai expor seletor.
- `POST /api/nutritionists/{id}/request-link` — id = practiceId. Cria `NutritionistClientLink` apontando para o nutri user (coluna `practice_id` na NCL sera adicionada na Fase 5C).
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
- Novo servico → criar `<Servico>Test` em `backend/src/test/java/com/prandiumnutri/service/` usando `PlanTestFixtures.seedAll()` no `@BeforeEach`.
- i18n: qualquer string exibida no frontend deve ter chave em `frontend/src/assets/i18n/{pt,en,fr}.json`.

## Observabilidade (Fases 4A + 4B + 8)

**Trace ID por request (Fase 8)**: `TraceIdFilter` (`Ordered.HIGHEST_PRECEDENCE`) gera UUID de 16 chars por request (ou honra `X-Trace-Id` do cliente), coloca em MDC (`traceId`), devolve como header `X-Trace-Id` na response. Pattern do `logback-spring.xml` inclui `[tid=%X{traceId:-none}]` em toda linha de log — dá pra correlacionar 1:1 log ↔ response HTTP. Fora de request (jobs @Scheduled, tests) aparece `[tid=none]`.

**Global error handler** (`config/GlobalExceptionHandler`): `@RestControllerAdvice` com handlers específicos (`EntityNotFoundException`, `BadCredentials`, `AccessDenied`, `IllegalArgument/State`, `PlanLimitExceeded`, `MethodArgumentNotValid`) + fallback para `Exception.class`. Todas as respostas incluem `traceId` no body. Fallback loga stack completo mas retorna `{error: "internal_error", traceId}` — nunca vaza detalhes internos pro cliente.

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
- **Fase 5A (nutritionist practices — modelo N:M com particular + vinculadas, schema + backfill + hooks; sem mudanca de UI): OK**
- **Fase 5B (bio/specialties/discovery por practice — cada practice vira 1 card no find-nutri, com badge Particular/Empresa): OK**
- **Fase 5C (vinculo cliente ↔ practice; PLUS automatico enquanto ha vinculo ativo; revoke desativa PLUS ou transfere sponsor pra outra practice): OK**
- **Fase 5D (practice switcher no toolbar + tela `/nutritionist/practices` + filtro por practice em clientes/metrics/dashboard): OK**
- **Fase 5E (desvinculacao inteligente — se nutri nao tem particular, a practice vinculada revogada e convertida em particular; caso contrario, apenas suspende): OK**
- **Fase 5F (feature gate por practice — nutri atuando via empresa herda `maxClientsPerNutri`/`aiSuggestionsMonthly` do plano da empresa; particular usa o plano pessoal): OK**
- **Fase 5G (sponsor so concede PLUS quando pagador tem tier != FREE; pricing filtrado por role + dialog "Ver outros planos"): OK**
- **Fase 6A (CPF obrigatorio + CRN opcional para nutris; V021 backfill sequencial; mascaramento LGPD `123.***.***-45`): OK**
- **Fase 6B (busca + ordenacao — endpoints com `?q=&sort=` em admin/users, admin/companies, company/nutritionists, company/clients, nutritionist/clients; SearchSortBar reutilizavel): OK**
- **Fase 7A (DevDataSeeder — popula 2 empresas + 4 nutris + 10 clientes se banco vazio; senha unificada `admin@123` em dev): OK**
- **Fase 7B (paginacao — `PageDto` no backend, `Page<T>` no TS, `mat-paginator` nas 5 listagens; 10 por pagina default): OK**
- **Fase 7C (padrao visual unificado nas listagens — `.row-card` grid `auto 1fr auto` com avatar circular + info + actions substitui `<mat-table>` em `/admin/users` e `/company/clients`, alinhando com `/company/nutritionists`): OK**
- **Fase 8 (hardening — Setembro 2026): OK**
  - CI GitHub Actions (backend + frontend em paralelo, cache maven/npm, sanity build)
  - Trace ID via MDC filter + logback pattern + fallback error handler com `traceId` na response
  - Payment webhook: `findByProviderSubscriptionId` (fim do `findAll()` N+1), upsert idempotente em `customer.subscription.created` corrige race condition Stripe, handler `invoice.payment_succeeded` recupera PAST_DUE→ACTIVE
  - HMAC-SHA256 validation dos webhooks Mercado Pago (`MercadoPagoSignatureVerifier`) — fail-open em dev, obrigatório em prod
  - `RestClient` com `connectTimeout=10s` + `readTimeout=30s` (override via `HTTP_*_TIMEOUT_SECONDS`) — evita webhook travar se provider externo ficar lento
  - Testes: +11 backend (`PaymentWebhookServiceTest`, `MercadoPagoSignatureVerifierTest`), +17 frontend (novos specs `billing`/`admin-metrics`/`nutri-menu-plans` + renderização real em `pricing`/`meals`)
  - Docs operacionais: `docs/PAGAMENTOS-SETUP.md`, `docs/STRIPE-DRY-RUN.md`, `docs/UX-AUDIT-2026-09.md`
  - UX audit ao vivo via Playwright cobrindo admin/nutri/cliente; 2 regressões P0 fixadas (sparklines dos KPI cards + Chart.js `bar` controller registrado globalmente em `main.ts`)
- **Fase 8 wave-2 (UX audit fixes — 2026-09-26): OK**
  - Bugs P0: billing "não incluído" com ✅ (fix via `[style.color]` inline contornando view encapsulation em `mat-icon`)
  - Fricções P1: `NutriClientsComponent` separa ativos de histórico (toggle `showHistory`), placeholders de busca contextuais por tela (`search.placeholderAdminUsers/Companies/Clients`), `DashboardComponent` mostra banner "Configure suas metas" + CTA quando `!hasGoals()`, ícones-ação ganham `matTooltip` + `aria-label`, contador de refeições separado dos macros no dashboard, campos "Data" com labels distintos em `/meals`
  - Polish P2: `adminCompanies.owner` traduzido, header duplicado do `/suggestions` removido, `CpfUtil.mask` agora mascara todos os primeiros dígitos (`***.***.***-45`), `FindNutritionistComponent` agrupa nutri por `nutritionistId` com practices internas, `PricingComponent` mostra "Plano atual" quando `isCurrentPlan(p)` e `formatFeature` retorna texto natural, linha "Provedor: MANUAL" oculta no billing
  - i18n: novas chaves em pt/en/fr para `search.placeholder*`, `nutri.clients.showHistory/hideHistory`, `dashboard.goalsNotSetTitle/Hint/setGoals`, `meals.mealDate/historyDate`, `adminCompanies.stats.admins/pending/nutris`, `findNutri.practicesCount`, `login.showPassword/hidePassword`
  - Verificação: `mvn test` 94/94 + `npm test` 60/60 + validação visual Playwright
- **Fase 9A (consulta clínica — labs + snapshots + self-fill — 2026-09-26): OK**
  - Migração V025: 18 colunas de exames laboratoriais em `client_assessments` (glicêmicos: glucose_fasting/hba1c/insulin_fasting; lipídios: total_cholesterol/ldl/hdl/triglycerides; tireoide: tsh/t4_free; vitaminas: vitamin_d/vitamin_b12/iron/ferritin; outros: crp/urea/creatinine/uric_acid + lab_date/lab_notes) + tabela `assessment_snapshots` (payload JSON versionado, `uq(assessment_id, version)`)
  - Backend: nova entity `AssessmentSnapshot` (JDBC type JSON via Hibernate 6) + `AssessmentSnapshotRepository` + service estende `ClientAssessmentService` (`createSnapshot`/`listSnapshots`/`listSnapshotsForClient`/`selfFillForClient`) + novo DTO `AssessmentSelfFillDto` restrito a campos safe (habitos/preferencias/objetivo)
  - Endpoints:
    - `POST /api/nutritionist/assessments/client/{id}/snapshots` `{label}` — cria snapshot da ficha corrente (nutri)
    - `GET  /api/nutritionist/assessments/client/{id}/snapshots` — lista timeline (nutri)
    - `PUT  /api/me/assessment/self-fill` — cliente atualiza preferências/hábitos/objetivo (rejeita antropometria/labs/clínico)
    - `GET  /api/me/assessment/snapshots` — cliente vê seus próprios snapshots
  - Frontend: `NutriAssessmentComponent` ganha seção "Exames laboratoriais" (5 sub-grupos) + botão "Nova consulta" (cria snapshot) + card timeline com `v1..vN` + peso do momento; `MyAssessmentComponent` ganha render read-only dos labs + `<mat-expansion-panel>` "Atualizar minhas informações" (self-fill form) + timeline "Minhas consultas anteriores"
  - Testes: `ClientAssessmentServiceTest` cobre upsert com labs, snapshot com incremento de versão, snapshot falha sem ficha, self-fill atualiza só campos safe e falha sem ficha inicial, ownership (nutri sem link é bloqueado)
  - Verificação: `mvn test` 100/100 (94 + 6 novos) + `npm test` 60/60
- **Fase 9B (antropometria completa + composição corporal + gráfico de evolução — 2026-09-26): OK**
  - Migração V026: 7 dobras cutâneas (tricipital, subescapular, suprailíaca, abdominal, coxa, peitoral, axilar média) + 5 perímetros faltantes (coxa, panturrilha, tórax, antebraço, abdômen) + coluna `skinfold_protocol` (JP_7/JP_3/DURNIN_4/POLLOCK_3/MANUAL)
  - Backend: entity/DTO/service atualizados; `ClientAssessmentDto` agora expõe `clientSex`/`clientBirthDate` a partir do `User` (necessários pra cálculo Jackson-Pollock no cliente)
  - Frontend: novo `util/assessment-metrics.ts` com funções puras `bmi()`, `whr()`, `bodyFatJP7()` (fórmula Siri sobre densidade JP), `leanMassKg()`, `fatMassKg()` + categoria IMC + risco cardiovascular WHR por sexo; nova seção "Dobras cutâneas" e 5 novos perímetros na antropometria; card de derivados (IMC/RCQ/%G-JP7/MG/MM) em pills coloridas (verde/amarelo/vermelho pra risco); render read-only espelhado no `MyAssessmentComponent`
  - Novo componente reutilizável `AssessmentTrendChartComponent` (Chart.js line via ng2-charts) — consome snapshots + ficha atual, exibe chips só das métricas com >=1 valor no histórico (peso, %gordura, IMC, cintura, quadril, coxa, braço, glicemia, HbA1c, colesterol total/LDL/HDL/triglicerídeos, vit D), renderiza série temporal ordenada por data. Empty state exige >=2 pontos.
  - Testes: novo caso `upsert_persistsSkinfoldsAndPerimeters` no `ClientAssessmentServiceTest`. Suite backend em 101/101, frontend em 60/60.
- Fase 8 (geolocalizacao restaurantes): backlog
