package com.msa.order.local.domain.order.repository;

import com.msa.order.local.domain.order.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    Optional<StatusHistory> findTopByFlowCodeOrderByIdDesc(Long flowCode);

    List<StatusHistory> findAllByFlowCode(Long flowCode);
}
