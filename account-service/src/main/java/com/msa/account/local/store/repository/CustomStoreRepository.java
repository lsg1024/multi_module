package com.msa.account.local.store.repository;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomStoreRepository {

    Optional<StoreDto.StoreSingleResponse> findByStoreId(Long storeId);
    CustomPage<StoreDto.StoreResponse> findAllStore(String name, Pageable pageable);

    CustomPage<AccountDto.accountResponse> findAllStoreAndAttempt(String name, Pageable pageable);
    AccountDto.accountResponse findByStoreIdAndAttempt(Long storeId);
}
