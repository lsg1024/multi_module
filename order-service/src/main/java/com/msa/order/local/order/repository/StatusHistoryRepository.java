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

    /**
     * N+1 쿼리 문제 해결을 위한 배치 조회 메서드
     * 여러 flowCode에 대한 StatusHistory를 한 번의 쿼리로 조회합니다.
     *
     * @param flowCodes 조회할 flowCode 리스트
     * @return flowCode와 createAt 순으로 정렬된 StatusHistory 리스트
     */
    @Query("""
        SELECT s FROM StatusHistory s
        WHERE s.flowCode IN :flowCodes
        ORDER BY s.flowCode ASC, s.createAt ASC
        """)
    List<StatusHistory> findAllByFlowCodeInOrderByCreateAtAsc(@Param("flowCodes") List<Long> flowCodes);
}
