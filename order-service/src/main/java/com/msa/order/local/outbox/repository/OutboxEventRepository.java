package com.msa.order.local.outbox.repository;

import com.msa.common.global.redis.enum_type.EventStatus;
import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query("""
        SELECT e FROM OutboxEvent e 
        WHERE e.status = :status 
        AND e.eventType = :eventType
        AND (e.nextRetryAt IS NULL OR e.nextRetryAt < CURRENT_TIMESTAMP)
        ORDER BY e.createDate ASC
        """)
    List<OutboxEvent> findPendingEventsByType(
            @Param("status") EventStatus status,
            @Param("eventType") String eventType,
            Pageable pageable
    );

    @Query("""
        SELECT e FROM OutboxEvent e 
        WHERE e.status = :status 
        AND (e.nextRetryAt IS NULL OR e.nextRetryAt < CURRENT_TIMESTAMP)
        ORDER BY e.createDate ASC
        """)
    List<OutboxEvent> findRetryableEvents(
            @Param("status") EventStatus status,
            Pageable pageable
    );

}
