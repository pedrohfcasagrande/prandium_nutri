package com.prandiumnutri.service;

import com.prandiumnutri.domain.ClientAssessment;
import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.NutritionistClientLink;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.ClientAssessmentDto;
import com.prandiumnutri.repository.ClientAssessmentRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientAssessmentService {

    private final ClientAssessmentRepository repository;
    private final NutritionistClientLinkRepository linkRepository;
    private final UserRepository userRepository;

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

    private ClientAssessmentDto toDto(ClientAssessment a) {
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
                a.getNotes(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
