package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuditAction;
import com.prandiumnutri.domain.SuggestionScope;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.DailySummaryDto;
import com.prandiumnutri.dto.SuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuSuggestionService {

    private static final String SYSTEM_PROMPT =
            "Voce e um nutricionista pratico. Responda em portugues, com um cardapio "
          + "objetivo em markdown, respeitando preferencias, restricoes e condicoes de saude.";

    private final UserService userService;
    private final DashboardService dashboardService;
    private final RestClient restClient;
    private final FeatureGateService featureGate;
    private final AuditService auditService;

    @Value("${prandiumnutri.ai.provider}")
    private String provider;

    @Value("${prandiumnutri.openai.api-key}")
    private String openAiKey;

    @Value("${prandiumnutri.openai.model}")
    private String openAiModel;

    @Value("${prandiumnutri.openai.base-url}")
    private String openAiBaseUrl;

    @Value("${prandiumnutri.gemini.api-key}")
    private String geminiKey;

    @Value("${prandiumnutri.gemini.model}")
    private String geminiModel;

    @Value("${prandiumnutri.gemini.base-url}")
    private String geminiBaseUrl;

    public SuggestionResponse suggest(Long userId, String extraNotes) {
        return suggest(userId, extraNotes, SuggestionScope.DAY, null);
    }

    public SuggestionResponse suggest(Long userId, String extraNotes, SuggestionScope scope) {
        return suggest(userId, extraNotes, scope, null);
    }

    /** Fase 5F: quando o nutri gera sugestao via practice vinculada, o gate usa o plano da empresa. */
    public SuggestionResponse suggest(Long userId, String extraNotes, SuggestionScope scope, Long practiceId) {
        SuggestionScope effectiveScope = scope == null ? SuggestionScope.DAY : scope;
        User user = userService.getOrThrow(userId);
        if (user.getBirthDate() == null || user.getWeightKg() == null || user.getHeightCm() == null
                || user.getSex() == null || user.getActivityLevel() == null || user.getGoal() == null) {
            throw new IllegalArgumentException(
                    "Complete o perfil de saude (peso, altura, nascimento, sexo, nivel de atividade e objetivo) para receber sugestoes personalizadas.");
        }

        // Enforcement de limite mensal por plano (considera practice quando aplicavel)
        com.prandiumnutri.domain.PlanTarget target = switch (user.getRole()) {
            case NUTRITIONIST -> com.prandiumnutri.domain.PlanTarget.NUTRITIONIST;
            default -> com.prandiumnutri.domain.PlanTarget.USER;
        };
        featureGate.assertCanUseAiSuggestion(target, user.getId(), practiceId);

        DailySummaryDto summary = dashboardService.summary(userId, LocalDate.now());
        String prompt = effectiveScope == SuggestionScope.WEEK
                ? buildWeekPrompt(user, summary, extraNotes)
                : buildPrompt(user, summary, extraNotes);

        SuggestionResponse response;
        try {
            response = switch (provider == null ? "" : provider.toLowerCase()) {
                case "gemini" -> callGemini(prompt);
                case "openai" -> callOpenAi(prompt);
                default -> {
                    log.warn("AI provider desconhecido: '{}'. Usando fallback.", provider);
                    yield new SuggestionResponse(fallbackMenu(summary, effectiveScope));
                }
            };
        } catch (RuntimeException e) {
            log.error("Falha ao consultar provedor de IA ({}). Usando fallback.", provider, e);
            response = new SuggestionResponse(fallbackMenu(summary, effectiveScope));
        }

        // So conta uso se a chamada retornou algo real (ou fallback tambem conta como consumo)
        featureGate.recordAiSuggestionUsage(target, user.getId(), practiceId);
        auditService.record(AuditAction.SUGGESTION_GENERATED, user.getId(), user.getRole());
        return response;
    }

    private SuggestionResponse callGemini(String prompt) {
        if (geminiKey == null || geminiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY nao configurada");
        }
        String url = geminiBaseUrl + "/models/" + geminiModel + ":generateContent?key=" + geminiKey;

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.7)
        );

        RuntimeException lastError = null;
        int[] delaysMs = { 0, 1500, 3500 };
        for (int attempt = 0; attempt < delaysMs.length; attempt++) {
            if (delaysMs[attempt] > 0) {
                try { Thread.sleep(delaysMs[attempt]); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            try {
                Map<?, ?> response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                List<?> candidates = (List<?>) response.get("candidates");
                if (candidates == null || candidates.isEmpty()) {
                    throw new RuntimeException("Resposta Gemini sem candidates: " + response);
                }
                Map<?, ?> first = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) first.get("content");
                List<?> parts = (List<?>) content.get("parts");
                Map<?, ?> part = (Map<?, ?>) parts.get(0);
                return new SuggestionResponse((String) part.get("text"));
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                // 5xx e passageiro (rate limit ou alta demanda) - tenta de novo
                log.warn("Gemini {} (tentativa {}/{}). Retrying...",
                        e.getStatusCode(), attempt + 1, delaysMs.length);
                lastError = e;
            }
        }
        throw lastError != null ? lastError : new RuntimeException("Gemini falhou apos tentativas");
    }

    private SuggestionResponse callOpenAi(String prompt) {
        if (openAiKey == null || openAiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY nao configurada");
        }
        Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7
        );
        Map<?, ?> response = restClient.post()
                .uri(openAiBaseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        List<?> choices = (List<?>) response.get("choices");
        Map<?, ?> first = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) first.get("message");
        return new SuggestionResponse((String) message.get("content"));
    }

    private String buildPrompt(User user, DailySummaryDto summary, String extraNotes) {
        StringBuilder health = new StringBuilder();
        if (user.isHasBariatricSurgery()) {
            health.append("- Fez cirurgia bariatrica (porcoes menores, priorizar proteina)\n");
        }
        if (user.getMedications() != null && !user.getMedications().isBlank()) {
            health.append("- Medicacoes em uso: ").append(user.getMedications()).append("\n");
        }
        if (user.getPreExistingConditions() != null && !user.getPreExistingConditions().isBlank()) {
            health.append("- Condicoes preexistentes: ").append(user.getPreExistingConditions()).append("\n");
        }
        if (user.getAllergies() != null && !user.getAllergies().isBlank()) {
            health.append("- Alergias/intolerancias: ").append(user.getAllergies()).append("\n");
        }

        return """
                Monte um cardapio para o resto do dia de hoje respeitando estas metas e restricoes.

                Perfil:
                - Nome: %s
                - Objetivo: %s
                - Meta de calorias diaria: %.0f kcal
                - Meta de proteinas diaria: %.0f g
                - Preferencias: %s

                Condicoes de saude:
                %s

                Ja consumido hoje:
                - Calorias: %.0f kcal
                - Proteinas: %.0f g
                - Restante: %.0f kcal e %.0f g de proteina

                Notas extras: %s

                Sugira 2 a 3 refeicoes (com quantidades em gramas), totalizando aproximadamente o restante de calorias e proteinas.
                Se houver bariatrica, use porcoes menores. Se houver alergia/intolerancia listada, NAO inclua esse ingrediente.
                Formato em markdown com titulos (## Refeicao) e bullets.
                """.formatted(
                user.getName(),
                user.getGoal().name(),
                summary.calorieGoal(),
                summary.proteinGoal(),
                user.getPreferences() == null ? "nenhuma" : user.getPreferences(),
                health.length() == 0 ? "- Nenhuma condicao informada" : health.toString(),
                summary.caloriesConsumed(),
                summary.proteinConsumed(),
                summary.calorieRemaining(),
                summary.proteinRemaining(),
                extraNotes == null ? "-" : extraNotes
        );
    }

    private String buildWeekPrompt(User user, DailySummaryDto summary, String extraNotes) {
        StringBuilder health = new StringBuilder();
        if (user.isHasBariatricSurgery()) {
            health.append("- Fez cirurgia bariatrica (porcoes menores, priorizar proteina)\n");
        }
        if (user.getMedications() != null && !user.getMedications().isBlank()) {
            health.append("- Medicacoes em uso: ").append(user.getMedications()).append("\n");
        }
        if (user.getPreExistingConditions() != null && !user.getPreExistingConditions().isBlank()) {
            health.append("- Condicoes preexistentes: ").append(user.getPreExistingConditions()).append("\n");
        }
        if (user.getAllergies() != null && !user.getAllergies().isBlank()) {
            health.append("- Alergias/intolerancias: ").append(user.getAllergies()).append("\n");
        }

        return """
                Monte um cardapio completo para uma SEMANA (7 dias) respeitando as metas e restricoes.

                Perfil:
                - Nome: %s
                - Objetivo: %s
                - Meta diaria de calorias: %.0f kcal
                - Meta diaria de proteinas: %.0f g
                - Preferencias: %s

                Condicoes de saude:
                %s

                Notas extras: %s

                Regras de formato (obrigatorio):
                - Use exatamente 7 titulos H2 (##) no formato "## Dia 1 - Segunda", "## Dia 2 - Terca", ..., "## Dia 7 - Domingo".
                - Sob cada dia, liste 4 refeicoes (Cafe da manha, Almoco, Lanche, Jantar) usando titulos H3 (### Cafe da manha, etc).
                - Sob cada refeicao, use bullets "-" com quantidade em gramas ou medidas caseiras.
                - Varie os alimentos ao longo dos dias (nao repita as mesmas refeicoes).
                - Se houver bariatrica, use porcoes menores.
                - Se houver alergia/intolerancia listada, NAO inclua esse ingrediente em nenhum dia.
                - Nao adicione texto fora dessa estrutura.
                """.formatted(
                user.getName(),
                user.getGoal().name(),
                summary.calorieGoal(),
                summary.proteinGoal(),
                user.getPreferences() == null ? "nenhuma" : user.getPreferences(),
                health.length() == 0 ? "- Nenhuma condicao informada" : health.toString(),
                extraNotes == null ? "-" : extraNotes
        );
    }

    private String fallbackMenu(DailySummaryDto summary, SuggestionScope scope) {
        if (scope == SuggestionScope.WEEK) {
            StringBuilder sb = new StringBuilder();
            sb.append("### Sugestao (modo offline - configure uma API de IA para sugestoes dinamicas)\n\n");
            String[] dias = { "Segunda", "Terca", "Quarta", "Quinta", "Sexta", "Sabado", "Domingo" };
            for (int i = 0; i < 7; i++) {
                sb.append("## Dia ").append(i + 1).append(" - ").append(dias[i]).append("\n\n");
                sb.append("### Cafe da manha\n- 2 fatias de pao integral\n- 2 ovos mexidos\n- 1 fruta\n\n");
                sb.append("### Almoco\n- 150g de peito de frango grelhado\n- 100g de arroz integral\n- Salada a vontade com azeite\n\n");
                sb.append("### Lanche\n- 1 iogurte natural (170g)\n- 30g de castanhas\n\n");
                sb.append("### Jantar\n- 120g de peixe assado\n- 80g de batata doce\n- Legumes no vapor\n\n");
            }
            return sb.toString();
        }
        return ("""
                ### Sugestao (modo offline - configure uma API de IA para sugestoes dinamicas)

                Voce ainda tem cerca de **%.0f kcal** e **%.0f g de proteina** para o restante do dia.

                ## Almoco/Jantar
                - 150g de peito de frango grelhado
                - 100g de arroz integral cozido
                - Salada a vontade com azeite (1 colher)

                ## Lanche
                - 1 iogurte natural (170g)
                - 30g de castanhas

                ## Ceia
                - 2 ovos mexidos
                """).formatted(summary.calorieRemaining(), summary.proteinRemaining());
    }
}
