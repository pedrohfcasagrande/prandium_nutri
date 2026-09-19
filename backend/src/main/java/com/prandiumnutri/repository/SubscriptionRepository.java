package com.prandiumnutri.repository;

import com.prandiumnutri.domain.PlanTarget;
import com.prandiumnutri.domain.Subscription;
import com.prandiumnutri.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByPayerTypeAndPayerIdOrderByStartedAtDesc(PlanTarget payerType, Long payerId);

    Optional<Subscription> findFirstByPayerTypeAndPayerIdAndStatusInOrderByStartedAtDesc(
            PlanTarget payerType, Long payerId, List<SubscriptionStatus> statuses);

    long countByStatus(SubscriptionStatus status);

    @Query("""
           select s.currency as currency, sum(s.amountCents) as total
           from Subscription s
           where s.status = :status and s.amountCents > 0
           group by s.currency
           """)
    List<CurrencyTotal> sumAmountByCurrency(@Param("status") SubscriptionStatus status);

    @Query("""
           select s.plan.code as planCode, count(s) as total
           from Subscription s
           where s.status = :status
           group by s.plan.code
           """)
    List<PlanCount> countByPlanCode(@Param("status") SubscriptionStatus status);

    @Query("""
           select s from Subscription s
           where s.status = :status and s.payerType = :payerType and s.amountCents > 0
           order by s.amountCents desc
           """)
    List<Subscription> topByAmount(@Param("status") SubscriptionStatus status,
                                   @Param("payerType") PlanTarget payerType,
                                   org.springframework.data.domain.Pageable pageable);

    interface CurrencyTotal {
        String getCurrency();
        Long getTotal();
    }

    interface PlanCount {
        String getPlanCode();
        Long getTotal();
    }
}
