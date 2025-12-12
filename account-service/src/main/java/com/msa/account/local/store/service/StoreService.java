package com.msa.account.local.store.service;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.global.exception.ExceptionMessage;
import com.msa.account.global.exception.NotAuthorityException;
import com.msa.account.global.exception.NotFoundException;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.common.global.util.CustomPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public AccountDto.AccountSingleResponse getStoreInfo(String storeId) {
        return storeRepository.findByStoreId(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND_STORE));
    }
    //상점 목록 호출(infoList)
    @Transactional(readOnly = true)
    public CustomPage<StoreDto.StoreResponse> getStoreList(String name, Pageable pageable) {
        return storeRepository.findAllStore(name, pageable);
    }

    @Transactional(readOnly = true)
    public CustomPage<AccountDto.accountResponse> getStoreAttempt(String name, Pageable pageable) {
        return storeRepository.findAllStoreAndAttempt(name, pageable);
    }

    @Transactional(readOnly = true)
    public AccountDto.accountResponse getStoreAttemptDetail(String storeId) {
        return storeRepository.findByStoreIdAndAttempt(Long.valueOf(storeId));
    }

    //상점 추가
    public void createStore(StoreDto.StoreRequest storeInfo) {
        if (storeRepository.existsByStoreName(storeInfo.getAccountInfo().getAccountName())) {
            throw new NotFoundException(ALREADY_EXIST_STORE);
        }

        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(storeInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

        Store newstore = storeInfo.toEntity(goldHarry);

        storeRepository.save(newstore);
    }

    //상점 수정
    public void updateStore(String token, String storeId, AccountDto.AccountUpdate updateInfo) {
        Store store = storeRepository.findWithAllOptionsById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));

        if (authorityUserRoleUtil.verification(token)) {
            AccountDto.AccountInfo storeInfo = updateInfo.getAccountInfo();

            //상점 이름 검증
            if (store.isNameChanged(storeInfo.getAccountName())) {
                if (storeRepository.existsByStoreName(storeInfo.getAccountName())) {
                    throw new NotFoundException(ALREADY_EXIST_STORE);
                }
            }

            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getCommonOptionInfo().getGoldHarryId()))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            store.updateStoreInfo(updateInfo.getAccountInfo());
            store.updateCommonOption(updateInfo.getCommonOptionInfo(), goldHarry);

            if (updateInfo.getAdditionalOptionInfo() != null) {
                store.updateAdditionalOption(updateInfo.getAdditionalOptionInfo());
            }

            if (updateInfo.getAddressInfo() != null) {
                store.updateAddressInfo(updateInfo.getAddressInfo());
            }

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

        if (authorityUserRoleUtil.verification(token)) {
            storeRepository.delete(store);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public StoreDto.ApiStoreInfo getStoreInfo(Long id) {
        Store store = storeRepository.findByStoreInfo(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        return new StoreDto.ApiStoreInfo(store.getStoreId(), store.getStoreName(), store.getCommonOption().getOptionLevel().getLevel(), store.getCommonOption().getGoldHarryLoss());
    }

    public String getStoreGrade(String storeId) {
        OptionLevel grade = storeRepository.findByCommonOptionOptionLevel(Long.valueOf(storeId));
        return grade.getLevel();
    }

    public void updateStoreHarry(String accessToken, String storeId, String harryId) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));

        if (authorityUserRoleUtil.verification(accessToken)) {
            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(harryId))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            store.getCommonOption().updateGoldHarry(goldHarry);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public void updateStoreGrade(String accessToken, String storeId, String grade) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));

        if (authorityUserRoleUtil.verification(accessToken)) {
            store.getCommonOption().updateOptionLevel(grade);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public List<AccountExcelDto> getExcel(String accessToken) {
        if (authorityUserRoleUtil.verification(accessToken)) {
            return storeRepository.findAllStoreExcel();
        }
        throw new NotAuthorityException(NO_ROLE);
    }
}
