package com.nutritracker.repository;

import com.nutritracker.domain.PaymentProvider;
import com.nutritracker.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByProviderAndProviderEventId(PaymentProvider provider, String providerEventId);
}
