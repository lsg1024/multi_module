package com.msa.account.domain.store.repository;


import com.msa.account.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long>, CustomStoreRepository {
}
