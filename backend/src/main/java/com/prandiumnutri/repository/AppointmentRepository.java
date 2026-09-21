package com.prandiumnutri.repository;

import com.prandiumnutri.domain.Appointment;
import com.prandiumnutri.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByClientIdOrderByScheduledAtDesc(Long clientId);

    List<Appointment> findByNutritionistIdOrderByScheduledAtDesc(Long nutritionistId);

    List<Appointment> findByPracticeIdInOrderByScheduledAtDesc(Collection<Long> practiceIds);

    List<Appointment> findByNutritionistIdAndStatusIn(Long nutritionistId, Collection<AppointmentStatus> statuses);
}
