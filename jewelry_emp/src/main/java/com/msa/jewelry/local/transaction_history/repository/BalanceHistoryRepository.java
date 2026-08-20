package com.msa.jewelry.local.transaction_history.repository;

import com.msa.jewelry.local.transaction_history.entity.BalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, Long> {

    /** 멱등 가드 — 동일 이벤트가 이미 기록됐는지. */
    boolean existsByEventId(String eventId);
}
