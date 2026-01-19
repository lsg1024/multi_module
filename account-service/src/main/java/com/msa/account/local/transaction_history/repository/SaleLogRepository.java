package com.msa.account.local.transaction_history.repository;

import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SaleLogRepository extends JpaRepository<SaleLog, Long> {
    Optional<SaleLog> findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateDesc(Long saleCode, Long storeId);
    Optional<SaleLog> findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateAsc(Long saleCode, Long storeId);
    Optional<SaleLog> findTopByAccountSaleCodeAndFactory_FactoryIdOrderBySaleDateDesc(Long saleCode, Long factoryId);
    Optional<SaleLog> findTopByStoreAndOwnerTypeOrderBySaleDateDesc(Store store, String ownerType);
    Optional<SaleLog> findTopByFactoryAndOwnerTypeOrderBySaleDateDesc(Factory factory, String ownerType);
    @Query("""
        SELECT COUNT(s) > 0 
        FROM SaleLog s 
        WHERE s.store.storeId = :storeId 
          AND (
              s.saleDate > :currentDate 
              OR 
              (s.saleDate = :currentDate AND s.id > :currentId)
          )
    """)
    boolean existsFutureLog(@Param("storeId") Long storeId,
                            @Param("currentDate") LocalDateTime currentDate,
                            @Param("currentId") Long currentId);
}