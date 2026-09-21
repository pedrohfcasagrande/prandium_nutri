package com.prandiumnutri.repository;

import com.prandiumnutri.domain.ClientAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientAssessmentRepository extends JpaRepository<ClientAssessment, Long> {

    Optional<ClientAssessment> findByNutritionistIdAndClientId(Long nutritionistId, Long clientId);

    List<ClientAssessment> findByClientIdOrderByUpdatedAtDesc(Long clientId);
}
