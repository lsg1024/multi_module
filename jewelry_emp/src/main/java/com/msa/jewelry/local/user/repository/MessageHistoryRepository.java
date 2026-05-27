package com.msa.jewelry.local.user.repository;

import com.msa.jewelry.local.user.entity.MessageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {

    Page<MessageHistory> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    // 테넌트별 전송 이력 검색 — 수신자/전화번호/내용 LIKE + 날짜 범위 조건을 모두 선택적으로 적용.
    // 빈 문자열·null 파라미터는 조건 무시.
    //
    // [중요] PostgreSQL 파라미터 타입 추론 한계 회피:
    //   기존 "(:startDateTime IS NULL OR h.createdAt >= :startDateTime)" 패턴에서 양쪽 ? 가
    //   모두 null 일 때 PostgreSQL 이 TIMESTAMP 타입을 결정하지 못해
    //   SQLState 42P18 "could not determine data type of parameter $11" 로 터졌다.
    //   COALESCE 로 null 분기를 SQL 내부에서 해결해 ? 가 h.createdAt 와 비교되는
    //   TIMESTAMP 컨텍스트에 명시적으로 들어가도록 한다.
    @Query("SELECT h FROM MessageHistory h " +
            "WHERE h.tenantId = :tenantId " +
            "AND (:receiverName IS NULL OR :receiverName = '' OR LOWER(h.receiverName) LIKE LOWER(CONCAT('%', :receiverName, '%'))) " +
            "AND (:receiverPhone IS NULL OR :receiverPhone = '' OR REPLACE(h.receiverPhone, '-', '') LIKE CONCAT('%', REPLACE(:receiverPhone, '-', ''), '%')) " +
            "AND (:content IS NULL OR :content = '' OR LOWER(h.content) LIKE LOWER(CONCAT('%', :content, '%'))) " +
            "AND h.createdAt >= COALESCE(:startDateTime, h.createdAt) " +
            "AND h.createdAt <= COALESCE(:endDateTime, h.createdAt) " +
            "ORDER BY h.createdAt DESC")
    Page<MessageHistory> searchByTenantId(
            @Param("tenantId") String tenantId,
            @Param("receiverName") String receiverName,
            @Param("receiverPhone") String receiverPhone,
            @Param("content") String content,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);
}
