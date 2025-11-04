package com.msa.account.local.store.service;

import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.account.global.domain.dto.AdditionalOptionDto;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks
    StoreService storeService;

    @Mock
    StoreRepository storeRepository;
    @Mock
    GoldHarryRepository goldHarryRepository;

    private StoreDto.StoreInfo originStoreInfo;
    private AddressDto.AddressInfo originAddressInfo;
    private CommonOptionDto.CommonOptionInfo originCommonOptionInfo;
    private AdditionalOptionDto.AdditionalOptionInfo originAdditionalOptionInfo;
    private GoldHarry mockGoldHarry;

    @BeforeEach
    void setUp() {

        originStoreInfo = StoreDto.StoreInfo.builder()
                .storeName("testName")
                .storeOwnerName("testOwner")
                .storePhoneNumber("01012341234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        originAddressInfo = AddressDto.AddressInfo.builder()
                .addressZipCode("12345")
                .addressBasic("서울특별시")
                .addressAdd("강남구")
                .build();

        originCommonOptionInfo = CommonOptionDto.CommonOptionInfo.builder()
                .level("ONE")
                .tradeType("WEIGHT")
                .goldHarryId("1")
                .build();

        originAdditionalOptionInfo = AdditionalOptionDto.AdditionalOptionInfo.builder()
                .additionalMaterialId("1")
                .additionalMaterialName("gold")
                .additionalApplyPastSales(true)
                .build();

        mockGoldHarry = GoldHarry.builder()
                .goldHarryLoss(new BigDecimal("1.10"))
                .build();

        ReflectionTestUtils.setField(mockGoldHarry, "goldHarryId", 1L);
    }

    @Test
    void createStore() {
        //given
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        setField(storeDto, "storeInfo", originStoreInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        //when
        when(goldHarryRepository.findById(1L)).thenReturn(Optional.of(mockGoldHarry));
        storeService.createStore(storeDto);

        //then
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    void saveStore() {
        //given
        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        setField(storeDto, "storeInfo", originStoreInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        ArgumentCaptor<Store> storeArgumentCaptor = ArgumentCaptor.forClass(Store.class);

        //when
        when(goldHarryRepository.findById(1L)).thenReturn(Optional.of(mockGoldHarry));
        storeService.createStore(storeDto);

        //then
        verify(storeRepository).save(storeArgumentCaptor.capture());

        assertThat(storeArgumentCaptor.getValue().getStoreName()).isEqualTo("testName");
    }

    @Test
    void createFail_storeInfo_alreadyHave() {
        //given
        StoreDto.StoreInfo failStoreInfo = StoreDto.StoreInfo.builder()
                .storeName("existTestName")
                .storeOwnerName("testOwner")
                .storePhoneNumber("01012341234")
                .storeContactNumber1("01012341235")
                .storeContactNumber2("01012345678")
                .storeFaxNumber("010234556")
                .storeNote("testNote")
                .build();

        StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
        setField(storeDto, "storeInfo", failStoreInfo);
        setField(storeDto, "addressInfo", originAddressInfo);
        setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
        setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

        //when & then
        when(storeRepository.existsByStoreName("existTestName")).thenReturn(true);

        assertThrows(NotFoundException.class, () -> {
            storeService.createStore(storeDto);
        });
    }

    //수정

    //삭제

    //단일 조회

    //복수 조회
}