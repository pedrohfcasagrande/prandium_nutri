package com.prandiumnutri.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prandiumnutri.domain.Plan;
import com.prandiumnutri.domain.PlanPrice;
import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.dto.PlanDto;
import com.prandiumnutri.dto.PricingDto;
import com.prandiumnutri.repository.PlanPriceRepository;
import com.prandiumnutri.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PlanRepository planRepository;
    private final PlanPriceRepository priceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public PricingDto pricing(String currency, PlanTarget target) {
        String cur = currency.toUpperCase();
        List<Plan> plans = target != null
                ? planRepository.findByTargetAndActiveTrueOrderBySortOrderAsc(target)
                : planRepository.findByActiveTrueOrderByTargetAscSortOrderAsc();

        Map<String, PlanPrice> byPlanCode = new HashMap<>();
        for (PlanPrice p : priceRepository.findByCurrencyAndActiveTrue(cur)) {
            byPlanCode.put(p.getPlan().getCode(), p);
        }

        List<PlanDto> dtos = plans.stream().map(p -> {
            PlanPrice price = byPlanCode.get(p.getCode());
            return new PlanDto(p.getCode(), p.getTarget(), p.getTier(), p.getName(),
                    parseFeatures(p.getFeatures()),
                    cur,
                    price != null ? price.getAmountCents() : 0,
                    price != null ? price.getProvider() : null);
        }).toList();

        return new PricingDto(cur, dtos);
    }

    @Transactional(readOnly = true)
    public Plan getPlanOrThrow(String code) {
        return planRepository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Plano nao encontrado: " + code));
    }

    @Transactional(readOnly = true)
    public PlanPrice getPriceOrThrow(String planCode, String currency) {
        return priceRepository.findByPlanCodeAndCurrencyAndActiveTrue(planCode, currency.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plano " + planCode + " nao disponivel em " + currency));
    }

    public Map<String, Object> parseFeatures(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
