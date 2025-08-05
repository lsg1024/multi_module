package com.msa.order.local.domain.order.repository;

import com.msa.order.local.domain.order.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
}
