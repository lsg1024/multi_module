package com.msa.account.global.domain.repository;

import com.msa.account.global.domain.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BusinessRepository extends JpaRepository<Business, Long> {
}
