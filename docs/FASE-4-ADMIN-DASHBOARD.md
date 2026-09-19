# Fase 4 — Admin Dashboard (Métricas & Monetização)

**Status:** proposta de modelagem (não implementado)
**Pré-requisitos:** Fase 2 (multi-tenant) e Fase 3 (pagamentos) idealmente prontas. Sem Fase 3, o dashboard perde as métricas de receita.
**Objetivo:** painéis de controle para os 3 níveis com poder gerencial — Admin (global), Company (empresa), Nutritionist (própria carteira). Cada nível vê apenas o que seu escopo permite.

---

## 1. Três dashboards, três escopos

| Dashboard | Quem vê | Escopo |
|---|---|---|
| **Admin Global** | ADMIN | Toda a plataforma: empresas, nutris independentes, todos os clientes, receita consolidada em BRL |
| **Empresa** | COMPANY OWNER/ADMIN | Nutricionistas vinculados, clientes deles (agregado, sem PII), consumo, billing da empresa |
| **Nutricionista** | NUTRITIONIST | Clientes vinculados, sugestões usadas, cardápios ativos, evolução coletiva, billing (se independente) |

---

## 2. Métricas por dashboard

### 2.1 Admin Global

**KPIs no topo (cards):**
- MRR consolidado em BRL (com % de crescimento mês-a-mês)
- ARR (MRR × 12)
- MAU global
- Churn (30d)
- # empresas ativas
- # nutricionistas ativos (empresa + independente)
- # clientes ativos totais

**Gráficos:**
- Linha: MRR ao longo dos últimos 12 meses (BRL consolidado)
- Barras: novos assinantes por mês (últimos 6 meses), stacked por tipo (Company, NutriPro, NutriBusiness, ClientPlus)
- Pie: distribuição de planos ativos
- Pie: distribuição de currency (BRL / USD / EUR)
- Barras: uso da IA por mês (# sugestões geradas)
- Linha: DAU / WAU / MAU nos últimos 30 dias

**Tabelas:**
- Top 10 empresas por MRR
- Top 10 nutris independentes por # clientes
- Empresas em inadimplência (status `PAST_DUE`)
- Alertas ativos (ver seção 6)

**Filtros globais:** período (7d, 30d, 90d, 12m, custom) — todas as métricas respeitam.

### 2.2 Empresa

**KPIs:**
- # nutricionistas ativos (de N do plano — ex: 8/10 do Growth)
- # clientes totais (soma dos clientes dos nutris)
- % de utilização do plano
- Próxima cobrança (data + valor)
- Status do billing (OK / PAST_DUE / etc)

**Gráficos:**
- Barras: sugestões IA geradas por nutri no mês
- Linha: crescimento de clientes ao longo do tempo
- Pie: distribuição de clientes por nutri

**Tabelas:**
- Nutricionistas: nome, # clientes, sugestões mês, última atividade
- Convites pendentes

**Sem PII individual:** empresa nunca vê nome/peso/refeições de cliente. Só agregados.

### 2.3 Nutricionista

**KPIs:**
- # clientes ativos
- Sugestões IA geradas no mês (X / Y do plano)
- Cardápios ativos
- Adesão média dos clientes (% de dias com refeição registrada nos últimos 7 dias)

**Gráficos:**
- Linha: engajamento coletivo (# refeições registradas por dia — soma dos clientes)
- Barras: sugestões IA por semana
- Ranking: clientes por adesão (# refeições últimos 30d)

**Tabelas:**
- Clientes com adesão baixa (< 30% nos últimos 7 dias) — flag "Precisa atenção"
- Clientes sem cardápio ativo
- Últimas ações realizadas nos clientes

---

## 3. Coleta de dados (novo)

Hoje o sistema não trackeia:
- Data de criação do user (não há coluna)
- Acessos / last_seen
- Ações relevantes (sugestão gerada, refeição registrada, cardápio salvo)

### 3.1 Adicionar timestamps em users

```sql
-- V007__user_timestamps.sql
ALTER TABLE users
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_seen_at DATETIME NULL;
```

`JwtAuthFilter` atualiza `last_seen_at` (throttled: no máximo 1 write a cada 5 min por user).

### 3.2 Audit log (já mencionado na Fase 2)

```sql
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    on_behalf_of_user_id BIGINT NULL,
    company_context_id BIGINT NULL,
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60),
    entity_id BIGINT,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_actor (actor_user_id, created_at),
    INDEX idx_target (on_behalf_of_user_id, created_at),
    INDEX idx_action (action, created_at),
    INDEX idx_company (company_context_id, created_at)
);
```

Escrever assíncrono (`@Async`) via aspect ou eventos.

**Ações a trackear:**
- `LOGIN`, `LOGOUT`
- `MEAL_LOGGED`, `MEAL_DELETED`
- `SUGGESTION_GENERATED`
- `MENU_PLAN_CREATED`, `MENU_PLAN_ACTIVATED`
- `PROGRESS_LOGGED`
- `HEALTH_PROFILE_UPDATED`
- `CLIENT_INVITED`, `CLIENT_ACCEPTED`, `CLIENT_REVOKED`
- `NUTRI_INVITED`, `NUTRI_ACCEPTED`, `NUTRI_REVOKED`
- `SUBSCRIPTION_CREATED`, `SUBSCRIPTION_CANCELED`, `PAYMENT_FAILED`

**Retenção:** 90 dias por padrão; políticas LGPD podem exigir purge quando user pede exclusão (apaga o user_id → NULL, mantém stats).

### 3.3 Subscriptions e FX (vêm da Fase 3)

Usadas para consolidar MRR em BRL.

---

## 4. Endpoints backend

### `/api/admin/metrics/**`

```
GET /overview                        → todos KPIs em um payload
GET /users?range=30d                 → série temporal novos users
GET /users/active?range=30d          → série temporal DAU/WAU/MAU
GET /actions?action=X&range=30d      → série temporal por action
GET /revenue?range=12m               → MRR consolidado por mês
GET /revenue/by-currency?range=12m   → separado por moeda
GET /plans/distribution              → contagem por plano
GET /companies/top?limit=10          → top por MRR
GET /nutris/top?limit=10             → top independentes por # clientes
```

### `/api/company/metrics/**`

```
GET /overview                        → KPIs da empresa
GET /nutritionists                   → lista + métricas de cada
GET /nutritionists/{id}/summary      → agregado (sem PII)
GET /usage?range=30d                 → uso de IA, refeições, etc
```

Backend valida sempre: quem pede é OWNER/ADMIN dessa empresa via `CompanyMember`.

### `/api/nutritionist/metrics/**`

```
GET /overview
GET /clients                         → clientes vinculados + adesão
GET /clients/{id}/adherence          → adesão detalhada de um cliente
```

### `/api/admin/alerts`

```
GET → lista de alertas ativos
POST /{id}/dismiss → marcar como visto
```

---

## 5. Cache e performance

**Estratégia:**
- Métricas caras (agregações do dia) → cache com TTL de 5-15 min via Caffeine em memória
- Chave do cache inclui: escopo (admin/company_id/nutri_id) + período + tipo
- Refresh assíncrono opcional: job diário à meia-noite pré-aquece o cache do dia anterior

**Consultas otimizadas:**
- Índices em `audit_log(action, created_at)` e `audit_log(on_behalf_of_user_id, created_at)`
- Para MRR: uma view materializada seria ideal, mas com MySQL não temos direto — usar query direta cacheada
- Para grandes volumes (>100k rows/mês em audit_log): considerar rollup diário em `audit_daily_summary`

---

## 6. Alertas automáticos

Job scheduled (diário, hora fixa):

**Admin (globais):**
- Churn mensal > 5% → alerta
- MRR variou > 15% (up ou down) semana passada
- N cobranças falharam nas últimas 24h (threshold configurável)
- API de IA falhou > 20% das requisições
- Nutricionistas com 0 clientes há > 60 dias
- Empresas em PAST_DUE > 15 dias

**Empresa:**
- Um nutri não faz login há > 14 dias
- Consumo de IA na empresa está > 90% do limite do plano
- Nenhum cliente novo em 30 dias

**Nutri:**
- Cliente sem refeição registrada há > 7 dias
- Cliente sem cardápio ativo
- Você usou 90% das sugestões do mês

Tabela:

```sql
CREATE TABLE alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope VARCHAR(20) NOT NULL,          -- ADMIN, COMPANY, NUTRITIONIST
    scope_id BIGINT NULL,                -- id da company/nutri quando aplicável
    severity VARCHAR(20) NOT NULL,       -- INFO, WARN, CRITICAL
    kind VARCHAR(60) NOT NULL,           -- CHURN_HIGH, PAYMENT_FAILED, ...
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000),
    metadata JSON,
    dismissed_at DATETIME NULL,
    dismissed_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alerts_scope (scope, scope_id, dismissed_at)
);
```

Aparecem em banner amarelo/vermelho no topo do dashboard respectivo.

---

## 7. UI

### Layout genérico (comum aos 3 dashboards)

```
┌───────────────────────────────────────────────────┐
│  [Filtro período]                                 │
├───────────────────────────────────────────────────┤
│  KPI cards (4-6 em row responsiva)               │
├──────────────────────┬────────────────────────────┤
│  Gráfico principal   │  Gráfico secundário       │
├──────────────────────┴────────────────────────────┤
│  Gráficos menores em grid                        │
├───────────────────────────────────────────────────┤
│  Tabelas (top X, alertas, etc)                    │
└───────────────────────────────────────────────────┘
```

Componentes reutilizáveis:
- `<app-metric-card>` — label, valor, delta com seta, ícone
- `<app-chart-card>` — wrapper com título, subtitle, chart via ng2-charts
- `<app-alert-banner>` — banner dismissable
- `<app-metric-table>` — tabela paginada com filtros

---

## 8. Suporte / operação (bônus)

Admin dashboard também deve ter ferramentas operacionais:

- **Buscar user por email/nome** → ver perfil completo (com log)
- **Impersonar** (já existe) — mas mostrar aviso "acesso registrado em audit"
- **Ver auditoria de um user** — todas as ações dele nos últimos 30 dias
- **Refund manual** — criar refund via API do provider (Stripe/MP), atualizar payment
- **Suspender/reativar empresa manualmente** — override de status
- **Ajustar limite manual de um plano** — para clientes enterprise negociados
- **Log de webhooks** — debug de integrações

---

## 9. Segurança e LGPD (revisão)

- Todas as métricas agregadas: **nunca expor PII** (nome, email, dados de saúde)
- Drill-down por user só via `/admin/audit/user/{id}` → **log da consulta** para evitar abuso
- Métricas de empresa: valida caller é `CompanyMember` da empresa
- Métricas de nutri: valida caller é o próprio nutri (ou admin da empresa dele)
- LGPD: quando user pede exclusão → audit_log.user_id = NULL (preserva estatística), demais tabelas seguem a política de retenção

---

## 10. Estimativa

**Total: 5-7 dias após Fase 2 e 3 prontas.**

**Coleta (base para tudo):**
1. Migration V007 (user timestamps) + V008 (audit_log + alerts) (~2h)
2. `JwtAuthFilter` update `last_seen_at` throttled (~1h)
3. `@Async` audit writer + integração em endpoints-chave (~4-6h)

**Admin dashboard:**
4. `AdminMetricsController` + queries agregadas + cache (~6h)
5. UI Admin (KPIs, gráficos, tabelas, filtros) (~6-8h)
6. Ferramentas operacionais (buscar, impersonate + audit, refund, suspender) (~6h)

**Company dashboard:**
7. `CompanyMetricsController` (~3h)
8. UI Company (~4h)

**Nutri dashboard:**
9. `NutritionistMetricsController` + adesão (~3h)
10. UI Company (~4h)

**Alertas:**
11. Job daily de alertas + endpoint (~3h)
12. UI de banners + dismiss (~2h)

**Polish:**
13. i18n (~2h)
14. Loading skeletons + empty states (~2h)
