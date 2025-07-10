package com.msa.account.domain.factory.repository;

import com.msa.account.domain.factory.entity.Factory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactoryRepository extends JpaRepository<Factory, Long>, CustomFactoryRepository {
    boolean existsByFactoryName(String factoryName);
}
