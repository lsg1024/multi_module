package com.msa.userserver.domain.respository;

import com.msa.userserver.domain.entity.MessageHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {

    Page<MessageHistory> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
