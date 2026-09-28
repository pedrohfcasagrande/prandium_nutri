package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.AssessmentSnapshotDto;
import com.prandiumnutri.dto.ConsultationDto;
import com.prandiumnutri.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fase 9E — CRUD de consultas + timeline.
 * Autorizacao segue o mesmo padrao do ClientAssessmentService (via NutritionistClientLink).
 */
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository repository;
    private final NutritionistClientLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final AssessmentSnapshotRepository snapshotRepository;
    private final ClientAssessmentService assessmentService;

    /** Nutri lista as consultas do proprio par. */
    @Transactional(readOnly = true)
    public List<ConsultationDto> listForNutri(User nutri, Long clientId, Long practiceId) {
        authorizeNutriForClient(nutri.getId(), clientId, practiceId);
        return repository.findByNutritionistIdAndClientIdOrderByConsultationDateDesc(nutri.getId(), clientId)
                .stream().map(this::toDto).toList();
    }

    /** Cliente lista as proprias consultas (independente de qual nutri). */
    @Transactional(readOnly = true)
    public List<ConsultationDto> listForClient(Long clientId) {
        return repository.findByClientIdOrderByConsultationDateDesc(clientId)
                .stream().map(this::toDto).toList();
    }

    /**
     * Nutri cria uma consulta. Se {@code createSnapshot=true}, dispara
     * {@link ClientAssessmentService#createSnapshot} com label "Consulta de <data>"
     * e linka o snapshot resultante.
     */
    @Transactional
    public ConsultationDto create(User nutri, Long clientId, Long practiceId,
                                   ConsultationDto dto, boolean createSnapshot) {
        NutritionistClientLink link = authorizeNutriForClient(nutri.getId(), clientId, practiceId);
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado"));

        AssessmentSnapshot snapshot = null;
        if (createSnapshot) {
            try {
                AssessmentSnapshotDto s = assessmentService.createSnapshot(
                        nutri, clientId, practiceId,
                        "Consulta de " + (dto.consultationDate() != null
                                ? dto.consultationDate().toLocalDate()
                                : LocalDateTime.now().toLocalDate())
                );
                snapshot = snapshotRepository.findById(s.id()).orElse(null);
            } catch (Exception ignore) {
                // sem ficha ainda; consulta segue sem snapshot
            }
        }

        Consultation ent = Consultation.builder()
                .client(client)
                .nutritionist(nutri)
                .practice(link.getPractice())
                .consultationDate(dto.consultationDate() != null ? dto.consultationDate() : LocalDateTime.now())
                .type(dto.type() != null ? dto.type() : ConsultationType.RETURN)
                .chiefComplaint(dto.chiefComplaint())
                .findings(dto.findings())
                .plan(dto.plan())
                .nextConsultationDate(dto.nextConsultationDate())
                .snapshot(snapshot)
                .build();
        return toDto(repository.save(ent));
    }

    @Transactional
    public ConsultationDto update(User nutri, Long consultationId, ConsultationDto dto) {
        Consultation ent = repository.findById(consultationId)
                .orElseThrow(() -> new EntityNotFoundException("Consulta nao encontrada"));
        if (!ent.getNutritionist().getId().equals(nutri.getId())) {
            throw new AccessDeniedException("Consulta pertence a outro nutricionista");
        }
        if (dto.consultationDate() != null) ent.setConsultationDate(dto.consultationDate());
        if (dto.type() != null) ent.setType(dto.type());
        ent.setChiefComplaint(dto.chiefComplaint());
        ent.setFindings(dto.findings());
        ent.setPlan(dto.plan());
        ent.setNextConsultationDate(dto.nextConsultationDate());
        return toDto(repository.save(ent));
    }

    @Transactional
    public void delete(User nutri, Long consultationId) {
        Consultation ent = repository.findById(consultationId)
                .orElseThrow(() -> new EntityNotFoundException("Consulta nao encontrada"));
        if (!ent.getNutritionist().getId().equals(nutri.getId())) {
            throw new AccessDeniedException("Consulta pertence a outro nutricionista");
        }
        repository.delete(ent);
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

    private ConsultationDto toDto(Consultation c) {
        return new ConsultationDto(
                c.getId(),
                c.getClient() != null ? c.getClient().getId() : null,
                c.getClient() != null ? c.getClient().getName() : null,
                c.getNutritionist() != null ? c.getNutritionist().getId() : null,
                c.getNutritionist() != null ? c.getNutritionist().getName() : null,
                c.getPractice() != null ? c.getPractice().getId() : null,
                c.getConsultationDate(),
                c.getType(),
                c.getChiefComplaint(),
                c.getFindings(),
                c.getPlan(),
                c.getNextConsultationDate(),
                c.getSnapshot() != null ? c.getSnapshot().getId() : null,
                c.getSnapshot() != null ? c.getSnapshot().getVersion() : null,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
