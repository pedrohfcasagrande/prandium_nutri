package com.prandiumnutri.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prandiumnutri.domain.AssessmentSnapshot;
import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.NutritionistClientLink;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.AssessmentSelfFillDto;
import com.prandiumnutri.dto.AssessmentSnapshotDto;
import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.repository.AssessmentSnapshotRepository;
import com.prandiumnutri.repository.ClientAssessmentRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientAssessmentService {

    private final ClientAssessmentRepository repository;
    private final AssessmentSnapshotRepository snapshotRepository;
    private final NutritionistClientLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // -------------------- Leituras --------------------

    /** Cliente le a propria ficha mais recente (independente do nutri que criou). */
    @Transactional(readOnly = true)
    public Optional<ClientAssessmentDto> findLatestForClient(Long clientId) {
        return repository.findByClientIdOrderByUpdatedAtDesc(clientId).stream()
                .findFirst()
                .map(this::toDto);
    }

    /** Nutri le a ficha que ele mesmo mantem para aquele cliente. */
    @Transactional(readOnly = true)
    public Optional<ClientAssessmentDto> findByNutriAndClient(Long nutriId, Long clientId, Long practiceId) {
        authorizeNutriForClient(nutriId, clientId, practiceId);
        return repository.findByNutritionistIdAndClientId(nutriId, clientId).map(this::toDto);
    }

    // -------------------- Upsert --------------------

    /**
     * Cria ou atualiza a ficha do par (nutri, cliente). Sincroniza campos
     * antropometricos/clinicos/preferencias para `users` — assim o proprio
     * usuario ja usa esses dados no calculo de sugestoes IA, progresso, etc.
     */
    @Transactional
    public ClientAssessmentDto upsert(User nutri, Long clientId, Long practiceId,
                                       ClientAssessmentDto dto) {
        NutritionistClientLink link = authorizeNutriForClient(nutri.getId(), clientId, practiceId);
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado"));

        ClientAssessment ent = repository.findByNutritionistIdAndClientId(nutri.getId(), clientId)
                .orElseGet(() -> ClientAssessment.builder()
                        .nutritionist(nutri)
                        .client(client)
                        .practice(link.getPractice())
                        .assessmentDate(dto.assessmentDate() != null ? dto.assessmentDate() : LocalDateTime.now())
                        .build());

        // Practice pode ter mudado desde a ficha original — respeita o link atual
        ent.setPractice(link.getPractice());

        if (dto.assessmentDate() != null) ent.setAssessmentDate(dto.assessmentDate());

        // Antropometria
        ent.setWeightKg(dto.weightKg());
        ent.setHeightCm(dto.heightCm());
        ent.setBodyFatPct(dto.bodyFatPct());
        ent.setWaistCm(dto.waistCm());
        ent.setHipCm(dto.hipCm());
        ent.setNeckCm(dto.neckCm());
        ent.setArmCm(dto.armCm());

        // Clinico
        ent.setChiefComplaint(dto.chiefComplaint());
        ent.setMedicalHistory(dto.medicalHistory());
        ent.setMedications(dto.medications());
        ent.setPreExistingConditions(dto.preExistingConditions());
        ent.setAllergies(dto.allergies());
        ent.setFamilyHistory(dto.familyHistory());

        // Habitos
        ent.setDietaryRecall24h(dto.dietaryRecall24h());
        ent.setMealFrequency(dto.mealFrequency());
        ent.setWaterIntakeLiters(dto.waterIntakeLiters());
        ent.setAlcoholFrequency(dto.alcoholFrequency());
        ent.setFoodPreferences(dto.foodPreferences());
        ent.setFoodAversions(dto.foodAversions());

        // Estilo
        ent.setPhysicalActivityDetails(dto.physicalActivityDetails());
        ent.setSleepHours(dto.sleepHours());
        ent.setStressLevel(dto.stressLevel());
        ent.setSmoker(dto.smoker());
        ent.setSmokingDetails(dto.smokingDetails());

        // Objetivos
        ent.setGoalNotes(dto.goalNotes());
        ent.setTargetWeightKg(dto.targetWeightKg());
        ent.setTargetDate(dto.targetDate());

        // Exames laboratoriais
        ent.setLabDate(dto.labDate());
        ent.setGlucoseFasting(dto.glucoseFasting());
        ent.setHba1c(dto.hba1c());
        ent.setInsulinFasting(dto.insulinFasting());
        ent.setTotalCholesterol(dto.totalCholesterol());
        ent.setLdl(dto.ldl());
        ent.setHdl(dto.hdl());
        ent.setTriglycerides(dto.triglycerides());
        ent.setTsh(dto.tsh());
        ent.setT4Free(dto.t4Free());
        ent.setVitaminD(dto.vitaminD());
        ent.setVitaminB12(dto.vitaminB12());
        ent.setIron(dto.iron());
        ent.setFerritin(dto.ferritin());
        ent.setCrp(dto.crp());
        ent.setUrea(dto.urea());
        ent.setCreatinine(dto.creatinine());
        ent.setUricAcid(dto.uricAcid());
        ent.setLabNotes(dto.labNotes());

        // Meta
        ent.setNotes(dto.notes());

        ent = repository.save(ent);

        // Sync com o cadastro do usuario. So sobrescreve quando a ficha traz valor;
        // se veio null, mantem o que o cliente ja tinha.
        syncUserFromAssessment(client, dto);
        userRepository.save(client);

        return toDto(ent);
    }

    // -------------------- Helpers --------------------

    private void syncUserFromAssessment(User client, ClientAssessmentDto dto) {
        if (dto.weightKg() != null) client.setWeightKg(dto.weightKg());
        if (dto.heightCm() != null) client.setHeightCm(dto.heightCm().doubleValue());
        if (dto.medications() != null && !dto.medications().isBlank()) client.setMedications(dto.medications());
        if (dto.preExistingConditions() != null && !dto.preExistingConditions().isBlank()) {
            client.setPreExistingConditions(dto.preExistingConditions());
        }
        if (dto.allergies() != null && !dto.allergies().isBlank()) client.setAllergies(dto.allergies());
        if (dto.foodPreferences() != null && !dto.foodPreferences().isBlank()) {
            client.setPreferences(dto.foodPreferences());
        }
    }

    private NutritionistClientLink authorizeNutriForClient(Long nutriId, Long clientId, Long practiceId) {
        return linkRepository.findByNutritionistIdOrderByInvitedAtDesc(nutriId).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE
                          && l.getClient() != null && l.getClient().getId().equals(clientId)
                          && (practiceId == null
                              || (l.getPractice() != null && l.getPractice().getId().equals(practiceId))))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Voce nao atende este cliente"));
    }

    // -------------------- Self-service do cliente --------------------

    /**
     * Cliente atualiza a propria ficha em campos "safe" (habitos, preferencias, objetivos).
     * Exige que ao menos um nutri ja tenha iniciado a ficha — o cliente nao cria ficha do zero,
     * mas pode complementar/atualizar a mais recente. Nada de antropometria/labs/prescricao.
     */
    @Transactional
    public ClientAssessmentDto selfFillForClient(Long clientId, AssessmentSelfFillDto dto) {
        ClientAssessment ent = repository.findByClientIdOrderByUpdatedAtDesc(clientId).stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ainda nao ha ficha criada — aguarde o nutricionista iniciar."));

        if (dto.dietaryRecall24h() != null) ent.setDietaryRecall24h(dto.dietaryRecall24h());
        if (dto.mealFrequency() != null) ent.setMealFrequency(dto.mealFrequency());
        if (dto.waterIntakeLiters() != null) ent.setWaterIntakeLiters(dto.waterIntakeLiters());
        if (dto.alcoholFrequency() != null) ent.setAlcoholFrequency(dto.alcoholFrequency());
        if (dto.foodPreferences() != null) ent.setFoodPreferences(dto.foodPreferences());
        if (dto.foodAversions() != null) ent.setFoodAversions(dto.foodAversions());
        if (dto.physicalActivityDetails() != null) ent.setPhysicalActivityDetails(dto.physicalActivityDetails());
        if (dto.sleepHours() != null) ent.setSleepHours(dto.sleepHours());
        if (dto.smoker() != null) ent.setSmoker(dto.smoker());
        if (dto.smokingDetails() != null) ent.setSmokingDetails(dto.smokingDetails());
        if (dto.goalNotes() != null) ent.setGoalNotes(dto.goalNotes());
        if (dto.targetWeightKg() != null) ent.setTargetWeightKg(dto.targetWeightKg());
        if (dto.targetDate() != null) ent.setTargetDate(dto.targetDate());

        ent = repository.save(ent);

        // Sync com o cadastro do usuario para o que faz sentido
        User client = ent.getClient();
        if (dto.foodPreferences() != null && !dto.foodPreferences().isBlank()) {
            client.setPreferences(dto.foodPreferences());
            userRepository.save(client);
        }
        return toDto(ent);
    }

    // -------------------- Snapshots --------------------

    /**
     * Cria snapshot imutavel da ficha corrente do par (nutri, cliente).
     * Versao e auto-incrementada por assessment. Nao ha ficha? Nada a snapshotar.
     */
    @Transactional
    public AssessmentSnapshotDto createSnapshot(User nutri, Long clientId, Long practiceId, String label) {
        authorizeNutriForClient(nutri.getId(), clientId, practiceId);
        ClientAssessment assessment = repository.findByNutritionistIdAndClientId(nutri.getId(), clientId)
                .orElseThrow(() -> new EntityNotFoundException("Nao ha ficha para snapshot"));

        int nextVersion = snapshotRepository.findFirstByAssessmentIdOrderByVersionDesc(assessment.getId())
                .map(s -> s.getVersion() + 1)
                .orElse(1);

        String json;
        try {
            json = objectMapper.writeValueAsString(toDto(assessment));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar snapshot", e);
        }

        AssessmentSnapshot snap = AssessmentSnapshot.builder()
                .assessment(assessment)
                .version(nextVersion)
                .label(label != null && !label.isBlank() ? label : null)
                .payload(json)
                .createdBy(nutri)
                .build();
        snap = snapshotRepository.save(snap);
        return toSnapshotDto(snap);
    }

    @Transactional(readOnly = true)
    public List<AssessmentSnapshotDto> listSnapshots(Long nutriId, Long clientId, Long practiceId) {
        authorizeNutriForClient(nutriId, clientId, practiceId);
        return repository.findByNutritionistIdAndClientId(nutriId, clientId)
                .map(a -> snapshotRepository.findByAssessmentIdOrderByVersionDesc(a.getId()).stream()
                        .map(this::toSnapshotDto).toList())
                .orElseGet(List::of);
    }

    /** Cliente le seus proprios snapshots (independente do nutri que criou). */
    @Transactional(readOnly = true)
    public List<AssessmentSnapshotDto> listSnapshotsForClient(Long clientId) {
        return repository.findByClientIdOrderByUpdatedAtDesc(clientId).stream()
                .flatMap(a -> snapshotRepository.findByAssessmentIdOrderByVersionDesc(a.getId()).stream())
                .map(this::toSnapshotDto)
                .toList();
    }

    private AssessmentSnapshotDto toSnapshotDto(AssessmentSnapshot s) {
        ClientAssessmentDto payload = null;
        try {
            payload = objectMapper.readValue(s.getPayload(), ClientAssessmentDto.class);
        } catch (JsonProcessingException e) {
            // payload corrompido — devolve null e loga; caller decide o que fazer
        }
        return new AssessmentSnapshotDto(
                s.getId(),
                s.getAssessment() != null ? s.getAssessment().getId() : null,
                s.getVersion(),
                s.getLabel(),
                s.getCreatedBy() != null ? s.getCreatedBy().getId() : null,
                s.getCreatedBy() != null ? s.getCreatedBy().getName() : null,
                s.getCreatedAt(),
                payload
        );
    }

    ClientAssessmentDto toDto(ClientAssessment a) {
        return new ClientAssessmentDto(
                a.getId(),
                a.getClient() != null ? a.getClient().getId() : null,
                a.getClient() != null ? a.getClient().getName() : null,
                a.getClient() != null ? a.getClient().getEmail() : null,
                a.getNutritionist() != null ? a.getNutritionist().getId() : null,
                a.getNutritionist() != null ? a.getNutritionist().getName() : null,
                a.getPractice() != null ? a.getPractice().getId() : null,
                a.getAssessmentDate(),
                a.getWeightKg(), a.getHeightCm(), a.getBodyFatPct(),
                a.getWaistCm(), a.getHipCm(), a.getNeckCm(), a.getArmCm(),
                a.getChiefComplaint(), a.getMedicalHistory(), a.getMedications(),
                a.getPreExistingConditions(), a.getAllergies(), a.getFamilyHistory(),
                a.getDietaryRecall24h(), a.getMealFrequency(), a.getWaterIntakeLiters(),
                a.getAlcoholFrequency(), a.getFoodPreferences(), a.getFoodAversions(),
                a.getPhysicalActivityDetails(), a.getSleepHours(), a.getStressLevel(),
                a.isSmoker(), a.getSmokingDetails(),
                a.getGoalNotes(), a.getTargetWeightKg(), a.getTargetDate(),
                a.getLabDate(), a.getGlucoseFasting(), a.getHba1c(), a.getInsulinFasting(),
                a.getTotalCholesterol(), a.getLdl(), a.getHdl(), a.getTriglycerides(),
                a.getTsh(), a.getT4Free(), a.getVitaminD(), a.getVitaminB12(),
                a.getIron(), a.getFerritin(), a.getCrp(),
                a.getUrea(), a.getCreatinine(), a.getUricAcid(), a.getLabNotes(),
                a.getNotes(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
