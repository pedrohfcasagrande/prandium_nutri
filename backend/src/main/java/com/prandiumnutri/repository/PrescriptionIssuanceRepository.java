package com.prandiumnutri.repository;

import com.prandiumnutri.domain.PrescriptionIssuance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionIssuanceRepository extends JpaRepository<PrescriptionIssuance, Long> {
    List<PrescriptionIssuance> findByAssessmentIdOrderByIssuedAtDesc(Long assessmentId);
    List<PrescriptionIssuance> findByClientIdOrderByIssuedAtDesc(Long clientId);
    Optional<PrescriptionIssuance> findFirstByPdfSha256(String hash);
}
