package com.msa.account.local.store.repository;


import com.msa.account.local.store.entity.Store;
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

}
