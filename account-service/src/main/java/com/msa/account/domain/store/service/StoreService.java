package com.msa.account.domain.store.service;

import com.msa.account.domain.store.dto.StoreDto;
import com.msa.account.domain.store.entity.Store;
import com.msa.account.domain.store.repository.AdditionalOptionRepository;
import com.msa.account.domain.store.repository.StoreRepository;
import com.msa.account.global.domain.dto.util.AuthorityUserRoleUtil;
import com.msa.account.global.domain.entity.*;
import com.msa.account.global.domain.repository.AddressRepository;
import com.msa.account.global.domain.repository.CommonOptionRepository;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.ExceptionMessage;
import com.msa.account.global.exception.NotAuthorityException;
import com.msa.account.global.exception.NotFoundException;
import com.msacommon.global.util.CustomPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import static com.msa.account.global.exception.ExceptionMessage.*;

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
    public CustomPage<StoreDto.StoreResponse> getStoreList(Pageable pageable) {
        return storeRepository.findAllStore(pageable);
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
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));

        if (authorityUserRoleUtil.storeVerification(token, store)) {
            StoreDto.StoreInfo storeInfo = updateInfo.getStoreInfo();

            //상점 이름 검증
            if (store.isNameChanged(storeInfo.getStoreName())) {
                if (storeRepository.existsByStoreName(storeInfo.getStoreName())) {
                    throw new NotFoundException(ALREADY_EXIST_STORE);
                }
            }

            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getGoldHarryId()))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            store.updateStoreInfo(storeInfo);
            store.updateAddressInfo(updateInfo.getAddressInfo());
            store.updateCommonOption(updateInfo.getCommonOptionInfo(), goldHarry);
            store.updateAdditionalOption(updateInfo.getAdditionalOptionInfo());
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    //상점 삭제
    public void deleteStore(String token, String storeId) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));

        if (authorityUserRoleUtil.storeVerification(token, store)) {
            storeRepository.delete(store);
            return;
        }
        throw new NotAuthorityException(NO_ROLE);
    }

}
