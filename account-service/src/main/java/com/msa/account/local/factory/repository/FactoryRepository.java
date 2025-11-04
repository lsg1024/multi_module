package com.msa.account.local.factory.repository;


import com.msa.account.local.factory.entity.Factory;
import com.msa.account.local.transaction.domain.dto.TransactionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FactoryRepository extends JpaRepository<Factory, Long>, CustomFactoryRepository {
    boolean existsByFactoryName(String factoryName);
    @Query("select f from Factory f " +
            "join fetch f.commonOption co " +
            "join fetch co.goldHarry gh " +
            "join fetch f.address a " +
            "where f.factoryId = :factoryId")
    Optional<Factory> findWithAllOptionById(@Param("factoryId") Long factoryId);

    @Query("select f.currentGoldBalance, f.currentMoneyBalance from Factory f " +
            "where f.factoryId= :factoryId " +
            "and f.factoryName= :factoryName")
    TransactionDto findByFactoryIdAndFactoryName(@Param("factoryId") Long factoryId, @Param("factoryName") String factoryName);
}
