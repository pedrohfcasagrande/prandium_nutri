package com.prandiumnutri.repository;

import com.prandiumnutri.domain.DailyFxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyFxRateRepository extends JpaRepository<DailyFxRate, Long> {

    Optional<DailyFxRate> findFirstByCurrencyOrderByRateDateDesc(String currency);
}
