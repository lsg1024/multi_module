package com.msa.jewelry.local.user.repository;

import com.msa.jewelry.local.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByUserId(String userId);

    Optional<Users> findByUserId(String userId);

    Optional<Users> findByIdAndTenantId(Long id, String tenantId);
}
