package com.prandiumnutri.repository;

import com.prandiumnutri.domain.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, Long> {
    Optional<PlanPrice> findByPlanCodeAndCurrencyAndActiveTrue(String planCode, String currency);
    List<PlanPrice> findByCurrencyAndActiveTrue(String currency);
}
