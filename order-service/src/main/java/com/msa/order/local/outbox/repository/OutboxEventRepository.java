package com.msa.order.local.outbox.repository;

import com.msa.order.local.outbox.domain.entity.OutboxEvent;
import com.msa.common.global.redis.enum_type.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatus(EventStatus eventStatus);
}
