package com.msa.account.local.store.service;

import com.msa.account.global.domain.dto.util.AuthorityUserRoleUtil;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.ExceptionMessage;
import com.msa.account.global.exception.NotAuthorityException;
import com.msa.account.global.exception.NotFoundException;
import com.msa.account.local.store.dto.StoreDto;
import com.msa.account.local.store.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.common.global.util.CustomPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.msa.account.global.exception.ExceptionMessage.*;

@Slf4j
@Service
@Transactional
public class StoreService {

    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final StoreRepository storeRepository;
    private final GoldHarryRepository goldHarryRepository;

    public StoreService(AuthorityUserRoleUtil authorityUserRoleUtil, StoreRepository storeRepository, GoldHarryRepository goldHarryRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.storeRepository = storeRepository;
        this.goldHarryRepository = goldHarryRepository;
    }

    //상점 호출(info)
    @Transactional(readOnly = true)
    public StoreDto.StoreSingleResponse getStoreInfo(String storeId) {
        return storeRepository.findByStoreId(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND_STORE));
    }
    //상점 목록 호출(infoList)
    @Transactional(readOnly = true)
    public CustomPage<StoreDto.StoreResponse> getStoreList(String name, Pageable pageable) {
        return storeRepository.findAllStore(name, pageable);
    }

    //상점 추가
    public void createStore(StoreDto.StoreRequest storeInfo) {
        if (storeRepository.existsByStoreName(storeInfo.getStoreInfo().getStoreName())) {
            throw new NotFoundException(ALREADY_EXIST_STORE);
        }

        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(storeInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

        Store newstore = storeInfo.toEntity(goldHarry);

        storeRepository.save(newstore);
    }

    //상점 수정
    public void updateStore(String token, String storeId, StoreDto.StoreUpdate updateInfo) {
        Store store = storeRepository.findWithAllOptionsById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));

        if (authorityUserRoleUtil.storeVerification(token, store)) {
            StoreDto.StoreInfo storeInfo = updateInfo.getStoreInfo();

            //상점 이름 검증
            if (store.isNameChanged(storeInfo.getStoreName())) {
                if (storeRepository.existsByStoreName(storeInfo.getStoreName())) {
                    throw new NotFoundException(ALREADY_EXIST_STORE);
                }
            }

            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getCommonOptionInfo().getGoldHarryId()))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            store.updateStoreInfo(updateInfo.getStoreInfo());
            store.updateCommonOption(updateInfo.getCommonOptionInfo(), goldHarry);
            store.updateAdditionalOption(updateInfo.getAdditionalOptionInfo());
            store.updateAddressInfo(updateInfo.getAddressInfo());

            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    //상점 삭제
    public void deleteStore(String token, String storeId) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));

        if (store.isStoreDefault()) {
            throw new IllegalArgumentException("기본 값은 삭제가 불가능 합니다.");
        }

        if (authorityUserRoleUtil.storeVerification(token, store)) {
            storeRepository.delete(store);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public StoreDto.ApiStoreInfo getStoreInfo(Long id) {
        Store store = storeRepository.findByStoreInfo(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        return new StoreDto.ApiStoreInfo(store.getStoreId(), store.getStoreName(), store.getCommonOption().getOptionLevel().getLevel());
    }

    public String getStoreGrade(String storeId) {
        OptionLevel grade = storeRepository.findByCommonOptionOptionLevel(Long.valueOf(storeId));
        return grade.getLevel();
    }
}
