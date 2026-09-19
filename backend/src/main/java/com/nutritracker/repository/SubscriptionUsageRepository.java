package com.nutritracker.repository;

import com.nutritracker.domain.SubscriptionUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionUsage, Long> {
    Optional<SubscriptionUsage> findBySubscriptionIdAndFeatureKeyAndPeriodYyyymm(
            Long subscriptionId, String featureKey, String periodYyyymm);
}
