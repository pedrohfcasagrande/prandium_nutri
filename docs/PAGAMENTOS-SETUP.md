# Setup de Pagamentos — Stripe & Mercado Pago

Guia operacional pra configurar checkout e webhooks nos providers reais.
Cobre `sandbox` (test mode) e `prod` (live).

Para o modelo de planos e a decisão de qual provider cobra qual moeda, ver `FASE-3-PLANOS-E-PAGAMENTOS.md`.

---

## 1. Visão geral

- **Mercado Pago** cobra em **BRL** (via preapproval).
- **Stripe** cobra em **USD / EUR** (via checkout session recorrente).
- O provider é escolhido automaticamente pelo `PaymentProviderRegistry` com base na moeda do plano.
- Em `dev` e `test` o `MockPaymentClient` ativa a subscription imediatamente sem cobrança real.

Os webhooks são o caminho pelo qual o status da assinatura chega ao banco. Sem webhook configurado, o checkout completa mas a `Subscription` nunca vira `ACTIVE`.

---

## 2. Variáveis de ambiente

Cada perfil (`sandbox`, `prod`) precisa das mesmas vars, trocando as chaves de teste pelas chaves live.

```bash
# Stripe (test keys em sandbox, live keys em prod)
STRIPE_SECRET_KEY=sk_test_...              # ou sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...            # do endpoint configurado no dashboard

# Mercado Pago (test tokens em sandbox, live em prod)
MP_ACCESS_TOKEN=TEST-1234...               # ou APP_USR-... em prod
MP_WEBHOOK_SECRET=<secret gerado pelo MP>  # obrigatório em prod

# URLs de redirecionamento pós-checkout
CHECKOUT_SUCCESS_URL=https://app.prandiumnutri.com/billing?checkout=success
CHECKOUT_CANCEL_URL=https://app.prandiumnutri.com/pricing?checkout=canceled

# Timeouts opcionais (defaults 10s conexão, 30s leitura)
HTTP_CONNECT_TIMEOUT_SECONDS=10
HTTP_READ_TIMEOUT_SECONDS=30
```

O `application.yml` já mapeia todas essas vars para as props `prandiumnutri.*` correspondentes.

---

## 3. Setup Stripe

### 3.1 Chaves de API

1. Dashboard → Developers → API keys
2. Copiar **Secret key** (`sk_test_...` em test mode, `sk_live_...` em live)
3. Setar em `STRIPE_SECRET_KEY`

### 3.2 Webhook endpoint

1. Dashboard → Developers → Webhooks → **Add endpoint**
2. **Endpoint URL**: `https://<seu-host>/api/webhooks/stripe`
3. **Events to send** — assinar exatamente estes 6:
   - `checkout.session.completed`
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
4. Após criar o endpoint, expandir **Signing secret** → copiar (`whsec_...`)
5. Setar em `STRIPE_WEBHOOK_SECRET`

### 3.3 Teste local com Stripe CLI

Pra testar webhook sem deploy:

```bash
# instala uma vez
brew install stripe/stripe-cli/stripe
stripe login

# encaminha eventos do test mode pro backend rodando local
stripe listen --forward-to localhost:8080/api/webhooks/stripe
# copia o whsec_... que aparece e usa como STRIPE_WEBHOOK_SECRET no dev

# em outro terminal, dispara eventos sintéticos
stripe trigger checkout.session.completed
stripe trigger customer.subscription.updated
stripe trigger invoice.payment_succeeded
```

---

## 4. Setup Mercado Pago

### 4.1 Access token

1. Painel → Suas integrações → **Criar aplicação** (uma pra cada ambiente)
2. Aba **Credenciais de teste** (sandbox) ou **Credenciais de produção** (prod)
3. Copiar **Access token** (`TEST-...` ou `APP_USR-...`)
4. Setar em `MP_ACCESS_TOKEN`

### 4.2 Webhook endpoint

1. Painel → Sua aplicação → **Webhooks** → **Configurar notificações**
2. **URL de produção** (ou teste): `https://<seu-host>/api/webhooks/mercado-pago`
3. **Eventos** — assinar:
   - `Assinaturas` (preapproval)
   - `Pagamentos de assinaturas` (preapproval_authorized_payment)
4. Salvar. O MP **exibe uma "Chave secreta"** logo abaixo → copiar
5. Setar em `MP_WEBHOOK_SECRET`

> **Importante:** sem `MP_WEBHOOK_SECRET` configurado, o `MercadoPagoSignatureVerifier`
> abre a porta (fail-open com warning nos logs). **Em produção, sempre configure o secret** —
> caso contrário atacantes podem forjar webhooks e ativar assinaturas sem pagar.

### 4.3 Teste local

O MP não tem CLI equivalente ao Stripe. Duas opções:

**Opção A — expor local com ngrok** (mais realista):

```bash
ngrok http 8080
# copia a URL https://xxx.ngrok.io e configura no painel MP como webhook
```

**Opção B — simular via curl com assinatura válida** (mais rápido):

Ver `MercadoPagoSignatureVerifierTest.validHeader()` para exemplo de como gerar o header `x-signature` correto para um `dataId`+`ts` conhecidos.

---

## 5. Verificação pós-setup

Rode este checklist depois de subir sandbox/prod:

- [ ] Endpoint Stripe recebe `POST` sem 4xx? (Dashboard mostra "delivered")
- [ ] Endpoint MP recebe `POST` sem 4xx?
- [ ] Depois de um checkout completo, aparece 1 linha em `subscriptions` com status `ACTIVE`?
- [ ] Aparece 1 linha em `webhook_events` por evento processado?
- [ ] Não há linhas duplicadas em `webhook_events` (dedup funcionando)?
- [ ] Reenviar o mesmo evento manualmente (Dashboard → resend) → nenhum efeito colateral no banco?

---

## 6. Troubleshooting

| Sintoma | Provavel causa |
|---|---|
| Webhook Stripe retorna `invalid_signature` | `STRIPE_WEBHOOK_SECRET` errado ou endpoint diferente |
| Webhook MP retorna `invalid_signature` | `MP_WEBHOOK_SECRET` diferente do painel; ou `x-signature` ausente |
| Sub fica em `PAST_DUE` mesmo após pagamento | Evento `invoice.payment_succeeded` não assinado no Stripe |
| Sub criada duas vezes com o mesmo `providerSubscriptionId` | *não deveria acontecer* — reportar bug (`findByProviderSubscriptionId` no upsert previne isso) |
| Webhook trava e nunca responde | Provider externo lento; timeouts em `RestClientConfig` limitam a 10s+30s |
| Ordem de eventos: `subscription.created` chega antes de `checkout.session.completed` | *ok, tratado* — o upsert reconstrói a sub a partir do metadata do próprio subscription payload |

---

## 7. Rotação de chaves

Sempre que rotacionar `STRIPE_SECRET_KEY` ou `MP_ACCESS_TOKEN`:

1. Gerar nova chave no dashboard
2. Deploy com as duas chaves aceitas (se o provider suportar) OU janela de manutenção
3. Trocar env var e restart
4. Revogar chave antiga

Para `*_WEBHOOK_SECRET`: rotação sempre precisa de janela — troca no painel + deploy simultâneos.
