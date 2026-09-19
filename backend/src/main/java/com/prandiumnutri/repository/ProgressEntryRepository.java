package com.prandiumnutri.repository;

import com.prandiumnutri.domain.ProgressEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProgressEntryRepository extends JpaRepository<ProgressEntry, Long> {
    List<ProgressEntry> findByUserIdOrderByDateAsc(Long userId);
    List<ProgressEntry> findByUserIdAndDateGreaterThanEqualOrderByDateAsc(Long userId, LocalDate since);
    Optional<ProgressEntry> findFirstByUserIdOrderByDateDesc(Long userId);
    Optional<ProgressEntry> findFirstByUserIdOrderByDateAsc(Long userId);
}
