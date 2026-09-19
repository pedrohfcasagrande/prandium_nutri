# Fase 3 — Planos, Multi-currency e Pagamentos

**Status:** proposta de modelagem (não implementado)
**Pré-requisitos:** Fase 2 (multi-tenant Empresa → Nutri → Cliente) deve estar rodando.
**Objetivo:** habilitar cobrança recorrente em BRL, USD e EUR, com abstração de provedor de pagamento (Mercado Pago para BRL, Stripe para USD/EUR), planos tiered por tipo de tenant, feature gating e reporting consolidado em BRL.

---

## 1. Contexto fiscal e decisão do CNPJ

- **CNPJ é brasileiro** (você é PJ Brasil), morando na Europa.
- Cobrar em **BRL** → conta PJ brasileira direto via Mercado Pago (simples).
- Cobrar em **USD/EUR** → precisa receber moeda estrangeira. Opções, em ordem de recomendação:
  1. **Wise Business** — abre contas locais em USD/EUR/GBP, câmbio bom, envia BRL para PJ. Rápido de abrir, ~$50 setup.
  2. **Nomad Business** — similar, mais brasileiro em UX.
  3. **Stripe Brasil** — aceita CNPJ mas paga em BRL (você paga spread deles no câmbio, ~2-4%).
  4. **LLC USA (Delaware/Wyoming)** — só quando volume EUR/USD passar de ~US$ 3-5k/mês. ~$500/ano manutenção. Recebe USD direto, envia como serviço pra PJ Brasil (contrato entre empresas).

**Decisão para o MVP:** começar com **Wise Business** (recebe USD/EUR, converte pra BRL). Rever quando volume justificar LLC.

**Nota fiscal:** cobranças BRL → NFS-e normal do município. Cobranças USD/EUR (exportação de serviço) → verificar isenção de ISS na exportação (varia por cidade); PIS/COFINS podem ser isentos também. **Fale com contador antes do primeiro faturamento internacional.**

---

## 2. Planos definidos

### 2.1 Empresas (`target=COMPANY`)

Tiered por # de nutricionistas ativos.

| Code | Nome | Nutris | Clientes/nutri | BRL | USD | EUR |
|---|---|---|---|---|---|---|
| `COMP_STARTER` | Starter | até 3 | até 30 | R$ 499 | $99 | €89 |
| `COMP_GROWTH` | Growth | até 10 | até 50 | R$ 1.499 | $299 | €269 |
| `COMP_BUSINESS` | Business | até 25 | 100 | R$ 3.499 | $699 | €629 |
| `COMP_ENTERPRISE` | Enterprise | custom | custom | custom | custom | custom |

Enterprise: contrato manual (sem checkout), CFO admin ativa manualmente.

### 2.2 Nutricionistas independentes (`target=NUTRITIONIST`)

Freemium + tiered.

| Code | Nome | Clientes | Sugestões IA/mês | BRL | USD | EUR |
|---|---|---|---|---|---|---|
| `NUTRI_FREE` | Free | até 3 | 10 | R$ 0 | $0 | €0 |
| `NUTRI_PRO` | Pro | até 25 | ilimitado | R$ 79 | $19 | €17 |
| `NUTRI_BUSINESS` | Business | ilimitado | ilimitado + branding próprio | R$ 149 | $35 | €32 |

### 2.3 Clientes diretos (`target=USER`)

Opcional — só para clientes sem nutri, mercado B2C. **Recomendo NÃO priorizar no MVP.**

| Code | Nome | Sugestões IA/mês | Cardápios salvos | BRL | USD | EUR |
|---|---|---|---|---|---|---|
| `CLIENT_FREE` | Free | 3 | 1 ativo | R$ 0 | $0 | €0 |
| `CLIENT_PLUS` | Plus | ilimitado | ilimitado | R$ 19 | $5 | €4 |

---

## 3. Model de dados

### `plans` (catálogo)

```sql
CREATE TABLE plans (
    code VARCHAR(30) PRIMARY KEY,
    target VARCHAR(20) NOT NULL,     -- COMPANY, NUTRITIONIST, USER
    tier VARCHAR(30) NOT NULL,       -- FREE, STARTER, GROWTH, BUSINESS, PRO, ENTERPRISE
    name VARCHAR(100) NOT NULL,
    features JSON NOT NULL,          -- ex: {"maxNutritionists": 3, "maxClientsPerNutri": 30, "aiSuggestionsMonthly": null}
    active BIT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0
);
```

### `plan_prices` (multi-currency)

```sql
CREATE TABLE plan_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_code VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,           -- BRL, USD, EUR
    amount_cents INT NOT NULL,              -- 49900 para R$ 499
    provider VARCHAR(20) NOT NULL,          -- MERCADO_PAGO, STRIPE
    provider_price_id VARCHAR(255) NULL,    -- ID do preço no provedor (Stripe usa price_xxx)
    active BIT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_price_plan FOREIGN KEY (plan_code) REFERENCES plans(code),
    UNIQUE KEY uq_price_active (plan_code, currency, active)
);
```

### `subscriptions`

```sql
CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payer_type VARCHAR(20) NOT NULL,        -- COMPANY, NUTRITIONIST, USER
    payer_id BIGINT NOT NULL,               -- FK para companies.id OU users.id (não é FK física; discriminada por payer_type)
    plan_code VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,           -- travado no signup
    amount_cents INT NOT NULL,              -- snapshot do preço na hora do signup
    provider VARCHAR(20) NOT NULL,
    provider_subscription_id VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,            -- TRIALING, ACTIVE, PAST_DUE, CANCELED, SUSPENDED
    current_period_start DATETIME NOT NULL,
    current_period_end DATETIME NOT NULL,
    cancel_at_period_end BIT(1) NOT NULL DEFAULT 0,
    canceled_at DATETIME NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_code) REFERENCES plans(code),
    INDEX idx_sub_payer (payer_type, payer_id),
    INDEX idx_sub_status (status, current_period_end)
);
```

### `invoices` e `payments`

```sql
CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_cents INT NOT NULL,
    status VARCHAR(30) NOT NULL,            -- OPEN, PAID, FAILED, VOIDED
    provider_invoice_id VARCHAR(255),
    hosted_url VARCHAR(500),                -- URL do provedor para o cliente pagar
    due_at DATETIME NOT NULL,
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_sub FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NULL,
    subscription_id BIGINT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_payment_id VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_cents INT NOT NULL,
    status VARCHAR(30) NOT NULL,            -- PENDING, SUCCEEDED, FAILED, REFUNDED
    method VARCHAR(30),                     -- PIX, CARD, BOLETO, SEPA_DEBIT
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_inv FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);
```

### `daily_fx_rates` (consolidação BRL)

```sql
CREATE TABLE daily_fx_rates (
    date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rate_to_brl DECIMAL(12, 6) NOT NULL,    -- 1 USD = 5.20 BRL etc
    source VARCHAR(30) NOT NULL,
    PRIMARY KEY (date, currency)
);
```

Job diário busca em API pública (`exchangerate-api.com` free tier) e insere. Fallback: usa taxa do dia anterior.

---

## 4. Detecção de currency

Ordem de prioridade (executado ao carregar landing OU checkout):

1. **`user.currency_preference`** (se logado e definida)
2. **País por IP** → mapeia para currency:
   - BR → BRL
   - EU (DE, FR, IT, ES, PT, NL, BE, IE, LU, AT, FI, GR, MT, CY, EE, LV, LT, SI, SK) → EUR
   - default → USD
3. Fallback → USD

**API sugerida:** `ipapi.co` (free 1000/dia, HTTPS, sem cartão). Response inclui `country_code`.

**Backend endpoint:** `GET /api/pricing?currency=<opcional>` — retorna todos os planos ativos na currency detectada/informada. Se `currency` omitida, backend usa header `X-Forwarded-For` para IP-lookup.

**Trava:** ao criar `Subscription`, a `currency` fica **imutável**. Cliente muda país? Mantém a currency até cancelar e re-assinar.

---

## 5. Abstração `PaymentProvider`

```java
public interface PaymentProvider {
    String name();  // "MERCADO_PAGO" ou "STRIPE"
    boolean supportsCurrency(String currency);

    // Criar sessão de checkout
    CheckoutSession createCheckout(Subscription sub, String successUrl, String cancelUrl);

    // Cancelar assinatura
    void cancelSubscription(String providerSubscriptionId, boolean atPeriodEnd);

    // Reativar
    void resumeSubscription(String providerSubscriptionId);

    // Processar webhook (idempotente)
    void handleWebhook(String rawPayload, String signature);
}
```

**Implementações:**
- `MercadoPagoProvider` — supports BRL (e Latam quando add)
- `StripeProvider` — supports USD, EUR, GBP

**Roteamento:** `PaymentService.getProviderFor(currency)` — mapeia currency → provider.

### Webhooks

- Mercado Pago: `POST /api/webhooks/mercado-pago`
- Stripe: `POST /api/webhooks/stripe`

Ambos precisam:
- Validar assinatura HMAC do provider
- Ser **idempotentes** (mesma notificação chegar 2x não deve dobrar coisa) — usar `provider_event_id` guardado em tabela `webhook_events`
- Responder 200 rápido; enfileirar processamento pesado (`@Async`)

---

## 6. Feature gating

Cada plano tem features (JSON no `plans.features`). Ex Nutri Pro:

```json
{
  "maxClients": 25,
  "aiSuggestionsMonthly": null,   // ilimitado
  "customBranding": false,
  "menusUnlimited": true
}
```

**Enforcement:**

```java
@Service
public class FeatureGate {
    boolean canAddClient(Long nutritionistId) {
        Subscription sub = subscriptionService.effectiveSubscription(nutritionistId);
        Integer limit = sub.getPlanFeatures().get("maxClients");
        if (limit == null) return true;
        int current = linkRepository.countActive(nutritionistId);
        return current < limit;
    }

    boolean canUseAiSuggestion(Long userId) { ... }
}
```

Chamado antes de operações relevantes (adicionar cliente, gerar sugestão, salvar cardápio). Se bloquear, retornar 402 (Payment Required) com body:
```json
{ "error": "plan_limit_reached", "feature": "aiSuggestionsMonthly", "upgradeUrl": "..." }
```

Frontend interpreta e mostra modal "Você atingiu o limite. Fazer upgrade?".

### Nutri em empresa

Se nutri está vinculado a uma empresa, sua "subscription efetiva" é a da empresa. Limites são aplicados no total agregado da empresa. Ex: Growth = 50 clientes por nutri; se empresa tem 3 nutris no plano Growth, 150 clientes totais.

---

## 7. Fluxos principais

### Signup empresa
```
1. Company owner cria conta em /register?type=company
2. Preenche dados da empresa + escolhe plano (currency detectada por IP)
3. Vai para checkout do provider (MP se BRL, Stripe se USD/EUR)
4. Após pagamento, webhook cria Subscription status=ACTIVE
5. Company owner entra no dashboard, convida primeiro nutri
```

### Upgrade nutri Free → Pro
```
1. Nutri clica em "Upgradar" no dashboard
2. Escolhe plano Pro (currency = a do país detectado, NÃO a do trial)
3. Checkout Stripe/MP
4. Webhook substitui subscription ativa (Free → Pro), aplica prorata
```

### Cancelamento
```
1. Cliente clica "Cancelar" no billing
2. Backend chama provider.cancelSubscription(atPeriodEnd=true)
3. subscription.cancel_at_period_end = 1, canceled_at = now()
4. Cliente mantém acesso até current_period_end
5. Ao expirar, subscription.status = CANCELED
6. Feature gating passa a bloquear
```

### Inadimplência (past_due)
```
1. Cobrança falha, provider tenta reembolso 3 dias/7 dias/14 dias
2. subscription.status = PAST_DUE
3. Cliente recebe email e vê banner "Atualize seu pagamento"
4. Após 30 dias sem pagamento: status = SUSPENDED
   - Company suspensa → todos nutris/clientes perdem acesso a features pagas (dados preservados)
   - Nutri suspenso → volta pro Free (limita a 3 clientes)
5. Após 60 dias: soft delete? (política a decidir)
```

---

## 8. UI necessária

**Landing/marketing** (fora do escopo do app? ou dentro `/pricing`?)
- Tabela de preços com toggle mensal/anual (anual = 2 meses grátis, incentiva LTV)
- Detecta currency automaticamente
- CTA "Começar" → cadastro pré-selecionando o plano

**No app:**
- `/billing` (Company, Nutri independente, Cliente) — plano atual, próxima cobrança, invoices anteriores, botão upgrade/downgrade/cancel
- `/checkout?plan=NUTRI_PRO` — se necessário passar por confirmação antes do provider
- `/admin/companies/{id}/billing` — visão admin da empresa
- `/admin/plans` — CRUD dos planos (Fase avançada)

---

## 9. Reporting consolidado (input pra Fase 4)

**MRR consolidado em BRL:**
```
MRR_BRL = sum(
  case sub.currency
    when 'BRL' then sub.amount_cents / 100
    else sub.amount_cents / 100 * fx.rate_to_brl
  end
) para sub.status = ACTIVE
```

Métricas por moeda também disponíveis (não converter tudo):
- MRR-BRL, MRR-USD, MRR-EUR (separados)
- MRR total em BRL (consolidado)

Reporte também:
- ARPU (receita média por conta paga)
- Churn mensal (%)
- Distribuição de planos
- Distribuição de currency (# subs por BRL/USD/EUR)

Detalhes no doc da Fase 4.

---

## 10. Estimativa

**Sem MP+Stripe já integrados: 5-7 dias.**

1. Migrations (plans, prices, subscriptions, invoices, payments, fx) (~3h)
2. Seed dos planos definidos (~1h)
3. Abstração PaymentProvider + interface (~2h)
4. MercadoPagoProvider (createCheckout, cancel, webhook) (~6-8h)
5. StripeProvider (~6h — API é mais bem documentada)
6. Detecção de currency (backend + frontend) (~3h)
7. FeatureGate + integração em controllers-chave (~4h)
8. UI: /billing (~5h)
9. UI: /pricing público (~4h)
10. UI: fluxo de checkout (~3h)
11. Emails transacionais (recibo, cobrança falhou, cancelamento) (~3h)
12. Job diário de FX rates (~1h)
13. Testes E2E de webhook (~4h)
14. i18n (~2h)

**Contador em paralelo:** validar setup fiscal Wise Business + NFS-e municipal para exportação de serviço (~1-2 dias de research/contato).
