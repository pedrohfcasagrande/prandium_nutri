package com.prandiumnutri.repository;

import com.prandiumnutri.domain.AssessmentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentSnapshotRepository extends JpaRepository<AssessmentSnapshot, Long> {

    List<AssessmentSnapshot> findByAssessmentIdOrderByVersionDesc(Long assessmentId);

    Optional<AssessmentSnapshot> findFirstByAssessmentIdOrderByVersionDesc(Long assessmentId);
}
