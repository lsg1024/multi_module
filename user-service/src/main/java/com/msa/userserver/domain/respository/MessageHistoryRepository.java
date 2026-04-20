package com.msa.userserver.domain.respository;

import com.msa.userserver.domain.entity.MessageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {

    Page<MessageHistory> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    /**
     * 테넌트별 전송 이력 검색 — 수신자/전화번호/내용 LIKE + 날짜 범위 조건을 모두 선택적으로 적용한다.
     * 빈 문자열·null 파라미터는 조건 무시.
     */
    @Query("SELECT h FROM MessageHistory h " +
            "WHERE h.tenantId = :tenantId " +
            "AND (:receiverName IS NULL OR :receiverName = '' OR LOWER(h.receiverName) LIKE LOWER(CONCAT('%', :receiverName, '%'))) " +
            "AND (:receiverPhone IS NULL OR :receiverPhone = '' OR REPLACE(h.receiverPhone, '-', '') LIKE CONCAT('%', REPLACE(:receiverPhone, '-', ''), '%')) " +
            "AND (:content IS NULL OR :content = '' OR LOWER(h.content) LIKE LOWER(CONCAT('%', :content, '%'))) " +
            "AND (:startDateTime IS NULL OR h.createdAt >= :startDateTime) " +
            "AND (:endDateTime IS NULL OR h.createdAt <= :endDateTime) " +
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
