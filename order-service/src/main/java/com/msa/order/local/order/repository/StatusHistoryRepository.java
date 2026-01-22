package com.msa.order.local.order.repository;

import com.msa.order.local.order.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    Optional<StatusHistory> findTopByFlowCodeOrderByIdDesc(Long flowCode);
    @Query("""
    select distinct s from StatusHistory s
     where s.flowCode in :ids
    """)
    List<StatusHistory> findTopByFlowCodeOrderByIdDescIn(@Param("ids") List<Long> flowCodes);
    List<StatusHistory> findAllByFlowCodeOrderByCreateAtAsc(Long flowCode);
}
