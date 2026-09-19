package com.prandiumnutri.payment;

import com.prandiumnutri.domain.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentProviderRegistry {

    private final List<PaymentProviderClient> clients;

    public PaymentProviderClient forProvider(PaymentProvider provider) {
        return clients.stream()
                .filter(c -> c.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Provider nao encontrado: " + provider));
    }
}
