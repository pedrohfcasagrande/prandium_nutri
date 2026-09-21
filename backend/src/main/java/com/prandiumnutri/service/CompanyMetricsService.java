package com.prandiumnutri.service;

import com.prandiumnutri.domain.AuditAction;
import com.prandiumnutri.domain.LinkStatus;
import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.Subscription;
import com.prandiumnutri.domain.SubscriptionStatus;
import com.prandiumnutri.domain.User;
import com.prandiumnutri.dto.CompanyMetricsDto;
import com.prandiumnutri.dto.CompanyMetricsDto.NutriRow;
import com.prandiumnutri.dto.CompanyMetricsDto.Overview;
import com.prandiumnutri.repository.AuditLogRepository;
import com.prandiumnutri.repository.CompanyNutritionistRepository;
import com.prandiumnutri.repository.NutritionistClientLinkRepository;
import com.prandiumnutri.repository.SubscriptionRepository;
import com.prandiumnutri.repository.UserRepository;
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
        for (Long nid : nutriIds) totalClients += countActiveClients(nid, companyId);

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

        // Meals dos clientes dos nutris ultimos 30d — apenas clientes vinculados
        // via practice DESTA empresa (Fase 8 fix: nao contar clientes particulares do nutri).
        long meals30d = 0;
        for (Long nid : nutriIds) {
            var clientIds = clientLinkRepository.findByNutritionistIdOrderByInvitedAtDesc(nid).stream()
                    .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                    .filter(l -> l.getPractice() != null
                              && l.getPractice().getCompany() != null
                              && l.getPractice().getCompany().getId().equals(companyId))
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
                .map(nid -> buildNutriRow(nid, aiByNutri.getOrDefault(nid, 0L), companyId))
                .toList();

        return new Overview(
                activeNutris, pendingNutris, totalClients,
                aiThisMonth, meals30d, subCents, currency, rows);
    }

    /** Conta clientes ativos do nutri vinculados via practice desta empresa (Fase 8 fix). */
    private long countActiveClients(Long nutriId, Long companyId) {
        return clientLinkRepository.findByNutritionistIdOrderByInvitedAtDesc(nutriId).stream()
                .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
                .filter(l -> l.getPractice() != null
                          && l.getPractice().getCompany() != null
                          && l.getPractice().getCompany().getId().equals(companyId))
                .count();
    }

    private NutriRow buildNutriRow(Long nutriId, long aiUsage, Long companyId) {
        User nutri = userRepository.findById(nutriId).orElse(null);
        String name = nutri != null ? nutri.getName() : ("Nutri #" + nutriId);
        String email = nutri != null ? nutri.getEmail() : "";
        LocalDate lastSeen = nutri != null && nutri.getLastSeenAt() != null
                ? nutri.getLastSeenAt().toLocalDate() : null;
        return new NutriRow(nutriId, name, email, countActiveClients(nutriId, companyId),
                aiUsage, lastSeen != null ? lastSeen.toString() : null);
    }
}
