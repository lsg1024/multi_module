package com.msa.account.domain.factory.service;

import com.msa.account.domain.factory.dto.FactoryDto;
import com.msa.account.domain.factory.entity.Factory;
import com.msa.account.domain.factory.repository.FactoryRepository;
import com.msa.account.global.domain.dto.util.AuthorityUserRoleUtil;
import com.msa.account.global.domain.entity.Address;
import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.AddressRepository;
import com.msa.account.global.domain.repository.CommonOptionRepository;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotAuthorityException;
import com.msa.account.global.exception.NotFoundException;
import com.msacommon.global.util.CustomPage;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.msa.account.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class FactoryService {
    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final FactoryRepository factoryRepository;
    private final AddressRepository addressRepository;
    private final CommonOptionRepository commonOptionRepository;
    private final GoldHarryRepository goldHarryRepository;

    public FactoryService(AuthorityUserRoleUtil authorityUserRoleUtil, FactoryRepository factoryRepository, AddressRepository addressRepository, CommonOptionRepository commonOptionRepository, GoldHarryRepository goldHarryRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.factoryRepository = factoryRepository;
        this.addressRepository = addressRepository;
        this.commonOptionRepository = commonOptionRepository;
        this.goldHarryRepository = goldHarryRepository;
    }


    @Transactional(readOnly = true)
    public FactoryDto.FactorySingleResponse getFactoryInfo(String factoryId) {
        return factoryRepository.findByFactoryId(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
    }

    @Transactional(readOnly = true)
    public CustomPage<FactoryDto.FactoryResponse> getFactoryList(Pageable pageable) {
        return factoryRepository.findAllFactory(pageable);
    }

    public void addFactory(FactoryDto.FactoryRequest factoryInfo) throws NotFoundException {
        if (factoryRepository.existsByFactoryName(factoryInfo.getFactoryInfo().getFactoryName())) {
            throw new NotFoundException(ALREADY_EXIST_FACTORY);
        }

        //로스
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(factoryInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

        Factory factory = factoryInfo.toEntity(goldHarry);

        factoryRepository.save(factory);
    }

    public void updateFactory(String token, String factoryId, FactoryDto.FactoryUpdate updateInfo) {

        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        if (authorityUserRoleUtil.factoryVerification(token, factory)) {
            FactoryDto.FactoryInfo factoryInfo = updateInfo.getFactoryInfo();

            if (!factory.getFactoryName().equals(factoryInfo.getFactoryName()) &&
                    factoryRepository.existsByFactoryName(factoryInfo.getFactoryName())) {
                throw new NotFoundException(ALREADY_EXIST_FACTORY);
            }

            Address address = addressRepository.findById(Long.valueOf(updateInfo.getAddressId()))
                    .orElseThrow(() -> new NotFoundException(WRONG_ADDRESS));
            CommonOption commonOption = commonOptionRepository.findById(Long.valueOf(updateInfo.getCommonOptionId()))
                    .orElseThrow(() -> new NotFoundException(WRONG_COMMON_OPTION));
            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getGoldHarryId()))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            commonOption.update(updateInfo.getCommonOptionInfo());
            commonOption.setGoldHarry(goldHarry);

            factory.updateFactoryInfo(factoryInfo);
            factory.setAddress(address);
            factory.setCommonOption(commonOption);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public void deleteFactory(String token, String factoryId) {

        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        if (authorityUserRoleUtil.factoryVerification(token, factory)) {
            factoryRepository.delete(factory);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

}
