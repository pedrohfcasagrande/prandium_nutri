package com.nutritracker.repository;

import com.nutritracker.domain.MenuPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuPlanRepository extends JpaRepository<MenuPlan, Long> {
    List<MenuPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<MenuPlan> findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("update MenuPlan m set m.active = false where m.user.id = :userId")
    void deactivateAllForUser(@Param("userId") Long userId);

    List<MenuPlan> findBySubmittedToNutriIdAndStatusOrderBySubmittedAtDesc(
            Long nutriId, com.nutritracker.domain.MenuPlanStatus status);
}
