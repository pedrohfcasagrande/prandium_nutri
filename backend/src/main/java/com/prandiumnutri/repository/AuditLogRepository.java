package com.prandiumnutri.repository;

import com.prandiumnutri.domain.AuditAction;
import com.prandiumnutri.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId, Pageable pageable);

    Page<AuditLog> findByOnBehalfOfUserIdOrderByCreatedAtDesc(Long onBehalfOfUserId, Pageable pageable);

    @Query("""
            select a.action as action, count(a) as count
            from AuditLog a
            where a.createdAt >= :since
            group by a.action
            """)
    List<ActionCount> countByActionSince(@Param("since") LocalDateTime since);

    @Query("""
            select count(distinct a.actorUserId)
            from AuditLog a
            where a.createdAt >= :since and a.actorUserId is not null
            """)
    long countDistinctActorsSince(@Param("since") LocalDateTime since);

    @Query("""
            select count(a) from AuditLog a
            where a.action = :action
              and a.actorUserId = :userId
              and a.createdAt >= :since
            """)
    long countByActionAndActorSince(@Param("action") AuditAction action,
                                    @Param("userId") Long userId,
                                    @Param("since") LocalDateTime since);

    @Query("""
            select a.actorUserId as actorUserId, count(a) as total
            from AuditLog a
            where a.action = :action
              and a.actorUserId in :userIds
              and a.createdAt >= :since
            group by a.actorUserId
            """)
    List<ActorCount> countByActionAndActorsSince(@Param("action") AuditAction action,
                                                 @Param("userIds") java.util.Collection<Long> userIds,
                                                 @Param("since") LocalDateTime since);

    interface ActionCount {
        AuditAction getAction();
        Long getCount();
    }

    interface ActorCount {
        Long getActorUserId();
        Long getTotal();
    }
}
