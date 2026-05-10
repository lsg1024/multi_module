package com.msa.jewelry.user.internal.domain.respository;

import com.msa.jewelry.user.internal.domain.entity.SensConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensConfigRepository extends JpaRepository<SensConfig, Long> {

    Optional<SensConfig> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);
}
