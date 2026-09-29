# Stripe Dry-Run E2E

Runbook pra validar o `PaymentWebhookService` **contra o Stripe test mode real**, não só H2. Foco: provar que os cenários que a gente cobriu em unit test (idempotência, race condition, `invoice.payment_succeeded`) funcionam ponta-a-ponta com o payload real do Stripe.

**Tempo estimado:** ~30 min.
**Pré-requisitos:** backend rodando no perfil `sandbox`, MySQL sandbox limpo, `stripe-cli` instalado.

> **Atalho:** depois do setup do capítulo 1, você pode rodar `./scripts/stripe-dry-run.sh` para
> executar automaticamente os cenários A-D e obter pass/fail por cenário. Ele espera os webhooks
> chegarem, consulta o banco e reporta o resultado. O cenário E (idempotência) ainda pede uma
> ação manual no dashboard Stripe (é um teste de resend).

---

## 1. Setup local

### 1.1 Instalar stripe-cli

```bash
brew install stripe/stripe-cli/stripe
stripe login    # abre browser, autentica na sua conta Stripe test
```

### 1.2 Configurar backend no perfil sandbox

Edite `backend/src/main/resources/application-local.yml` (git-ignored) com sua chave de teste:

```yaml
prandiumnutri:
  stripe:
    secret-key: sk_test_...           # do dashboard Stripe → Developers → API keys
    webhook-secret: (preencher no passo 1.3)
```

Suba o backend:
```bash
cd backend
SPRING_PROFILES_ACTIVE=sandbox mvn spring-boot:run
```

### 1.3 Encaminhar webhooks Stripe pro backend

Em outro terminal:
```bash
stripe listen --forward-to localhost:8081/api/webhooks/stripe
```

O comando imprime algo como:
```
> Ready! Your webhook signing secret is whsec_abc123...
```

Copie esse `whsec_...` e cole em `application-local.yml` → `stripe.webhook-secret`. Reinicie o backend.

### 1.4 Criar usuário de teste

Use o cadastro público ou seed do dev. Anote o **user ID** (via `/api/users/me` autenticado, ou consultando `select id, email from users where email='...';`).

---

## 2. Cenário A — Golden Path (checkout completo)

**Objetivo:** cliente inicia checkout, paga, sub vira ACTIVE.

### 2.1 Iniciar checkout via API

```bash
# Autentique-se e pegue o JWT primeiro (via login)
TOKEN="Bearer eyJ..."

curl -X POST http://localhost:8081/api/checkout \
  -H "Authorization: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"planCode":"CLIENT_PLUS","currency":"USD"}'
```

Resposta:
```json
{"checkoutUrl":"https://checkout.stripe.com/c/pay/cs_test_...","sessionId":"cs_test_..."}
```

### 2.2 Completar checkout

Abra a `checkoutUrl` no browser. Use cartão de teste Stripe:
- Número: `4242 4242 4242 4242`
- Data: qualquer futura
- CVC: qualquer
- Nome/endereço: qualquer

Complete o pagamento. Stripe redireciona pra `CHECKOUT_SUCCESS_URL`.

### 2.3 Verificar no terminal do stripe-cli

Você deve ver algo assim, na ordem:
```
--> checkout.session.completed [evt_...]
<--  [200] POST http://localhost:8081/api/webhooks/stripe [evt_...]
--> customer.subscription.created [evt_...]
<--  [200] POST http://localhost:8081/api/webhooks/stripe [evt_...]
--> invoice.paid [evt_...]
--> invoice.payment_succeeded [evt_...]
<--  [200] POST http://localhost:8081/api/webhooks/stripe [evt_...]
```

Todos `200`? ✅ Passou.

### 2.4 Verificar no banco

```sql
SELECT id, plan_id, provider, provider_subscription_id, status,
       amount_cents, current_period_start, current_period_end
FROM subscriptions
WHERE payer_id = <USER_ID>
ORDER BY started_at DESC LIMIT 3;
```

Esperado:
- **1 linha** (não duplicada) com `provider='STRIPE'`, `status='ACTIVE'`
- `current_period_end` ≈ 30 dias no futuro

```sql
SELECT provider_event_id, event_type FROM webhook_events
WHERE provider='STRIPE' ORDER BY id DESC LIMIT 5;
```

Deve ter uma linha por evento processado, sem duplicatas.

---

## 3. Cenário B — Race condition (subscription.created antes de checkout.session.completed)

**Objetivo:** provar que a ordem invertida dos eventos não deixa a sub sem criar.

Stripe entrega webhooks em ordem best-effort mas não garante. Nossa correção no `upsertFromStripeSubscription` cria a sub em qualquer um dos dois eventos.

### 3.1 Disparar apenas subscription.created (sintético)

```bash
# stripe-cli permite disparar eventos com payload custom
stripe trigger customer.subscription.created \
  --override subscription:metadata.planCode=CLIENT_PLUS \
  --override subscription:metadata.payerUserId=<USER_ID>
```

### 3.2 Verificar

```sql
SELECT status, provider_subscription_id
FROM subscriptions WHERE payer_id = <USER_ID>
ORDER BY started_at DESC LIMIT 1;
```

Deve haver **1 sub ACTIVE nova** mesmo sem `checkout.session.completed` ter chegado ainda. Se o handler antigo estivesse ativo, essa linha não existiria.

### 3.3 Disparar o checkout.session.completed depois

```bash
stripe trigger checkout.session.completed \
  --override checkout_session:client_reference_id=<USER_ID> \
  --override checkout_session:metadata.planCode=CLIENT_PLUS
```

Verificar que **não duplicou** — `SELECT count(*) FROM subscriptions WHERE payer_id = <USER_ID> AND status = 'ACTIVE'` continua 1.

---

## 4. Cenário C — Recuperação de PAST_DUE

**Objetivo:** sub que virou `PAST_DUE` deve voltar pra `ACTIVE` quando pagamento entra.

### 4.1 Simular falha de pagamento

```bash
stripe trigger invoice.payment_failed
```

Verificar:
```sql
SELECT status FROM subscriptions
WHERE provider_subscription_id LIKE 'sub_%'
ORDER BY started_at DESC LIMIT 1;
-- Esperado: PAST_DUE
```

### 4.2 Simular pagamento entrando

```bash
stripe trigger invoice.payment_succeeded
```

Verificar:
```sql
SELECT status, current_period_end FROM subscriptions
WHERE provider_subscription_id LIKE 'sub_%'
ORDER BY started_at DESC LIMIT 1;
-- Esperado: ACTIVE, current_period_end atualizado
```

---

## 5. Cenário D — Cancelamento

### 5.1 Cliente cancela (via UI: /billing → botão Cancelar)

Ou via API:
```bash
curl -X POST "http://localhost:8081/api/checkout/cancel?atPeriodEnd=true" \
  -H "Authorization: $TOKEN"
```

### 5.2 Verificar

```sql
SELECT status, cancel_at_period_end, canceled_at
FROM subscriptions WHERE payer_id = <USER_ID>
ORDER BY started_at DESC LIMIT 1;
-- Esperado: cancel_at_period_end=true, status ainda ACTIVE, canceled_at ainda null
```

### 5.3 Forçar cancelamento imediato (dashboard Stripe → cancel sub agora)

Aguardar webhook `customer.subscription.deleted`:

```sql
SELECT status, canceled_at FROM subscriptions
WHERE payer_id = <USER_ID>
ORDER BY started_at DESC LIMIT 1;
-- Esperado: status=CANCELED, canceled_at preenchido
```

---

## 6. Cenário E — Idempotência

**Objetivo:** re-enviar o mesmo evento não deve criar sub duplicada nem re-processar side-effects.

### 6.1 No dashboard Stripe

Developers → Webhooks → clique no endpoint → "Recent events" → escolha um `checkout.session.completed` já processado → **Resend**.

### 6.2 Verificar

```sql
SELECT count(*) FROM webhook_events WHERE provider_event_id = '<evt_id>';
-- Esperado: 1 (não 2)

SELECT count(*) FROM subscriptions WHERE provider_subscription_id = '<sub_id>';
-- Esperado: 1
```

---

## 7. Checklist de saída

Ao terminar o dry-run, todos estes devem ser ✅:

- [ ] Cenário A: sub criada ACTIVE, sem duplicata
- [ ] Cenário B: sub criada mesmo com subscription.created chegando antes
- [ ] Cenário B: checkout.session.completed subsequente não duplica
- [ ] Cenário C: PAST_DUE → ACTIVE com period_end atualizado
- [ ] Cenário D: cancelAtPeriodEnd marcado ao cancelar; CANCELED após webhook.deleted
- [ ] Cenário E: reenvio do mesmo evento não duplica em webhook_events nem em subscriptions
- [ ] Zero linhas com stack trace no log do backend durante os cenários
- [ ] Todos os POSTs no stripe-cli retornaram `200`

Se todos verdes: o refactor do `PaymentWebhookService` está validado em ambiente real. Pode ir pra sandbox de verdade (com URL pública via ngrok ou deploy).

---

## 8. Troubleshooting durante o dry-run

| Sintoma | Provavel causa |
|---|---|
| stripe-cli mostra `400 invalid_signature` | `STRIPE_WEBHOOK_SECRET` no `application-local.yml` diferente do que o `stripe listen` imprimiu |
| Nada aparece no stripe-cli quando completa checkout | Backend não está no ar OU checkout foi feito com chave `sk_live_` em vez de `sk_test_` |
| Sub duplicada aparece | Regressão: `findByProviderSubscriptionId` não estava sendo consultado antes do insert |
| `checkout.session.completed` chega mas sub não é criada | Faltou `metadata.planCode` ou `client_reference_id` no session — verificar `StripePaymentClient.createCheckout` |
| Sub fica em `PAST_DUE` mesmo depois de `invoice.payment_succeeded` | Handler não registrado — verificar que `PaymentWebhookService.processStripeEvent` tem case `"invoice.payment_succeeded"` |
