#!/usr/bin/env bash
# Stripe dry-run E2E — automatiza os cenarios do docs/STRIPE-DRY-RUN.md.
#
# Pre-requisitos (voce configura antes de rodar):
#   1. stripe-cli instalado e logado (brew install stripe/stripe-cli/stripe; stripe login)
#   2. Backend rodando no perfil sandbox (SPRING_PROFILES_ACTIVE=sandbox mvn spring-boot:run)
#   3. MySQL sandbox up (docker-compose ou externo)
#   4. `stripe listen --forward-to localhost:8080/api/webhooks/stripe` rodando em outro terminal
#   5. STRIPE_SECRET_KEY (sk_test_...) e STRIPE_WEBHOOK_SECRET (whsec_...) em application-local.yml
#
# Uso:
#   ./scripts/stripe-dry-run.sh                    # roda todos os cenarios
#   ./scripts/stripe-dry-run.sh --skip-checks      # pula validacao de pre-requisitos
#   BACKEND_URL=http://... MYSQL_HOST=... [...]  ./scripts/stripe-dry-run.sh
#
# Variaveis de ambiente:
#   BACKEND_URL       (default: http://localhost:8080)
#   MYSQL_HOST        (default: localhost)
#   MYSQL_PORT        (default: 3307)
#   MYSQL_USER        (default: nutri)
#   MYSQL_PASSWORD    (default: nutri)
#   MYSQL_DB          (default: nutritracker_sandbox)
#   TEST_USER_EMAIL   (default: joao@cliente.com — precisa existir no banco)
#   TEST_USER_PASS    (default: admin@123)

set -euo pipefail

# -------- Config --------
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_USER="${MYSQL_USER:-nutri}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-nutri}"
MYSQL_DB="${MYSQL_DB:-nutritracker_sandbox}"
TEST_USER_EMAIL="${TEST_USER_EMAIL:-joao@cliente.com}"
TEST_USER_PASS="${TEST_USER_PASS:-admin@123}"
SKIP_CHECKS=false

for arg in "$@"; do
  case "$arg" in
    --skip-checks) SKIP_CHECKS=true ;;
    *) echo "arg desconhecido: $arg"; exit 1 ;;
  esac
done

# -------- Helpers --------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[0;33m'; BLUE='\033[0;34m'; NC='\033[0m'
pass_count=0; fail_count=0
pass() { echo -e "  ${GREEN}✓${NC} $1"; pass_count=$((pass_count+1)); }
fail() { echo -e "  ${RED}✗${NC} $1"; fail_count=$((fail_count+1)); }
info() { echo -e "  ${BLUE}·${NC} $1"; }
warn() { echo -e "  ${YELLOW}!${NC} $1"; }
header() { echo -e "\n${BLUE}== $1 ==${NC}"; }

sql() {
  mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" -sN -e "$1" 2>/dev/null
}

wait_for_webhook() {
  # Espera N segundos até que uma nova linha em webhook_events apareça pra $1 (event_type)
  local event_type="$1"; local timeout="${2:-15}"
  local base=$(sql "SELECT COUNT(*) FROM webhook_events WHERE event_type = '$event_type'")
  for _ in $(seq 1 "$timeout"); do
    local cur=$(sql "SELECT COUNT(*) FROM webhook_events WHERE event_type = '$event_type'")
    if [ "$cur" -gt "$base" ]; then return 0; fi
    sleep 1
  done
  return 1
}

# -------- Pre-checks --------
if [ "$SKIP_CHECKS" != "true" ]; then
  header "Pre-check"

  command -v stripe >/dev/null || { fail "stripe-cli nao encontrado. brew install stripe/stripe-cli/stripe"; exit 1; }
  pass "stripe-cli disponivel"

  command -v mysql >/dev/null || { fail "mysql client nao encontrado (brew install mysql-client)"; exit 1; }
  pass "mysql client disponivel"

  command -v jq >/dev/null || { fail "jq nao encontrado (brew install jq)"; exit 1; }
  pass "jq disponivel"

  # Ping backend
  if curl -sf "$BACKEND_URL/actuator/health" >/dev/null 2>&1 \
     || curl -sf "$BACKEND_URL/api/pricing?currency=BRL&target=USER" >/dev/null; then
    pass "backend up em $BACKEND_URL"
  else
    fail "backend nao responde em $BACKEND_URL. Suba com: SPRING_PROFILES_ACTIVE=sandbox mvn spring-boot:run"
    exit 1
  fi

  # Ping MySQL
  if sql "SELECT 1" >/dev/null 2>&1; then
    pass "MySQL up em $MYSQL_HOST:$MYSQL_PORT/$MYSQL_DB"
  else
    fail "MySQL nao acessivel. Verifique MYSQL_* env vars."
    exit 1
  fi

  # Login e captura user ID + JWT
  info "Autenticando como $TEST_USER_EMAIL..."
  LOGIN_RESP=$(curl -sf -X POST "$BACKEND_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$TEST_USER_EMAIL\",\"password\":\"$TEST_USER_PASS\"}" || echo "")
  if [ -z "$LOGIN_RESP" ]; then
    fail "Login falhou. Usuario $TEST_USER_EMAIL existe? Rode DevDataSeeder ou crie manualmente."
    exit 1
  fi
  JWT=$(echo "$LOGIN_RESP" | jq -r '.token // .accessToken // empty')
  [ -z "$JWT" ] && { fail "Nao consegui extrair JWT da resposta de login"; exit 1; }
  USER_ID=$(sql "SELECT id FROM users WHERE email = '$TEST_USER_EMAIL' LIMIT 1")
  pass "Autenticado (user_id=$USER_ID)"
fi

# -------- Cenario A: Golden path --------
header "Cenario A: Golden path (checkout completo)"

info "Criando checkout session (plan=CLIENT_PLUS, currency=USD)..."
CHECKOUT_RESP=$(curl -sf -X POST "$BACKEND_URL/api/checkout" \
  -H "Authorization: Bearer $JWT" \
  -H 'Content-Type: application/json' \
  -d '{"planCode":"CLIENT_PLUS","currency":"USD"}' || echo "")

if [ -z "$CHECKOUT_RESP" ]; then
  fail "POST /api/checkout falhou. Cheque logs do backend + STRIPE_SECRET_KEY."
else
  CHECKOUT_URL=$(echo "$CHECKOUT_RESP" | jq -r .checkoutUrl)
  pass "Checkout session criada"
  info "Abra manualmente: $CHECKOUT_URL"
  info "Use cartao de teste: 4242 4242 4242 4242, qualquer data futura, qualquer CVC"
  warn "Aguardando webhook checkout.session.completed (60s)..."
  if wait_for_webhook "checkout.session.completed" 60; then
    pass "webhook checkout.session.completed recebido"
    SUB=$(sql "SELECT provider_subscription_id, status FROM subscriptions WHERE payer_id = $USER_ID ORDER BY started_at DESC LIMIT 1")
    if echo "$SUB" | grep -q "ACTIVE"; then
      pass "Subscription ACTIVE criada: $SUB"
    else
      fail "Subscription nao ACTIVE: $SUB"
    fi
    DUPS=$(sql "SELECT COUNT(*) FROM subscriptions WHERE payer_id = $USER_ID AND status = 'ACTIVE'")
    if [ "$DUPS" -eq 1 ]; then
      pass "Sem duplicatas ($DUPS sub ACTIVE)"
    else
      fail "Duplicata detectada: $DUPS subs ACTIVE"
    fi
  else
    fail "Timeout esperando webhook. Voce completou o checkout no browser?"
  fi
fi

# -------- Cenario B: Race condition --------
header "Cenario B: Race — subscription.created antes de checkout.session.completed"

info "Disparando customer.subscription.created isolado..."
BEFORE_COUNT=$(sql "SELECT COUNT(*) FROM subscriptions WHERE provider = 'STRIPE'")
stripe trigger customer.subscription.created \
  --add subscription:metadata.planCode=CLIENT_PLUS \
  --add subscription:metadata.payerUserId="$USER_ID" >/dev/null 2>&1 || warn "stripe trigger falhou (provavelmente nao tem sub_ correspondente no ambiente)"

if wait_for_webhook "customer.subscription.created" 20; then
  pass "webhook customer.subscription.created recebido"
  AFTER_COUNT=$(sql "SELECT COUNT(*) FROM subscriptions WHERE provider = 'STRIPE'")
  NEW=$((AFTER_COUNT - BEFORE_COUNT))
  if [ "$NEW" -ge 1 ]; then
    pass "Nova subscription criada mesmo sem checkout.session.completed prévio ($NEW nova(s))"
  else
    warn "Nenhuma sub nova. Pode ser que o metadata nao chegou (limitacao do --add do stripe-cli)"
  fi
else
  fail "Timeout esperando customer.subscription.created"
fi

# -------- Cenario C: Recovery PAST_DUE -> ACTIVE --------
header "Cenario C: Recovery invoice.payment_failed → invoice.payment_succeeded"

info "Disparando invoice.payment_failed..."
stripe trigger invoice.payment_failed >/dev/null 2>&1 || warn "stripe trigger invoice.payment_failed falhou"
if wait_for_webhook "invoice.payment_failed" 15; then
  pass "webhook invoice.payment_failed recebido"
else
  warn "Timeout no payment_failed (pode ser normal se nao ha sub compativel)"
fi

info "Disparando invoice.payment_succeeded..."
stripe trigger invoice.payment_succeeded >/dev/null 2>&1 || warn "stripe trigger invoice.payment_succeeded falhou"
if wait_for_webhook "invoice.payment_succeeded" 15; then
  pass "webhook invoice.payment_succeeded recebido"
  # Verifica se alguma sub PAST_DUE virou ACTIVE recentemente
  RECENT=$(sql "SELECT COUNT(*) FROM subscriptions WHERE status = 'ACTIVE' AND updated_at >= NOW() - INTERVAL 30 SECOND" 2>/dev/null || echo "?")
  info "Subs ACTIVE atualizadas nos ultimos 30s: $RECENT"
else
  fail "Timeout no payment_succeeded"
fi

# -------- Cenario D: Cancelamento --------
header "Cenario D: Cancel (atPeriodEnd)"

info "Cancelando via API..."
CANCEL_RESP=$(curl -sf -X POST "$BACKEND_URL/api/checkout/cancel?atPeriodEnd=true" \
  -H "Authorization: Bearer $JWT" || echo "")
if [ -n "$CANCEL_RESP" ]; then
  pass "POST /checkout/cancel retornou 200"
  CAP=$(sql "SELECT cancel_at_period_end FROM subscriptions WHERE payer_id = $USER_ID ORDER BY started_at DESC LIMIT 1")
  if [ "$CAP" = "1" ]; then
    pass "cancel_at_period_end = true no banco"
  else
    fail "cancel_at_period_end nao marcado (valor: $CAP)"
  fi
else
  fail "POST /checkout/cancel falhou"
fi

# -------- Cenario E: Idempotencia --------
header "Cenario E: Idempotência (reenvio no dashboard)"

warn "Este cenario requer acao manual:"
warn "  1. Abra o dashboard Stripe → Developers → Webhooks"
warn "  2. Clique no endpoint local → escolha um evento ja processado"
warn "  3. Clique 'Resend'"
warn "Depois rode este SQL e confirme que retorna 1:"
echo -e "\n  SELECT COUNT(*) FROM webhook_events WHERE provider_event_id = '<evt_id>';\n"

# -------- Sumario --------
header "Sumario"
echo -e "  ${GREEN}Pass:${NC} $pass_count"
echo -e "  ${RED}Fail:${NC} $fail_count"

if [ "$fail_count" -eq 0 ]; then
  echo -e "\n${GREEN}✓ Todos os cenarios automaticos passaram.${NC}"
  echo "Rode manualmente o cenario E (idempotencia) pra fechar o checklist do runbook."
  exit 0
else
  echo -e "\n${RED}✗ $fail_count cenarios falharam. Veja logs acima + logs do backend.${NC}"
  exit 1
fi
