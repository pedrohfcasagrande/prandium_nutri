package com.nutritracker.service;

import com.nutritracker.domain.AuditAction;
import com.nutritracker.domain.LinkStatus;
import com.nutritracker.domain.PlanTarget;
import com.nutritracker.domain.Subscription;
import com.nutritracker.domain.SubscriptionStatus;
import com.nutritracker.domain.User;
import com.nutritracker.dto.CompanyMetricsDto;
import com.nutritracker.dto.CompanyMetricsDto.NutriRow;
import com.nutritracker.dto.CompanyMetricsDto.Overview;
import com.nutritracker.repository.AuditLogRepository;
import com.nutritracker.repository.CompanyNutritionistRepository;
import com.nutritracker.repository.NutritionistClientLinkRepository;
import com.nutritracker.repository.SubscriptionRepository;
import com.nutritracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyMetricsService {

    private final CompanyNutritionistRepository companyNutriRepository;
    private final NutritionistClientLinkRepository clientLinkRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Overview overview(Long companyId) {
        List<Long> nutriIds = companyNutriRepository.activeNutritionistIds(companyId);
        long activeNutris = nutriIds.size();
        long pendingNutris = companyNutriRepository.findByCompanyIdOrderByInvitedAtDesc(companyId).stream()
                .filter(cn -> cn.getStatus() == LinkStatus.PENDING).count();

        long totalClients = 0;
        for (Long nid : nutriIds) totalClients += countActiveClients(nid);

        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime day30Ago = LocalDateTime.now().minusDays(30);

        long aiThisMonth = 0;
        Map<Long, Long> aiByNutri = new HashMap<>();
        if (!nutriIds.isEmpty()) {
            for (var row : auditRepository.countByActionAndActorsSince(
                    AuditAction.SUGGESTION_GENERATED, nutriIds, monthStart)) {
                aiByNutri.put(row.getActorUserId(), row.getTotal());
                aiThisMonth += row.getTotal();
            }
        }

        // MRR da assinatura da propria company
        Optional<Subscription> optSub = subscriptionRepository
                .findFirstByPayerTypeAndPayerIdAndStatusInOrderByStartedAtDesc(
                        PlanTarget.COMPANY, companyId,
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING));
        long subCents = optSub.map(s -> (long) s.getAmountCents()).orElse(0L);
        String currency = optSub.map(Subscription::getCurrency).orElse("BRL");

        // Meals dos clientes dos nutris ultimos 30d
        long meals30d = 0;
        for (Long nid : nutriIds) {
            var clientIds = clientLinkRepository.findByNutritionistIdOrderByInvitedAtDesc(nid).stream()
                    .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                    .map(l -> l.getClient() != null ? l.getClient().getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (!clientIds.isEmpty()) {
                // reaproveita audit log de MEAL_LOGGED
                for (var row : auditRepository.countByActionAndActorsSince(
                        AuditAction.MEAL_LOGGED, clientIds, day30Ago)) {
                    meals30d += row.getTotal();
                }
            }
        }

        List<NutriRow> rows = nutriIds.stream()
                .map(nid -> buildNutriRow(nid, aiByNutri.getOrDefault(nid, 0L)))
                .toList();

        return new Overview(
                activeNutris, pendingNutris, totalClients,
                aiThisMonth, meals30d, subCents, currency, rows);
    }

    private long countActiveClients(Long nutriId) {
        return clientLinkRepository.findByNutritionistIdOrderByInvitedAtDesc(nutriId).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE).count();
    }

    private NutriRow buildNutriRow(Long nutriId, long aiUsage) {
        User nutri = userRepository.findById(nutriId).orElse(null);
        String name = nutri != null ? nutri.getName() : ("Nutri #" + nutriId);
        String email = nutri != null ? nutri.getEmail() : "";
        LocalDate lastSeen = nutri != null && nutri.getLastSeenAt() != null
                ? nutri.getLastSeenAt().toLocalDate() : null;
        return new NutriRow(nutriId, name, email, countActiveClients(nutriId),
                aiUsage, lastSeen != null ? lastSeen.toString() : null);
    }
}
