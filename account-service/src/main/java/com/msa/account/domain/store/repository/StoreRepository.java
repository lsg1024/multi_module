package com.msa.account.domain.store.repository;


import com.msa.account.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long>, CustomStoreRepository {
    boolean existsByStoreName(String storeName);
}
