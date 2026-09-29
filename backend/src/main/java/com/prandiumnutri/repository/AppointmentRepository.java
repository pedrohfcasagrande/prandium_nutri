package com.prandiumnutri.repository;

import com.prandiumnutri.domain.Appointment;
import com.prandiumnutri.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByClientIdOrderByScheduledAtDesc(Long clientId);

    List<Appointment> findByNutritionistIdOrderByScheduledAtDesc(Long nutritionistId);

    List<Appointment> findByPracticeIdInOrderByScheduledAtDesc(Collection<Long> practiceIds);

    List<Appointment> findByNutritionistIdAndStatusIn(Long nutritionistId, Collection<AppointmentStatus> statuses);

    /**
     * Fase 9F: appointments confirmados que ocorrem dentro da janela e ainda nao
     * receberam lembrete. Usado pelo job {@code sendReminders()}.
     */
    @Query("select a from Appointment a " +
           "where a.status = :status " +
           "and a.reminderSentAt is null " +
           "and a.scheduledAt between :from and :to")
    List<Appointment> findConfirmedDueForReminder(@Param("status") AppointmentStatus status,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
