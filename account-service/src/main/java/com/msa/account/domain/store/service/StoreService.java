package com.msa.account.domain.store.service;

import com.msa.account.domain.store.repository.StoreRepository;
import com.msa.account.global.domain.dto.AccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public AccountDto.accountInfo getStoreInfo(String storeId) {
        AccountDto.accountInfo storeInfo = storeRepository.findByStoreId(Long.valueOf(storeId))
                .orElseThrow(() -> new RuntimeException("대상을 찾을 수 없습니다."));

        return AccountDto.accountInfo.builder()
                .createAt(storeInfo.getCreateAt())
                .manager(storeInfo.getManager())
                .businessName(storeInfo.getBusinessName())
                .businessOwnerName(storeInfo.getBusinessOwnerName())
                .businessNumber1(storeInfo.getBusinessNumber1())
                .businessNumber2(storeInfo.getBusinessNumber2())
                .faxNumber(storeInfo.getFaxNumber())
                .tradePlace(storeInfo.getAddress())
                .note(storeInfo.getNote())
                .level(storeInfo.getLevel())
                .tradeType(storeInfo.getTradeType())
                .goldLoss(storeInfo.getGoldLoss())
                .build();
    }

    public void getStoreList(String accessToken) {

    }

    //상점 호출(info)

    //상점 목록 호출(infoList)

    //상점 추가

    //상점 수정

    //상점 삭제

}
