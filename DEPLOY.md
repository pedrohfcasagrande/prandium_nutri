# Deploy em producao (VPS + Docker Compose + Caddy)

Testado para Hetzner CX22 (Ubuntu 24.04, 2 vCPU / 4 GB). Funciona em qualquer VPS.

## 1. Servidor (uma vez)
```bash
# como root no VPS
apt update && apt upgrade -y
apt install -y docker.io docker-compose-v2 ufw fail2ban
systemctl enable --now docker
ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw --force enable
adduser deploy && usermod -aG docker deploy
```
Apontar o DNS: registo A de `DOMAIN` para o IP do VPS. Esperar propagar antes do passo 3 (Caddy precisa para emitir o certificado).

## 2. Codigo e segredos
```bash
su - deploy
git clone <repo> prandium && cd prandium
cp .env.prod.example .env.prod
openssl rand -base64 24   # DB_PASS
openssl rand -base64 24   # DB_ROOT_PASS
openssl rand -base64 48   # JWT_SECRET
nano .env.prod            # preencher tudo
chmod 600 .env.prod
```

## 3. Subir
```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f backend   # esperar "Started PrandiumNutri..."
curl -sI https://$DOMAIN | head -1          # HTTP/2 200
```
Smoke test externo: abrir `https://DOMAIN`, fazer login com ADMIN_EMAIL, trocar a senha do admin imediatamente.

## 4. Webhooks
- Stripe → Developers → Webhooks → Add endpoint `https://DOMAIN/api/webhooks/stripe`, eventos de `checkout.session.*`, `customer.subscription.*`, `invoice.*`. Copiar o `whsec_` para `STRIPE_WEBHOOK_SECRET` e reiniciar o backend.
- Mercado Pago → Suas integracoes → Webhooks → `https://DOMAIN/api/webhooks/mercado-pago`.
- Testar com o Stripe CLI em modo live-trigger OU com uma compra real de 1 EUR e refund.

## 5. Atualizar
```bash
cd ~/prandium && git pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
docker image prune -f
```
Flyway aplica migrations novas no arranque. Fazer sempre `ls backups/` antes de um deploy que inclua migration destrutiva.

## 6. Backups
`mysql-backup` gera um dump por dia em `./backups`. Sincronizar para fora do servidor (cron do host):
```bash
# ex.: rclone configurado para Backblaze B2
0 4 * * * rclone sync /home/deploy/prandium/backups b2:prandium-backups --min-age 1h
```
Restaurar: `zcat backups/X.sql.gz | docker exec -i prandium-mysql mysql -uprandium -p"$DB_PASS" nutritracker`

## Checklist antes de aceitar o primeiro pagamento
- [ ] Chave Gemini antiga revogada
- [ ] Repositorio privado de novo
- [ ] Senha do admin trocada depois do primeiro login
- [ ] Webhook Stripe recebe e processa um evento de teste (ver logs)
- [ ] Backup existe em `./backups` e esta sincronizado para fora
- [ ] Termos de uso / politica de privacidade publicados (LGPD/RGPD, ha dados de saude)
