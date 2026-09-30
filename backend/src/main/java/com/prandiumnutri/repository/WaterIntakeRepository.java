package com.prandiumnutri.repository;

import com.prandiumnutri.domain.WaterIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WaterIntakeRepository extends JpaRepository<WaterIntake, Long> {

    List<WaterIntake> findByUserIdAndDateOrderByLoggedAtAsc(Long userId, LocalDate date);

    List<WaterIntake> findByUserIdOrderByLoggedAtDesc(Long userId);

    @Query("select coalesce(sum(w.amountMl), 0) from WaterIntake w where w.user.id = :userId and w.date = :date")
    long sumMlByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
