package com.prandiumnutri.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prandiumnutri.domain.*;
import com.prandiumnutri.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * LGPD Art. 18 II — portabilidade: usuario baixa um ZIP com todos os dados
 * pessoais que temos dele. Formato: JSONs por dominio + um README.md
 * explicando o conteudo.
 *
 * <p>Escopo por role:
 * <ul>
 *   <li>USER: perfil, ficha clinica (view do cliente), snapshots, refeicoes,
 *       progresso, hidratacao, notificacoes, mensagens de chat enviadas.</li>
 *   <li>NUTRITIONIST: perfil + practices + subscription + issuances de PDF.</li>
 *   <li>COMPANY (owner): perfil da empresa + membros.</li>
 * </ul>
 *
 * <p>Nunca exporta dados de OUTROS usuarios (LGPD proibe): nutri nao recebe
 * dados dos clientes; empresa nao recebe dados dos nutris.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataExportService {

    private final UserRepository userRepository;
    private final MealRepository mealRepository;
    private final ProgressEntryRepository progressRepository;
    private final WaterIntakeRepository waterRepository;
    private final ClientAssessmentRepository assessmentRepository;
    private final AssessmentSnapshotRepository snapshotRepository;
    private final NotificationRepository notificationRepository;
    private final ChatMessageRepository chatRepository;
    private final NutritionistPracticeRepository practiceRepository;
    private final NutritionistClientLinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PrescriptionIssuanceRepository issuanceRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Transactional(readOnly = true)
    public byte[] exportForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User nao encontrado"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            put(zip, "README.md", readme(user).getBytes());
            put(zip, "profile.json", jsonBytes(profilePayload(user)));

            switch (user.getRole()) {
                case USER -> exportClientData(zip, user);
                case NUTRITIONIST -> exportNutritionistData(zip, user);
                case COMPANY -> exportCompanyData(zip, user);
                case ADMIN -> { /* admin so tem perfil e audit — nao exportamos audit por LGPD (dados de outros) */ }
            }

            // Comum: notificacoes + subscription (todos os roles pagam ou tem plano free)
            put(zip, "notifications.json", jsonBytes(
                    notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                            org.springframework.data.domain.PageRequest.of(0, 500))));
            subscriptionRepository.findFirstByPayerTypeAndPayerIdAndStatusInOrderByStartedAtDesc(
                    PlanTarget.USER, userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING))
                    .ifPresent(sub -> put(zip, "subscription.json", jsonBytes(subscriptionPayload(sub))));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar export", e);
        }
        return baos.toByteArray();
    }

    // ---------- USER (cliente) ----------

    private void exportClientData(ZipOutputStream zip, User client) throws IOException {
        Long userId = client.getId();
        put(zip, "meals.json", jsonBytes(
                mealRepository.findByUserIdOrderByDateDescTimeDesc(userId).stream()
                        .map(this::mealPayload).toList()));
        put(zip, "progress.json", jsonBytes(
                progressRepository.findByUserIdOrderByDateAsc(userId).stream()
                        .map(this::progressPayload).toList()));
        // Toda hidratacao (sem filtro de data)
        put(zip, "water_intakes.json", jsonBytes(
                waterRepository.findByUserIdOrderByLoggedAtDesc(userId)));
        // Fichas do cliente (view do cliente — a mesma que ele ve em /my-assessment)
        List<ClientAssessment> fichas = assessmentRepository.findByClientIdOrderByUpdatedAtDesc(userId);
        put(zip, "assessments.json", jsonBytes(fichas.stream()
                .map(this::clientAssessmentPayload).toList()));
        // Snapshots — historico timeline
        fichas.forEach(a ->
                put(zip, "snapshots/assessment-" + a.getId() + ".json",
                        jsonBytes(snapshotRepository.findByAssessmentIdOrderByVersionDesc(a.getId()))));
        // Mensagens de chat enviadas por ele (nao inclui as recebidas — pertencem ao remetente)
        put(zip, "chat_sent.json", jsonBytes(
                chatRepository.findAll().stream()
                        .filter(m -> m.getSender().getId().equals(userId))
                        .map(this::chatPayload).toList()));
    }

    // ---------- NUTRITIONIST ----------

    private void exportNutritionistData(ZipOutputStream zip, User nutri) throws IOException {
        Long userId = nutri.getId();
        put(zip, "practices.json", jsonBytes(
                practiceRepository.findByNutritionistId(userId).stream()
                        .map(this::practicePayload).toList()));
        // Links ativos — so metadata (nome mascarado, status, datas), NAO os dados clinicos dos clientes
        put(zip, "client_links.json", jsonBytes(
                linkRepository.findByNutritionistIdOrderByInvitedAtDesc(userId).stream()
                        .map(this::linkPayloadForNutri).toList()));
        // Issuances de PDF que ele emitiu (metadata do ato de emissao)
        put(zip, "prescription_issuances.json", jsonBytes(
                issuanceRepository.findByNutriIdOrderByIssuedAtDesc(userId).stream()
                        .map(this::issuancePayload).toList()));
        // Mensagens de chat enviadas
        put(zip, "chat_sent.json", jsonBytes(
                chatRepository.findAll().stream()
                        .filter(m -> m.getSender().getId().equals(userId))
                        .map(this::chatPayload).toList()));
    }

    // ---------- COMPANY (owner) ----------

    private void exportCompanyData(ZipOutputStream zip, User owner) throws IOException {
        companyRepository.findByOwnerId(owner.getId()).ifPresent(c -> {
            put(zip, "company.json", jsonBytes(companyPayload(c)));
            put(zip, "company_members.json", jsonBytes(
                    companyMemberRepository.findByCompanyIdOrderByInvitedAtDesc(c.getId()).stream()
                            .map(this::companyMemberPayload).toList()));
        });
    }

    // ---------- payloads ----------

    private Map<String, Object> profilePayload(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("cpf", u.getCpf());
        m.put("role", u.getRole());
        m.put("crn", u.getCrn());
        m.put("provider", u.getProvider());
        m.put("createdAt", u.getCreatedAt());
        m.put("lastSeenAt", u.getLastSeenAt());
        m.put("birthDate", u.getBirthDate());
        m.put("sex", u.getSex());
        m.put("heightCm", u.getHeightCm());
        m.put("weightKg", u.getWeightKg());
        m.put("activityLevel", u.getActivityLevel());
        m.put("goal", u.getGoal());
        m.put("bio", u.getBio());
        m.put("specialties", u.getSpecialties());
        m.put("acceptsNewClients", u.isAcceptsNewClients());
        m.put("consentAcceptedAt", u.getConsentAcceptedAt());
        return m;
    }

    private Map<String, Object> mealPayload(Meal m) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", m.getId());
        out.put("date", m.getDate());
        out.put("time", m.getTime());
        out.put("type", m.getType());
        out.put("items", m.getItems().stream().map(this::mealItemPayload).toList());
        return out;
    }

    private Map<String, Object> mealItemPayload(MealItem i) {
        Map<String, Object> out = new HashMap<>();
        out.put("foodId", i.getFood() != null ? i.getFood().getId() : null);
        out.put("foodName", i.getFood() != null ? i.getFood().getName() : null);
        out.put("grams", i.getGrams());
        return out;
    }

    private Map<String, Object> progressPayload(ProgressEntry p) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", p.getId());
        out.put("date", p.getDate());
        out.put("weightKg", p.getWeightKg());
        out.put("bodyFatPercent", p.getBodyFatPercent());
        out.put("waistCm", p.getWaistCm());
        out.put("hipCm", p.getHipCm());
        out.put("createdAt", p.getCreatedAt());
        return out;
    }

    /** Cliente ve so o snapshot "publico" da ficha — sem nutriNotes. */
    private Map<String, Object> clientAssessmentPayload(ClientAssessment a) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", a.getId());
        out.put("assessmentDate", a.getAssessmentDate());
        out.put("targetCalories", a.getTargetCalories());
        out.put("targetProteinG", a.getTargetProteinG());
        out.put("targetCarbsG", a.getTargetCarbsG());
        out.put("targetFatG", a.getTargetFatG());
        out.put("targetWaterMl", a.getTargetWaterMl());
        out.put("notes", a.getNotes());
        out.put("labDate", a.getLabDate());
        out.put("createdAt", a.getCreatedAt());
        out.put("updatedAt", a.getUpdatedAt());
        return out;
    }

    private Map<String, Object> chatPayload(ChatMessage m) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", m.getId());
        out.put("linkId", m.getLink().getId());
        out.put("content", m.getContent());
        out.put("createdAt", m.getCreatedAt());
        out.put("readAt", m.getReadAt());
        return out;
    }

    private Map<String, Object> practicePayload(NutritionistPractice p) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", p.getId());
        out.put("companyId", p.getCompany() != null ? p.getCompany().getId() : null);
        out.put("companyName", p.getCompany() != null ? p.getCompany().getName() : null);
        out.put("bio", p.getBio());
        out.put("specialties", p.getSpecialties());
        out.put("acceptsNewClients", p.isAcceptsNewClients());
        out.put("status", p.getStatus());
        out.put("createdAt", p.getCreatedAt());
        return out;
    }

    private Map<String, Object> linkPayloadForNutri(NutritionistClientLink l) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", l.getId());
        out.put("clientEmail", l.getInvitedByEmail());  // email do convite (pode ser antes de aceitar)
        out.put("status", l.getStatus());
        out.put("invitedAt", l.getInvitedAt());
        out.put("acceptedAt", l.getAcceptedAt());
        out.put("revokedAt", l.getRevokedAt());
        return out;
    }

    private Map<String, Object> issuancePayload(PrescriptionIssuance i) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", i.getId());
        out.put("assessmentId", i.getAssessment() != null ? i.getAssessment().getId() : null);
        out.put("clientId", i.getClient() != null ? i.getClient().getId() : null);
        out.put("requesterRole", i.getRequesterRole());
        out.put("pdfSha256", i.getPdfSha256());
        out.put("pdfSizeBytes", i.getPdfSizeBytes());
        out.put("issuedAt", i.getIssuedAt());
        return out;
    }

    private Map<String, Object> subscriptionPayload(Subscription s) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", s.getId());
        out.put("planCode", s.getPlan() != null ? s.getPlan().getCode() : null);
        out.put("status", s.getStatus());
        out.put("provider", s.getProvider());
        out.put("currency", s.getCurrency());
        out.put("amountCents", s.getAmountCents());
        out.put("currentPeriodStart", s.getCurrentPeriodStart());
        out.put("currentPeriodEnd", s.getCurrentPeriodEnd());
        return out;
    }

    private Map<String, Object> companyPayload(Company c) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", c.getId());
        out.put("name", c.getName());
        out.put("tradeName", c.getTradeName());
        out.put("taxId", c.getTaxId());
        out.put("countryCode", c.getCountryCode());
        out.put("contactEmail", c.getContactEmail());
        out.put("createdAt", c.getCreatedAt());
        return out;
    }

    private Map<String, Object> companyMemberPayload(CompanyMember m) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", m.getId());
        out.put("email", m.getInvitedByEmail());
        out.put("role", m.getRoleInCompany());
        out.put("status", m.getStatus());
        out.put("invitedAt", m.getInvitedAt());
        return out;
    }

    // ---------- util ----------

    private String readme(User u) {
        return """
                # Export de dados — Prandium Nutri

                Este arquivo ZIP contem todos os dados pessoais que temos sobre voce,
                gerados sob o Art. 18 II da LGPD (direito a portabilidade).

                - Usuario: %s (%s)
                - Papel: %s
                - Data do export: %s

                ## Conteudo

                - `profile.json` — seus dados de perfil
                - `subscription.json` — plano ativo (se houver)
                - `notifications.json` — ultimas 500 notificacoes

                Especifico do seu papel:

                - USER: `meals.json`, `progress.json`, `water_intakes.json`,
                  `assessments.json`, `snapshots/`, `chat_sent.json`
                - NUTRITIONIST: `practices.json`, `client_links.json`,
                  `prescription_issuances.json`, `chat_sent.json`
                - COMPANY (owner): `company.json`, `company_members.json`

                Dados de outros usuarios (por exemplo, clientes que voce atende,
                se voce e nutricionista) nao sao incluidos — LGPD proibe.

                Para duvidas: encaminhe pro DPO.
                """.formatted(u.getName(), u.getEmail(), u.getRole(), LocalDateTime.now());
    }

    private byte[] jsonBytes(Object payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (Exception e) {
            log.warn("Falha ao serializar payload: {}", e.getMessage());
            return "{}".getBytes();
        }
    }

    private void put(ZipOutputStream zip, String entryName, byte[] content) {
        try {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content);
            zip.closeEntry();
        } catch (IOException e) {
            log.warn("Falha ao gravar {} no zip: {}", entryName, e.getMessage());
        }
    }
}
