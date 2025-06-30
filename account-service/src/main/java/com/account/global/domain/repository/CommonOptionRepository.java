package com.account.global.domain.repository;

import com.account.global.domain.entity.CommonOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CommonOptionRepository extends JpaRepository<CommonOption, Long> {
}
