package com.msa.account.domain.store.service;

import com.msa.account.domain.store.dto.StoreDto;
import com.msa.account.domain.store.entity.AdditionalOption;
import com.msa.account.domain.store.entity.Store;
import com.msa.account.domain.store.repository.AdditionalOptionRepository;
import com.msa.account.domain.store.repository.StoreRepository;
import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.*;
import com.msa.account.global.domain.repository.AddressRepository;
import com.msa.account.global.domain.repository.CommonOptionRepository;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import com.msacommon.global.jwt.JwtUtil;
import com.msacommon.global.util.CustomPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class StoreService {

    private final JwtUtil jwtUtil;
    private final StoreRepository storeRepository;
    private final AddressRepository addressRepository;
    private final GoldHarryRepository goldHarryRepository;
    private final CommonOptionRepository commonOptionRepository;
    private final AdditionalOptionRepository additionalOptionRepository;

    public StoreService(JwtUtil jwtUtil, StoreRepository storeRepository, AddressRepository addressRepository, GoldHarryRepository goldHarryRepository, CommonOptionRepository commonOptionRepository, AdditionalOptionRepository additionalOptionRepository) {
        this.jwtUtil = jwtUtil;
        this.storeRepository = storeRepository;
        this.addressRepository = addressRepository;
        this.goldHarryRepository = goldHarryRepository;
        this.commonOptionRepository = commonOptionRepository;
        this.additionalOptionRepository = additionalOptionRepository;
    }

    //상점 호출(info)
    public StoreDto.StoreSingleResponse getStoreInfo(String storeId) {
        StoreDto.StoreSingleResponse storeInfo = storeRepository.findByStoreId(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException("대상을 찾을 수 없습니다."));

        storeInfo.getTradeTypeTitle();
        storeInfo.getLevelTypeLevel();

        return storeInfo;
    }
    //상점 목록 호출(infoList)
    public CustomPage<StoreDto.StoreResponse> getStoreList(Pageable pageable) {
        return storeRepository.findAllStore(pageable);
    }

    //상점 추가
    @Transactional
    public void addStore(StoreDto.StoreRequest storeInfo) {
        
        //상점 이름 검증
        if (storeRepository.existsByStoreName(storeInfo.getStoreInfo().getStoreName())) {
            throw new NotFoundException("이미 존재하는 상점입니다.");
        }

        //로스
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(storeInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException("잘못된 해리값 입력입니다."));

        Store newstore = storeInfo.toEntity(goldHarry);

        storeRepository.save(newstore);
    }

    //상점 수정
    @Transactional
    public void updateStore(String storeId, StoreDto.StoreUpdate updateInfo) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException("존재하지 않은 상점 입니다."));

        StoreDto.StoreInfo storeInfo = updateInfo.getStoreInfo();

        //상점 이름 검증
        if (!store.getStoreName().equals(storeInfo.getStoreName()) &&
                storeRepository.existsByStoreName(storeInfo.getStoreName())) {
            throw new NotFoundException("이미 존재하는 상점입니다.");
        }

        Address address = addressRepository.findById(Long.valueOf(updateInfo.getAddressId()))
                .orElseThrow(() -> new NotFoundException("잘못된 주소 입력입니다."));
        CommonOption commonOption = commonOptionRepository.findById(Long.valueOf(updateInfo.getCommonOptionId()))
                .orElseThrow(() -> new NotFoundException("잘못된 기본 옵션 입력입니다."));
        AdditionalOption additionalOption = additionalOptionRepository.findById(Long.valueOf(updateInfo.getAdditionalOptionId()))
                .orElseThrow(() -> new NotFoundException("잘못된 추가 옵션 입력입니다."));
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException("잘못된 해리값 입력입니다."));

        commonOption.update(updateInfo.getCommonOptionInfo());
        commonOption.setGoldHarry(goldHarry);

        store.updateStoreInfo(storeInfo);
        store.setAddress(address);
        store.setCommonOption(commonOption);
        store.setAdditionalOption(additionalOption);
    }

    //상점 삭제
    @Transactional
    public void deleteStore(String storeId) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException("존재하지 않은 상점 입니다."));

        storeRepository.delete(store);
    }



}
