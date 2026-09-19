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
mvn spring-boot:run
```

Webhooks locais (test):
- Stripe: `stripe listen --forward-to localhost:8080/api/webhooks/stripe`
- MP: `ngrok http 8080` + configurar a URL publica no dashboard MP

## Testes

Backend (81 testes atualmente):
```bash
cd backend
mvn test
```

Frontend (Jest + jest-preset-angular; 43 testes atualmente):
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
- Fase 8 (geolocalizacao restaurantes): backlog
