package com.msa.jewelry.local.user.repository;

import com.msa.jewelry.local.user.entity.SensConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensConfigRepository extends JpaRepository<SensConfig, Long> {

    Optional<SensConfig> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);
}
