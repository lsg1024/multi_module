package com.account.domain.store.repository;

import com.account.global.domain.dto.AccountDto;
import com.msacommon.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

public interface CustomStoreRepository {
    AccountDto.accountInfo findByStoreId(Long storeId);
    CustomPage<AccountDto.accountInfo> findAllStore(Pageable pageable);
}
