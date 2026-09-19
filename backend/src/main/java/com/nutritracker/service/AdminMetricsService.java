package com.nutritracker.service;

import com.nutritracker.domain.Company;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Role;
import com.nutritracker.domain.Subscription;
import com.nutritracker.domain.SubscriptionStatus;
import com.nutritracker.domain.User;
import com.nutritracker.dto.AdminMetricsDto;
import com.nutritracker.dto.AdminMetricsDto.ActionCountDto;
import com.nutritracker.dto.AdminMetricsDto.Overview;
import com.nutritracker.dto.AdminMetricsDto.TopCompanyDto;
import com.nutritracker.dto.AdminMetricsDto.TopNutriDto;
import com.nutritracker.repository.AuditLogRepository;
import com.nutritracker.repository.CompanyRepository;
import com.nutritracker.repository.NutritionistClientLinkRepository;
import com.nutritracker.repository.SubscriptionRepository;
import com.nutritracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMetricsService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NutritionistClientLinkRepository linkRepository;
    private final AuditLogRepository auditRepository;
    private final FxRateService fxRateService;

    public Overview overview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime day30 = now.minusDays(30);
        LocalDateTime day7 = now.minusDays(7);
        LocalDateTime day1 = now.minusDays(1);

        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        long totalNutris = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.NUTRITIONIST).count();

        long activeSubs = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long newUsers30d = userRepository.countByCreatedAtAfter(day30);
        long dau = userRepository.countByLastSeenAtAfter(day1);
        long wau = userRepository.countByLastSeenAtAfter(day7);
        long mau = userRepository.countByLastSeenAtAfter(day30);

        Map<String, Long> mrrByCurrency = new LinkedHashMap<>();
        long mrrBrl = 0;
        for (SubscriptionRepository.CurrencyTotal ct :
                subscriptionRepository.sumAmountByCurrency(SubscriptionStatus.ACTIVE)) {
            long amount = ct.getTotal() == null ? 0 : ct.getTotal();
            mrrByCurrency.put(ct.getCurrency(), amount);
            mrrBrl += fxRateService.toBrlCents(amount, ct.getCurrency());
        }

        Map<String, Long> planDist = subscriptionRepository.countByPlanCode(SubscriptionStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        SubscriptionRepository.PlanCount::getPlanCode,
                        SubscriptionRepository.PlanCount::getTotal,
                        (a, b) -> a,
                        LinkedHashMap::new));

        List<ActionCountDto> suggestions = auditRepository.countByActionSince(day30).stream()
                .map(r -> new ActionCountDto(r.getAction().name(), r.getCount()))
                .toList();

        List<TopCompanyDto> topCompanies = buildTopCompanies();
        List<TopNutriDto> topNutris = buildTopNutris();

        return new Overview(
                totalUsers, totalNutris, totalCompanies, activeSubs,
                dau, wau, mau, newUsers30d,
                mrrBrl, mrrByCurrency, planDist,
                suggestions, topCompanies, topNutris
        );
    }

    private List<TopCompanyDto> buildTopCompanies() {
        List<Subscription> topSubs = subscriptionRepository.topByAmount(
                SubscriptionStatus.ACTIVE, PlanTarget.COMPANY, PageRequest.of(0, 10));
        return topSubs.stream()
                .map(s -> {
                    Company c = companyRepository.findById(s.getPayerId()).orElse(null);
                    String name = c != null ? c.getName() : ("Company #" + s.getPayerId());
                    return new TopCompanyDto(s.getPayerId(), name, s.getAmountCents(), s.getCurrency());
                })
                .toList();
    }

    private List<TopNutriDto> buildTopNutris() {
        return linkRepository.topNutrisByActiveClients(PageRequest.of(0, 10)).stream()
                .map(row -> {
                    User u = userRepository.findById(row.getNutriId()).orElse(null);
                    String name = u != null ? u.getName() : ("Nutri #" + row.getNutriId());
                    return new TopNutriDto(row.getNutriId(), name, row.getTotal());
                })
                .toList();
    }
}
