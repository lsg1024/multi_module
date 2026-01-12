package com.msa.account.local.transaction_history.repository;

import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaleLogRepository extends JpaRepository<SaleLog, Long> {
    Optional<SaleLog> findByAccountSaleCodeAndStore_StoreId(Long saleCode, Long storeId);
    Optional<SaleLog> findTopByStoreAndOwnerTypeOrderBySaleDateDesc(Store store, String ownerType);
    Optional<SaleLog> findTopByFactoryAndOwnerTypeOrderBySaleDateDesc(Factory factory, String ownerType);
    boolean existsByAccountSaleCodeAndStoreAndOwnerType(Long accountSaleCode, Store store, String ownerType);
    boolean existsByAccountSaleCodeAndFactoryAndOwnerType(Long accountSaleCode, Factory factory, String ownerType);
}