package com.prandiumnutri.repository;

import com.prandiumnutri.domain.Plan;
import com.prandiumnutri.domain.PlanTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, String> {
    List<Plan> findByTargetAndActiveTrueOrderBySortOrderAsc(PlanTarget target);
    List<Plan> findByActiveTrueOrderByTargetAscSortOrderAsc();
}
