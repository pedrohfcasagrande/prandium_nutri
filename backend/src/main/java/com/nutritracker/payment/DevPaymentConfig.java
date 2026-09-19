package com.nutritracker.payment;

import com.nutritracker.domain.PaymentProvider;
import com.nutritracker.repository.CompanyRepository;
import com.nutritracker.repository.SubscriptionRepository;
import com.nutritracker.service.PricingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Beans de pagamento MOCK. Ativados apenas em dev/test.
 * Em sandbox/prod, os beans reais (StripePaymentClient / MercadoPagoPaymentClient) sao ativados.
 */
@Configuration
@Profile({"dev", "test"})
@Slf4j
public class DevPaymentConfig {

    @Bean
    public PaymentProviderClient mockStripeClient(SubscriptionRepository sub,
                                                   PricingService pricing,
                                                   CompanyRepository company) {
        log.info("[DEV] MockPaymentClient para STRIPE ativado");
        return new MockPaymentClient(PaymentProvider.STRIPE, sub, pricing, company);
    }

    @Bean
    public PaymentProviderClient mockMercadoPagoClient(SubscriptionRepository sub,
                                                        PricingService pricing,
                                                        CompanyRepository company) {
        log.info("[DEV] MockPaymentClient para MERCADO_PAGO ativado");
        return new MockPaymentClient(PaymentProvider.MERCADO_PAGO, sub, pricing, company);
    }
}
