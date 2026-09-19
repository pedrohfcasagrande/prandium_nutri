package com.prandiumnutri.repository;

import com.prandiumnutri.domain.PaymentProvider;
import com.prandiumnutri.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByProviderAndProviderEventId(PaymentProvider provider, String providerEventId);
}
