package com.prandiumnutri.repository;

import com.prandiumnutri.domain.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByClientIdOrderByConsultationDateDesc(Long clientId);
    List<Consultation> findByNutritionistIdAndClientIdOrderByConsultationDateDesc(Long nutriId, Long clientId);
}
