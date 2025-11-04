package com.msa.account.local.store.repository;


import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.transaction_history.domain.dto.TransactionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long>, CustomStoreRepository {
    @Query("""
      select s
      from Store s
      join fetch s.commonOption co
      where s.storeId = :id and s.storeDeleted = false
    """)
    Optional<Store> findByStoreInfo(@Param("id") Long id);
    boolean existsByStoreName(String storeName);
    @Query("select s from Store s " +
            "join fetch s.commonOption co " +
            "join fetch co.goldHarry gh " +
            "join fetch s.additionalOption ao " +
            "join fetch s.address a " +
            "where s.storeId = :storeId")
    Optional<Store> findWithAllOptionsById(@Param("storeId") Long storeId);
    @Query("select s.commonOption.optionLevel " +
            "from Store s " +
            "where s.storeId = :storeId")
    OptionLevel findByCommonOptionOptionLevel(@Param("storeId") Long storeId);

    @Query("select s.currentGoldBalance, s.currentMoneyBalance from Store s " +
            "where s.storeId= :storeId " +
            "and s.storeName= :storeName")
    TransactionDto findByStoreIdAndStoreName(@Param("storeId") Long storeId, @Param("storeName") String storeName);
}
