package com.account.domain.store.service;

import com.account.domain.store.entity.Store;
import com.account.domain.store.repository.StoreRepository;
import com.account.global.domain.dto.AccountDto;
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
        Store storeInfo = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new RuntimeException("대상을 찾을 수 없습니다."));



        return null;
//        return AccountDto.accountInfo.builder()
//                .createAt(storeInfo.getCreateDate())
//                .manager(storeInfo.getCreatedBy())
//                .businessName(storeInfo.getStoreName())
//                .businessOwnerName(storeInfo.getStoreOwnerName())
//                .businessNumber1(storeInfo.getStoreContactNumber1())
//                .businessNumber2(storeInfo.getStoreContactNumber2())
//                .faxNumber(storeInfo.getStoreFaxNumber())
//                .tradePlace(storeInfo.getAddress().get)
//                .build();

    }

//    public void getStoreList(String accessToken) {
//
//    }

    //상점 호출(info)

    //상점 목록 호출(infoList)

    //상점 추가

    //상점 수정

    //상점 삭제

}
