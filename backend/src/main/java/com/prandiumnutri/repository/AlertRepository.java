package com.prandiumnutri.repository;

import com.prandiumnutri.domain.Alert;
import com.prandiumnutri.domain.AlertScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByScopeAndScopeIdAndDismissedAtIsNullOrderByCreatedAtDesc(AlertScope scope, Long scopeId);

    List<Alert> findByScopeAndDismissedAtIsNullOrderByCreatedAtDesc(AlertScope scope);

    boolean existsByKindAndScopeAndScopeIdAndCreatedAtAfter(
            String kind, AlertScope scope, Long scopeId, LocalDateTime after);
}
