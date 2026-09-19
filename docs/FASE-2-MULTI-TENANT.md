# Fase 2 — Multi-tenant: Empresa → Nutricionista → Cliente

**Status:** proposta de modelagem (não implementado)
**Objetivo:** transformar o Nutri Tracker em plataforma B2B2C com hierarquia de tenants. Empresas contratam a plataforma, disponibilizam pra seus nutricionistas, que atendem seus clientes. Nutricionistas independentes também são suportados.

---

## 1. Hierarquia e Personas

```
ADMIN (você)
  ├── Empresas (COMPANY)
  │     ├── Nutricionistas (N:N — nutri pode estar em várias empresas)
  │     │     └── Clientes (N:N — cliente pode ter várias nutris, dentro ou fora da mesma empresa)
  │     └── Billing corporativo
  └── Nutricionistas independentes (sem vínculo com empresa)
        └── Clientes
              └── Billing individual (paga direto à plataforma OU nutri paga pelo cliente)
```

| Persona | O que faz |
|---|---|
| **ADMIN** | Gerencia tudo. Vê empresas, nutris independentes, todos os pagamentos que chegam à plataforma. Métricas globais. Suporte. |
| **COMPANY** (novo) | Admin da empresa. Vê nutris vinculados à empresa e métricas agregadas dos clientes deles. Gerencia billing da empresa com a plataforma. Convida/desvincula nutris. **Não vê PII individual dos clientes finais** (LGPD). |
| **NUTRITIONIST** | Vê e gerencia seus clientes vinculados (impersonation). Cria/edita cardápios pros clientes. Se vinculado a empresa, seu uso é coberto pelo plano corporativo. Se independente, gerencia próprio plano na plataforma. |
| **USER** (cliente) | Registra refeições/evolução, aceita/revoga acesso de nutris, vê nutris vinculados. Se pagar direto (sem empresa/nutri cobrindo), gerencia próprio plano. |

---

## 2. Model de dados

### Roles atualizado

```java
enum Role { USER, NUTRITIONIST, COMPANY, ADMIN }
```

### Novas entidades

**`Company`**
| Campo | Tipo | Descrição |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR(255) | Razão social |
| trade_name | VARCHAR(255) | Nome fantasia |
| tax_id | VARCHAR(30) | CNPJ (ou VAT ID se internacional) |
| country_code | VARCHAR(2) | BR, US, PT... |
| contact_email | VARCHAR(255) | |
| contact_phone | VARCHAR(30) | |
| billing_email | VARCHAR(255) | E-mail de faturamento (pode ser diferente) |
| created_at | DATETIME | |
| active | BOOLEAN | |

**`CompanyMember`** — vínculo `Company ↔ User` (COMPANY admins e futuros roles internos)
| Campo | Tipo |
|---|---|
| company_id | FK |
| user_id | FK |
| role_in_company | enum: `OWNER`, `ADMIN`, `MEMBER` |
| joined_at | DATETIME |
| status | enum: `ACTIVE`, `INVITED`, `SUSPENDED` |

**`CompanyNutritionist`** — vínculo N:N `Company ↔ Nutritionist`
| Campo | Tipo |
|---|---|
| company_id | FK |
| nutritionist_id | FK (user com role=NUTRITIONIST) |
| status | enum: `PENDING`, `ACTIVE`, `REVOKED` |
| joined_at | DATETIME |
| revoked_at | DATETIME NULL |

**`NutritionistClientLink`** — vínculo N:N `Nutritionist ↔ Client` (já detalhado antes)
| Campo | Tipo |
|---|---|
| nutritionist_id | FK |
| client_id | FK NULL |
| invited_by_email | VARCHAR(255) |
| status | enum: `PENDING`, `ACTIVE`, `REJECTED`, `REVOKED_BY_CLIENT`, `REVOKED_BY_NUTRI` |
| invited_at | DATETIME |
| accepted_at | DATETIME NULL |
| revoked_at | DATETIME NULL |

---

## 3. Migration proposta

```sql
-- V006__multi_tenant.sql

CREATE TABLE companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    tax_id VARCHAR(30),
    country_code VARCHAR(2) NOT NULL DEFAULT 'BR',
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(30),
    billing_email VARCHAR(255),
    active BIT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_company_tax (tax_id, country_code)
) ENGINE=InnoDB;

CREATE TABLE company_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_in_company VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cm_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_cm_pair (company_id, user_id)
);

CREATE TABLE company_nutritionists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    nutritionist_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at DATETIME NULL,
    CONSTRAINT fk_cn_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_cn_nutri FOREIGN KEY (nutritionist_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_cn_pair (company_id, nutritionist_id)
);

CREATE TABLE nutritionist_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nutritionist_id BIGINT NOT NULL,
    client_id BIGINT NULL,
    invited_by_email VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    invited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME NULL,
    revoked_at DATETIME NULL,
    CONSTRAINT fk_nc_nutri FOREIGN KEY (nutritionist_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_nc_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_nc_pair (nutritionist_id, client_id),
    INDEX idx_nc_email (invited_by_email)
);
```

---

## 4. Permissões / escopo

### Regra geral

Cada request tem um "**escopo efetivo**" resolvido pelo `CurrentUser`:

| Quem está logado | Header `X-Impersonate-User-Id` | Escopo efetivo |
|---|---|---|
| USER | — | próprio user |
| NUTRITIONIST | ausente | dashboard do nutri |
| NUTRITIONIST | id de cliente vinculado (`ACTIVE`) | age como o cliente |
| NUTRITIONIST | id sem vínculo | 403 |
| COMPANY | ausente | dashboard da empresa |
| COMPANY | id de nutri da empresa | age como o nutri |
| COMPANY | id de cliente de nutri da empresa | age como o cliente (mas com **audit obrigatório**) |
| ADMIN | qualquer id | age como qualquer user (já funciona hoje) |

### Endpoints por role

**Company (`/api/company/**`)**
- `GET /api/company/me` — dados da empresa logada
- `PUT /api/company/me` — atualizar
- `GET /api/company/nutritionists` — nutris vinculados
- `POST /api/company/nutritionists/invite` `{ email }` — convidar nutri
- `DELETE /api/company/nutritionists/{id}` — revogar
- `GET /api/company/metrics` — dashboard da empresa (métricas agregadas, sem PII)
- `GET /api/company/billing` — situação financeira com a plataforma
- `GET /api/company/members` — outros admins da empresa
- `POST /api/company/members/invite` `{ email, role }`

**Nutritionist (`/api/nutritionist/**`)**
- `GET /api/nutritionist/clients` — clientes vinculados
- `POST /api/nutritionist/clients/invite` `{ clientEmail }`
- `DELETE /api/nutritionist/clients/{id}` — revogar
- `GET /api/nutritionist/companies` — empresas às quais está vinculado
- `GET /api/nutritionist/dashboard` — visão geral (# clientes, sugestões geradas mês, alertas)

**User (extra ao atual)**
- `GET /api/me/nutritionists` — nutris vinculados
- `DELETE /api/me/nutritionists/{id}` — revogar acesso
- `GET /api/me/invitations` — convites pendentes
- `POST /api/me/invitations/{token}/accept` / `reject`

**Admin extras**
- `GET/POST /api/admin/companies` — CRUD empresas
- `POST /api/admin/users/{id}/promote-nutritionist`
- `POST /api/admin/users/{id}/promote-company-owner`

---

## 5. Fluxos de convite (2 níveis)

**Empresa convida Nutricionista**
```
1. Company admin cadastra: email + mensagem opcional
2. Se email existe e user é NUTRITIONIST → CompanyNutritionist status=PENDING
   Se email existe mas user é USER → notifica "usuário existe mas não é nutricionista, peça pra ele upgradar o perfil"
   Se email não existe → convite em aberto, ao cadastrar/promover, vincula automaticamente
3. Nutri recebe e-mail → aceita/rejeita
4. Aceito: status=ACTIVE
```

**Nutri convida Cliente** (mesmo fluxo já descrito na v1 do doc, sem mudanças)

**Empresa NUNCA convida cliente direto** — sempre via nutri.

---

## 6. LGPD e proteção de dados

Regra fundamental: **cada nível vê menos PII à medida que sobe**.

| O que | USER vê | NUTRI vê | COMPANY vê | ADMIN vê |
|---|---|---|---|---|
| Dados de saúde (peso, medidas, condições) | próprios | dos clientes vinculados | **NÃO** — só métricas agregadas | tudo (com log de auditoria) |
| Refeições/cardápios | próprios | dos clientes vinculados | **NÃO** | tudo (auditado) |
| Email/telefone do cliente | próprio | dos vinculados | apenas contagens | tudo (auditado) |
| Métricas agregadas (# usuários ativos, adesão média, evolução média) | — | dos seus clientes | dos clientes dos nutris vinculados | globais |
| Billing da empresa | — | — | tudo | tudo |
| Pagamentos individuais | próprios | — | — | tudo |

**Consentimento** do cliente deve mencionar explicitamente:
- Que nutri vai ver dados detalhados
- Que empresa da nutri vê só métricas agregadas
- Que plataforma-admin pode ver tudo para suporte (com registro)

---

## 7. Auditoria (obrigatória)

`audit_log` (mesma estrutura já proposta):

```sql
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,          -- quem fez
    actor_role VARCHAR(20) NOT NULL,        -- USER/NUTRI/COMPANY/ADMIN
    on_behalf_of_user_id BIGINT NULL,       -- se impersonando
    company_context_id BIGINT NULL,         -- se ação foi feita no contexto de uma empresa
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60),
    entity_id BIGINT,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_actor (actor_user_id, created_at),
    INDEX idx_target (on_behalf_of_user_id, created_at),
    INDEX idx_action (action, created_at)
);
```

Toda ação de impersonation e edição de dados alheios **deve** ser logada (async, `@Async`).

---

## 8. UI

### Novas telas

**Company**
- `/company/dashboard` — # nutris, # clientes totais, uso da IA no mês, próximo billing
- `/company/nutritionists` — tabela + convidar
- `/company/nutritionists/{id}` — perfil do nutri + métricas agregadas dos clientes dele
- `/company/members` — outros admins da empresa
- `/company/billing` — plano, invoices, pagamentos
- `/company/settings` — dados da empresa

**Nutritionist**
- `/nutritionist/dashboard` — # clientes, sugestões mês, ranking clientes por engajamento
- `/nutritionist/clients` — tabela + convidar
- `/nutritionist/clients/{id}` — impersona (mesmo fluxo do admin)
- `/nutritionist/companies` — empresas vinculadas
- `/nutritionist/billing` (só se independente) — próprio plano

**User (extras)**
- Nova seção em `/profile`: "Nutricionistas vinculados" — lista + revogar
- `/invitations` — bandeja com convites pendentes

### Sidebar (dinâmica)

- **USER:** Dashboard, Refeições, Sugestões, Meus cardápios, Evolução, Meu perfil
- **NUTRITIONIST:** Painel do nutri, Meus clientes, Empresas vinculadas, Meu perfil
- **COMPANY:** Painel da empresa, Nutricionistas, Métricas, Membros, Billing, Configurações
- **ADMIN:** Painel geral, Empresas, Usuários (todos), Nutricionistas independentes, Auditoria, Configurações

---

## 9. Casos-limite a tratar

1. **Nutri em várias empresas:** cada cliente é vinculado a UMA nutri por vez, mas nutri pode estar em N empresas. Ao gerar métrica agregada pra empresa X, considera apenas clientes cujo link com o nutri **foi criado enquanto ele estava vinculado à X** (evita "roubar" clientes para métricas).
2. **Nutri sai da empresa:** clientes dela permanecem no nutri (a menos que empresa politicamente decida diferente). Empresa perde acesso às métricas desses clientes.
3. **Cliente em várias nutris ao mesmo tempo:** permitir sim, com aviso na aceitação. Todas veem os mesmos dados do cliente.
4. **Empresa suspensa por não pagamento:** vínculos com nutris ficam `SUSPENDED`; nutris não conseguem impersonate; clientes seguem intactos (Fase 3 detalha).
5. **Nutri independente vira parte de empresa:** vínculos com clientes existentes migram; billing individual é cancelado, passa a ser coberto pela empresa.

---

## 10. Estimativa (só Fase 2, sem billing)

Sequência (~3-5 dias):
1. Migration V006 + entidades + repositórios (~3h)
2. `CurrentUser` refactor (multi-nível de impersonation com verificação de vínculo) (~2h)
3. Endpoints backend + testes (~6h)
4. Fluxo de convite completo (nutri↔cliente e empresa↔nutri) (~4h)
5. UI: telas Company (~6h)
6. UI: telas Nutritionist (~5h)
7. UI: extras User (invitations, vínculos) (~3h)
8. Auditoria assíncrona (~3h)
9. i18n + polish (~3h)
