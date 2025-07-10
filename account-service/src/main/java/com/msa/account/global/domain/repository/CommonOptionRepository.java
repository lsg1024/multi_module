package com.msa.account.global.domain.repository;

import com.msa.account.global.domain.entity.CommonOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

public interface CommonOptionRepository extends JpaRepository<CommonOption, Long> {
}
