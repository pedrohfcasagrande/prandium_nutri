package com.prandiumnutri.service;

import com.prandiumnutri.domain.*;
import com.prandiumnutri.dto.AppointmentDto;
import com.prandiumnutri.repository.AppointmentRepository;
import com.prandiumnutri.repository.NutritionistPracticeRepository;
import com.prandiumnutri.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private static final String JITSI_BASE = "https://meet.jit.si/";
    private static final String JITSI_ROOM_PREFIX = "PrandiumNutri-";

    private final AppointmentRepository repository;
    private final UserRepository userRepo;
    private final NutritionistPracticeRepository practiceRepo;
    private final NutritionistLinkService linkService;
    private final NotificationService notificationService;

    /**
     * Fase 9G-3: gera URL Jitsi Meet quando appointment e ONLINE e nao veio URL manual.
     * Jitsi: sem conta, sem chave, sala criada on-demand. UUID no nome da sala garante que
     * nao ha colisao e a URL nao e adivinhavel. Nutri/cliente que ja tenham URL propria
     * (Zoom/Meet) podem enviar via meetingUrl.
     */
    private String ensureMeetingUrl(AppointmentLocation location, String meetingUrl) {
        if (location != AppointmentLocation.ONLINE) return meetingUrl;
        if (meetingUrl != null && !meetingUrl.isBlank()) return meetingUrl;
        return JITSI_BASE + JITSI_ROOM_PREFIX + UUID.randomUUID();
    }

    // -------------------- Listagens --------------------

    @Transactional(readOnly = true)
    public List<AppointmentDto> listForClient(Long clientId) {
        return repository.findByClientIdOrderByScheduledAtDesc(clientId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> listForNutritionist(Long nutritionistId, Long practiceId) {
        return repository.findByNutritionistIdOrderByScheduledAtDesc(nutritionistId).stream()
                .filter(a -> practiceId == null
                          || (a.getPractice() != null && a.getPractice().getId().equals(practiceId)))
                .map(this::toDto).toList();
    }

    /** Empresa ve apenas consultas cujo practice pertence a ela (nunca as da particular do nutri). */
    @Transactional(readOnly = true)
    public List<AppointmentDto> listForCompany(Long companyId) {
        var practices = practiceRepo.findByCompanyIdAndStatus(companyId, PracticeStatus.ACTIVE);
        if (practices.isEmpty()) return List.of();
        var ids = practices.stream().map(NutritionistPractice::getId).toList();
        return repository.findByPracticeIdInOrderByScheduledAtDesc(ids).stream()
                .map(this::toDto).toList();
    }

    // -------------------- Cliente solicita --------------------

    @Transactional
    public AppointmentDto requestByClient(User client, Long nutritionistId,
                                          LocalDateTime scheduledAt, Integer durationMinutes,
                                          AppointmentLocation location, String locationDetails,
                                          String meetingUrl, String notes) {
        NutritionistClientLink link = findActiveLink(client.getId(), nutritionistId);
        User nutri = userRepo.findById(nutritionistId)
                .orElseThrow(() -> new EntityNotFoundException("Nutricionista nao encontrado"));

        Appointment ap = Appointment.builder()
                .client(client)
                .nutritionist(nutri)
                .practice(link.getPractice())
                .scheduledAt(scheduledAt)
                .durationMinutes(durationMinutes != null ? durationMinutes : 60)
                .location(location != null ? location : AppointmentLocation.ONLINE)
                .locationDetails(locationDetails)
                .meetingUrl(ensureMeetingUrl(location != null ? location : AppointmentLocation.ONLINE, meetingUrl))
                .notes(notes)
                .status(AppointmentStatus.REQUESTED)
                .requestedBy(AppointmentActor.CLIENT)
                .lastActionBy(AppointmentActor.CLIENT)
                .build();
        ap = repository.save(ap);
        notify(nutri.getId(), "Nova solicitacao de consulta",
                client.getName() + " solicitou uma consulta em " + fmt(scheduledAt), ap.getId());
        return toDto(ap);
    }

    // -------------------- Nutri sugere --------------------

    @Transactional
    public AppointmentDto suggestByNutri(User nutri, Long clientId, Long practiceId,
                                          LocalDateTime scheduledAt, Integer durationMinutes,
                                          AppointmentLocation location, String locationDetails,
                                          String meetingUrl, String notes) {
        NutritionistClientLink link = findActiveLinkForNutri(nutri.getId(), clientId, practiceId);
        User client = userRepo.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado"));

        Appointment ap = Appointment.builder()
                .client(client)
                .nutritionist(nutri)
                .practice(link.getPractice())
                .scheduledAt(scheduledAt)
                .durationMinutes(durationMinutes != null ? durationMinutes : 60)
                .location(location != null ? location : AppointmentLocation.ONLINE)
                .locationDetails(locationDetails)
                .meetingUrl(ensureMeetingUrl(location != null ? location : AppointmentLocation.ONLINE, meetingUrl))
                .notes(notes)
                .status(AppointmentStatus.REQUESTED)
                .requestedBy(AppointmentActor.NUTRITIONIST)
                .lastActionBy(AppointmentActor.NUTRITIONIST)
                .build();
        ap = repository.save(ap);
        notify(client.getId(), "Nutricionista sugeriu uma consulta",
                nutri.getName() + " sugeriu uma consulta em " + fmt(scheduledAt), ap.getId());
        return toDto(ap);
    }

    // -------------------- Aceitar --------------------

    @Transactional
    public AppointmentDto confirm(User actor, Long appointmentId) {
        Appointment ap = fetchAuthorized(actor, appointmentId);
        AppointmentActor actorRole = roleOf(actor, ap);

        if (ap.getStatus() == AppointmentStatus.REQUESTED) {
            // Somente o lado que NAO solicitou pode confirmar
            if (actorRole == ap.getRequestedBy()) {
                throw new IllegalStateException("Voce mesmo solicitou; aguarde a resposta.");
            }
            ap.setStatus(AppointmentStatus.CONFIRMED);
        } else if (ap.getStatus() == AppointmentStatus.RESCHEDULE_REQUESTED) {
            // Somente o lado que NAO propos o reagendamento pode confirmar
            if (actorRole == ap.getProposedBy()) {
                throw new IllegalStateException("Voce propos o reagendamento; aguarde a outra parte.");
            }
            ap.setScheduledAt(ap.getProposedScheduledAt());
            ap.setProposedScheduledAt(null);
            ap.setProposedBy(null);
            ap.setStatus(AppointmentStatus.CONFIRMED);
        } else {
            throw new IllegalStateException("Consulta nao pode ser confirmada neste estado.");
        }
        ap.setLastActionBy(actorRole);
        ap = repository.save(ap);
        notifyCounterparty(ap, actorRole, "Consulta confirmada",
                actor.getName() + " confirmou a consulta em " + fmt(ap.getScheduledAt()));
        return toDto(ap);
    }

    // -------------------- Cancelar --------------------

    @Transactional
    public AppointmentDto cancel(User actor, Long appointmentId, String reason) {
        Appointment ap = fetchAuthorized(actor, appointmentId);
        if (EnumSet.of(AppointmentStatus.CANCELED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW)
                .contains(ap.getStatus())) {
            throw new IllegalStateException("Consulta ja finalizada; nao pode ser cancelada.");
        }
        AppointmentActor actorRole = roleOf(actor, ap);
        ap.setStatus(AppointmentStatus.CANCELED);
        ap.setLastActionBy(actorRole);
        ap.setCancelReason(reason);
        ap.setCanceledAt(LocalDateTime.now());
        ap = repository.save(ap);
        notifyCounterparty(ap, actorRole, "Consulta cancelada",
                actor.getName() + " cancelou a consulta." + (reason != null && !reason.isBlank()
                        ? " Motivo: " + reason : ""));
        return toDto(ap);
    }

    // -------------------- Propor reagendamento --------------------

    @Transactional
    public AppointmentDto requestReschedule(User actor, Long appointmentId, LocalDateTime newScheduledAt) {
        Appointment ap = fetchAuthorized(actor, appointmentId);
        Set<AppointmentStatus> allowed = EnumSet.of(
                AppointmentStatus.REQUESTED,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.RESCHEDULE_REQUESTED);
        if (!allowed.contains(ap.getStatus())) {
            throw new IllegalStateException("Consulta nao pode ser reagendada neste estado.");
        }
        AppointmentActor actorRole = roleOf(actor, ap);
        ap.setStatus(AppointmentStatus.RESCHEDULE_REQUESTED);
        ap.setProposedScheduledAt(newScheduledAt);
        ap.setProposedBy(actorRole);
        ap.setLastActionBy(actorRole);
        ap = repository.save(ap);
        notifyCounterparty(ap, actorRole, "Solicitacao de reagendamento",
                actor.getName() + " sugeriu remarcar a consulta para " + fmt(newScheduledAt));
        return toDto(ap);
    }

    // -------------------- Marcar como realizada / no-show --------------------

    @Transactional
    public AppointmentDto markCompleted(User nutri, Long appointmentId) {
        Appointment ap = fetchAuthorized(nutri, appointmentId);
        if (nutri.getRole() != Role.NUTRITIONIST) {
            throw new AccessDeniedException("Somente nutricionista pode marcar como realizada.");
        }
        ap.setStatus(AppointmentStatus.COMPLETED);
        ap.setLastActionBy(AppointmentActor.NUTRITIONIST);
        return toDto(repository.save(ap));
    }

    @Transactional
    public AppointmentDto markNoShow(User nutri, Long appointmentId) {
        Appointment ap = fetchAuthorized(nutri, appointmentId);
        if (nutri.getRole() != Role.NUTRITIONIST) {
            throw new AccessDeniedException("Somente nutricionista pode marcar como faltou.");
        }
        ap.setStatus(AppointmentStatus.NO_SHOW);
        ap.setLastActionBy(AppointmentActor.NUTRITIONIST);
        return toDto(repository.save(ap));
    }

    // -------------------- Helpers --------------------

    private Appointment fetchAuthorized(User actor, Long appointmentId) {
        Appointment ap = repository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Consulta nao encontrada"));
        Long uid = actor.getId();
        boolean isClient = ap.getClient() != null && ap.getClient().getId().equals(uid);
        boolean isNutri  = ap.getNutritionist() != null && ap.getNutritionist().getId().equals(uid);
        if (!isClient && !isNutri) {
            throw new AccessDeniedException("Voce nao participa desta consulta.");
        }
        return ap;
    }

    private AppointmentActor roleOf(User actor, Appointment ap) {
        if (ap.getClient() != null && ap.getClient().getId().equals(actor.getId())) return AppointmentActor.CLIENT;
        return AppointmentActor.NUTRITIONIST;
    }

    private NutritionistClientLink findActiveLink(Long clientId, Long nutritionistId) {
        // Reaproveita a lista de clientes do nutri (ja checa ACTIVE + practice)
        return linkService.findActiveLinkBetween(nutritionistId, clientId)
                .orElseThrow(() -> new AccessDeniedException("Voce nao tem vinculo ativo com este nutricionista."));
    }

    private NutritionistClientLink findActiveLinkForNutri(Long nutritionistId, Long clientId, Long practiceId) {
        return linkService.findActiveLinkBetween(nutritionistId, clientId)
                .filter(l -> practiceId == null
                          || (l.getPractice() != null && l.getPractice().getId().equals(practiceId)))
                .orElseThrow(() -> new AccessDeniedException("Voce nao atende este cliente nesta practice."));
    }

    // -------------------- Fase 9F: lembretes automaticos --------------------

    /**
     * Job horario: busca appointments CONFIRMED que ocorrem nas proximas 24h
     * e ainda nao receberam lembrete. Dispara notificacao in-app + email pra
     * cliente e nutri; marca {@code reminderSentAt} pra evitar reenvio.
     *
     * A janela e [agora, agora+24h] com margem inferior de 1h pra dar tempo
     * de agendar na noite anterior sem lembrete duplo (executa a cada hora).
     */
    @Scheduled(cron = "0 5 * * * *") // 5min apos cada hora
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(24);
        List<Appointment> due = repository.findConfirmedDueForReminder(
                AppointmentStatus.CONFIRMED, now, windowEnd);
        if (due.isEmpty()) return;
        log.info("Enviando {} lembretes de consulta ({} a {})", due.size(), now, windowEnd);
        for (Appointment ap : due) {
            try {
                String when = fmt(ap.getScheduledAt());
                if (ap.getClient() != null) {
                    notify(ap.getClient().getId(),
                            "Lembrete: consulta amanha",
                            "Voce tem consulta com " + safeName(ap.getNutritionist()) + " em " + when + ".",
                            ap.getId());
                }
                if (ap.getNutritionist() != null) {
                    notify(ap.getNutritionist().getId(),
                            "Lembrete: consulta amanha",
                            "Voce tem consulta com " + safeName(ap.getClient()) + " em " + when + ".",
                            ap.getId());
                }
                ap.setReminderSentAt(now);
                repository.save(ap);
            } catch (Exception e) {
                // Falha em um nao pode parar o job
                log.warn("Falha ao enviar lembrete de appointment {}: {}", ap.getId(), e.getMessage());
            }
        }
    }

    private String safeName(User u) {
        return u != null && u.getName() != null ? u.getName() : "(usuario)";
    }

    private void notify(Long userId, String title, String body, Long appointmentId) {
        notificationService.notify(userId, NotificationType.GENERIC, title, body, "Appointment", appointmentId);
    }

    private void notifyCounterparty(Appointment ap, AppointmentActor actorRole, String title, String body) {
        Long targetId = actorRole == AppointmentActor.CLIENT
                ? ap.getNutritionist().getId()
                : ap.getClient().getId();
        notify(targetId, title, body, ap.getId());
    }

    private String fmt(LocalDateTime at) {
        return at == null ? "-" : at.toString().replace('T', ' ');
    }

    private AppointmentDto toDto(Appointment a) {
        NutritionistPractice p = a.getPractice();
        Company comp = p != null ? p.getCompany() : null;
        return new AppointmentDto(
                a.getId(),
                a.getClient() != null ? a.getClient().getId() : null,
                a.getClient() != null ? a.getClient().getName() : null,
                a.getClient() != null ? a.getClient().getEmail() : null,
                a.getNutritionist() != null ? a.getNutritionist().getId() : null,
                a.getNutritionist() != null ? a.getNutritionist().getName() : null,
                p != null ? p.getId() : null,
                comp != null ? comp.getId() : null,
                comp != null ? comp.getName() : null,
                a.getScheduledAt(),
                a.getDurationMinutes(),
                a.getLocation(),
                a.getLocationDetails(),
                a.getMeetingUrl(),
                a.getReminderSentAt(),
                a.getNotes(),
                a.getStatus(),
                a.getRequestedBy(),
                a.getLastActionBy(),
                a.getProposedScheduledAt(),
                a.getProposedBy(),
                a.getCancelReason(),
                a.getCanceledAt(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
