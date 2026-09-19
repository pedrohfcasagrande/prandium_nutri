package com.nutritracker.repository;

import com.nutritracker.domain.Plan;
import com.nutritracker.domain.PlanTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, String> {
    List<Plan> findByTargetAndActiveTrueOrderBySortOrderAsc(PlanTarget target);
    List<Plan> findByActiveTrueOrderByTargetAscSortOrderAsc();
}
