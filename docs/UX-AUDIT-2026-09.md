# UX Audit — Setembro 2026

Auditoria manual via Playwright, cobrindo perfis **admin**, **nutricionista** e **cliente**. Não cobriu perfil **empresa** por escopo de tempo — deixado como follow-up.

Escala de severidade:
- 🔴 **Bug** — comportamento errado ou quebrado
- 🟡 **Fricção** — funciona mas custa cognitivo ao usuário
- 🟢 **Melhoria** — polish, oportunidade sem urgência

---

## Regressões introduzidas por refactors anteriores

### 🔴 ~~Sparklines sumiram dos KPI cards~~ ✅ FIXADO
**Onde:** `/admin/metrics`, `/nutritionist/metrics`, `/company/metrics`, `/nutritionist/dashboard`
**Causa real (via inspection):** todos os `[attr.*]` bindings dinâmicos do `SparklineComponent` (viewBox, d, fill, stroke, stop-color) estavam retornando null. Os `computed()` estavam corretos mas nunca eram consumidos no primeiro CD tick — só depois de um `applyChanges` manual os atributos apareciam. Bug de signals + SVG attribute bindings.
**Fix aplicado:** refactor do `SparklineComponent` — como `seed/color/data` nunca mudam durante o ciclo de vida, removi todos os `computed()` e passei a calcular tudo em `ngOnInit`, armazenando em propriedades regulares. Ver `frontend/src/app/components/sparkline.component.ts`.

### 🔴 ~~Gráficos `bar` do admin quebram no console~~ ✅ FIXADO
**Onde:** `/admin/metrics` — cards "MRR por moeda" e "Distribuição de planos ativos" ficavam brancos.
**Erro no console:** `Error: "bar" is not a registered controller.`
**Causa:** Chart.js v4 exige registro explícito dos controllers/scales.
**Fix aplicado:** em `frontend/src/main.ts` adicionei `Chart.register(...registerables)` antes do `bootstrapApplication`. Cobre todos os componentes (progress, dashboard, admin-metrics).

---

## Bugs de estado / lógica

### 🔴 ~~Feature paga aparece com ✅ e "Não incluído" ao mesmo tempo~~ ✅ FIXADO
**Onde:** `/billing` — cliente free via "Encontrar nutricionista" com checkmark verde + texto "Não incluído".
**Causa real:** view encapsulation do Angular bloqueava seletores CSS aninhados/`::ng-deep` no `mat-icon` — a classe `.included`/`.not-included` era aplicada mas o CSS não pegava.
**Fix aplicado:** binding inline `[style.color]` diretamente no `mat-icon` do template do `BillingComponent`, contornando encapsulation. Além disso, esconde a linha "Provedor" quando `provider === 'MANUAL'`.

### 🔴 ~~Sidebar destaca item errado após navegação~~ ⚠️ NÃO REPRODUZIU
**Onde:** dashboard cliente com "Minha ficha" destacada em vez de "Dashboard".
**Investigação:** re-testado via Playwright em sessão nova (login → dashboard → my-assessment → dashboard). Sidebar destaca corretamente `Dashboard` em todos os cenários. Provavelmente foi um estado transitório do HMR do dev server ou timing entre screenshot e change detection.
**Ação:** monitorar. Se aparecer de novo em produção, capturar URL exata + estado do DOM naquele momento.

### 🟡 ~~Cliente revogado aparece duplicado na lista de "Meus clientes"~~ ✅ FIXADO
**Onde:** `/nutritionist/clients` — Pedro Henrique aparecia como "Ativo/Plus" E como "Revogado por você/Plus".
**Fix aplicado:** `NutriClientsComponent` agora separa `activeSection` (vínculos ativos/pending) de `historySection` (revoked/rejected). Toggle "Mostrar vínculos anteriores" (i18n `nutri.clients.showHistory/hideHistory`) esconde a segunda seção por default. Template do card foi extraído para `<ng-template #clientCard>` para reuso entre as duas seções.

---

## Fricções de copy / UX

### 🟡 ~~Placeholder de busca reutilizado onde não faz sentido~~ ✅ FIXADO
**Onde:** vários lugares.
**Fix aplicado:** placeholders contextuais por tela via i18n:
- `/admin/users` → `search.placeholderAdminUsers` (nome, e-mail, CPF, CNPJ, CRN)
- `/admin/companies` → `search.placeholderAdminCompanies` (nome, CNPJ)
- `/nutritionist/clients` → `search.placeholderAdminClients` (nome, e-mail, CPF)
Chaves criadas em pt/en/fr.

### 🟡 ~~Coluna "OWNER" em inglês~~ ✅ FIXADO
**Fix aplicado:** i18n `adminCompanies.owner` → "Responsável" (pt), "Owner" (en), "Responsable" (fr).

### 🟡 ~~Ícones de ação sem tooltip~~ ✅ FIXADO
**Fix aplicado:** `matTooltip` + `aria-label` adicionados nos botões-ícone das listagens (`/admin/users`, `/nutritionist/clients`, `/admin/companies`). Também nos toggles de senha do login/register/reset (i18n `login.showPassword/hidePassword`).

### 🟡 ~~Coluna "ESTATÍSTICAS" com 3 números sem legenda~~ ✅ FIXADO
**Fix aplicado:** cada número recebe `matTooltip` (`adminCompanies.stats.admins/pending/nutris`) explicando o que representa.

### 🟡 ~~Duplicação visual de header em Sugestões~~ ✅ FIXADO
**Fix aplicado:** removido card roxo redundante do `SuggestionsComponent`. Só o page header sobra.

### 🟡 ~~CPF mascarado começando com `000` confunde~~ ✅ FIXADO
**Fix aplicado:** `CpfUtil.mask` agora retorna `***.***.***-45` (mascara todos os primeiros dígitos, não só do meio).

### 🟡 Menu lateral com sufixo redundante "(admin)"
**Onde:** sidebar do admin — "Métricas (admin)", "Usuários (admin)", "Empresas (admin)".
Admin já sabe que está no admin (viu o próprio badge). Sufixo é ruído visual. Ou remove ou usa como agrupamento visual (separator "ADMIN" acima).
**Status:** deixado como polish futuro (baixa prioridade — não bloqueia workflow).

### 🟡 "Meus perfis" ambíguo no menu do nutri
**Onde:** sidebar nutricionista — item "Meus perfis" no fim.
Não é claro o que é "perfis" quando já existe "Meu perfil público" acima e o switcher de practice no topo. É "practices"? "companies"? Renomear.
**Status:** deixado como polish futuro (nomenclatura de domínio, precisa alinhamento).

### 🟡 ~~Metas zeradas viram texto sem sentido no dashboard~~ ✅ FIXADO
**Fix aplicado:** `DashboardComponent` mostra banner amarelo "Configure suas metas" com CTA `/profile-setup` quando `!hasGoals()`. Progress bars usam `[goalCondition]` para se esconder quando `goal=0`.

### 🟡 ~~"Refeições" enfiado dentro de "Outros macros"~~ ✅ FIXADO
**Fix aplicado:** contador de refeições separado do card de macros por `border-top` e estilo `.meals-count` com ícone próprio.

### 🟡 ~~Dois campos de "Data" na tela Refeições~~ ✅ FIXADO
**Fix aplicado:** labels distintos via i18n (`meals.mealDate` = "Data da refeição", `meals.historyDate` = "Ver histórico de:").

### 🟡 ~~Nutricionista aparece 2x em "Encontrar nutricionista"~~ ✅ FIXADO
**Fix aplicado:** `FindNutritionistComponent` agrupa por `nutritionistId` via `grouped` computed signal. Um card por nutri com `<practice-section>` interno por practice (Particular / cada empresa). i18n `findNutri.practicesCount`.

### 🟡 ~~"Grátis para sempre" em vez de "Plano atual"~~ ✅ FIXADO
**Fix aplicado:** `PricingComponent` tem `currentPlanCode` signal + `isCurrentPlan(p)` helper. Botão desabilitado mostra "Plano atual" quando é o plano do usuário logado.

### 🟢 ~~"Provedor de pagamento: MANUAL" aparece pra cliente free~~ ✅ FIXADO
**Fix aplicado:** linha "Provedor" oculta quando `provider === 'MANUAL'` no `BillingComponent`.

### 🟢 ~~Formato de features "1 · Cardápios salvos"~~ ✅ FIXADO
**Fix aplicado:** `formatFeature()` no `PricingComponent` retorna "1 cardapio salvo", "3 sugestoes IA por mes", etc. — texto natural em vez de "N · Nome".

---

## Priorização sugerida

**Atacar já (P0 — bugs):** ✅ 100%
1. ~~Sparklines sumidas nos KPIs~~ ✅ fixado
2. ~~Chart `bar` não registrado~~ ✅ fixado
3. ~~"Não incluído" com ✅ no billing~~ ✅ fixado
4. ~~Sidebar destacando rota errada~~ ⚠️ não reproduziu

**Próximo sprint (P1 — fricções altas):** ✅ 100%
5. ~~Placeholders de busca contextuais~~ ✅ fixado
6. ~~Cliente revogado duplicado em "Meus clientes"~~ ✅ fixado
7. ~~Metas zeradas no dashboard (empty state + CTA)~~ ✅ fixado
8. ~~Ícones sem tooltip nas tabelas~~ ✅ fixado

**Depois (P2 — polish):** ✅ 100% dos que faz sentido corrigir agora
9. ~~"OWNER" → "RESPONSÁVEL"~~ ✅ fixado
10. ~~Header duplicado em Sugestões~~ ✅ fixado
11. ~~Máscara de CPF (todos dígitos)~~ ✅ fixado
12. ~~Nutri duplicado em Encontrar~~ ✅ fixado
13. Sufixo "(admin)" no menu — deixado (polish, nao bloqueia)
14. ~~Copy dos botões pricing/billing~~ ✅ fixado

---

## Fechamento

Aplicado em: **2026-09-26** (Fase 8 wave-2).

Escopo restante (não bloqueia entrega):
- Auditar perfil **empresa** (não coberto por escopo de tempo desta rodada)
- Renomear "Meus perfis" (nutri) e sufixo "(admin)" — decisões de nomenclatura, precisam alinhamento com produto
- Reinvestigar sidebar destacando rota errada se voltar a aparecer em produção

Verificação: `mvn test` (94/94 verde), `npm test` (60/60 verde), + validação visual via Playwright em admin/nutri/cliente.
