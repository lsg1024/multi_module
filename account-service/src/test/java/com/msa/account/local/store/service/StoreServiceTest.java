package com.msa.account.local.store.service;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.AdditionalOptionDto;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.account.local.transaction_history.repository.SaleLogRepository;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private GoldHarryRepository goldHarryRepository;

    @Mock
    private SaleLogRepository saleLogRepository;

    @Mock
    private AuthorityUserRoleUtil authorityUserRoleUtil;

    private StoreDto.StoreInfo originStoreInfo;
    private AddressDto.AddressInfo originAddressInfo;
    private CommonOptionDto.CommonOptionInfo originCommonOptionInfo;
    private AdditionalOptionDto.AdditionalOptionInfo originAdditionalOptionInfo;
    private GoldHarry mockGoldHarry;
    private Store testStore;
    private CommonOption testCommonOption;

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
                .grade("ONE")
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

        testCommonOption = CommonOption.builder()
                .optionLevel(OptionLevel.ONE)
                .goldHarry(mockGoldHarry)
                .goldHarryLoss("1.10")
                .build();

        testStore = Store.builder()
                .storeName("테스트매장")
                .storeOwnerName("홍길동")
                .storePhoneNumber("01012345678")
                .storeNote("테스트 메모")
                .commonOption(testCommonOption)
                .build();
    }

    @Nested
    @DisplayName("판매처 생성")
    class CreateStore {

        @Test
        @DisplayName("성공")
        void createStore_success() {
            // given
            StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
            setField(storeDto, "accountInfo", createAccountInfo());
            setField(storeDto, "addressInfo", originAddressInfo);
            setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
            setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

            given(storeRepository.existsByStoreName("testName")).willReturn(false);
            given(goldHarryRepository.findById(1L)).willReturn(Optional.of(mockGoldHarry));

            // when
            storeService.createStore(storeDto);

            // then
            verify(storeRepository).save(any(Store.class));
        }

        @Test
        @DisplayName("성공 - 저장된 판매처 정보 확인")
        void createStore_verifyData() {
            // given
            StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
            setField(storeDto, "accountInfo", createAccountInfo());
            setField(storeDto, "addressInfo", originAddressInfo);
            setField(storeDto, "commonOptionInfo", originCommonOptionInfo);
            setField(storeDto, "additionalOptionInfo", originAdditionalOptionInfo);

            ArgumentCaptor<Store> storeArgumentCaptor = ArgumentCaptor.forClass(Store.class);

            given(storeRepository.existsByStoreName("testName")).willReturn(false);
            given(goldHarryRepository.findById(1L)).willReturn(Optional.of(mockGoldHarry));

            // when
            storeService.createStore(storeDto);

            // then
            verify(storeRepository).save(storeArgumentCaptor.capture());
            assertThat(storeArgumentCaptor.getValue().getStoreName()).isEqualTo("testName");
        }

        @Test
        @DisplayName("실패 - 이미 존재하는 판매처명")
        void createStore_alreadyExists() {
            // given
            StoreDto.StoreRequest storeDto = new StoreDto.StoreRequest();
            AccountDto.AccountInfo accountInfo = new AccountDto.AccountInfo();
            setField(accountInfo, "accountName", "existTestName");
            setField(accountInfo, "accountOwnerName", "testOwner");
            setField(storeDto, "accountInfo", accountInfo);
            setField(storeDto, "commonOptionInfo", originCommonOptionInfo);

            given(storeRepository.existsByStoreName("existTestName")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> storeService.createStore(storeDto))
                    .isInstanceOf(NotFoundException.class);
        }

        private AccountDto.AccountInfo createAccountInfo() {
            AccountDto.AccountInfo accountInfo = new AccountDto.AccountInfo();
            setField(accountInfo, "accountName", "testName");
            setField(accountInfo, "accountOwnerName", "testOwner");
            setField(accountInfo, "accountPhoneNumber", "01012341234");
            setField(accountInfo, "accountContactNumber1", "01012341235");
            setField(accountInfo, "accountContactNumber2", "01012345678");
            setField(accountInfo, "accountFaxNumber", "010234556");
            setField(accountInfo, "accountNote", "testNote");
            return accountInfo;
        }
    }

    @Nested
    @DisplayName("판매처 단일 조회")
    class GetStoreInfo {

        @Test
        @DisplayName("성공 - String id")
        void getStoreInfo_withStringId_success() {
            // given
            String storeId = "1";
            AccountDto.AccountSingleResponse mockResponse = new AccountDto.AccountSingleResponse(
                    "1", "테스트매장", "홍길동", "01012345678", null, null, null, "테스트 메모",
                    null, null, null, null, "1", "WEIGHT", "ONE", "1", "1.10"
            );

            given(storeRepository.findByStoreId(1L)).willReturn(Optional.of(mockResponse));

            // when
            AccountDto.AccountSingleResponse result = storeService.getStoreInfo(storeId);

            // then
            assertThat(result.getAccountName()).isEqualTo("테스트매장");
            assertThat(result.getAccountOwnerName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 판매처")
        void getStoreInfo_notFound() {
            // given
            String storeId = "999";
            given(storeRepository.findByStoreId(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.getStoreInfo(storeId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("성공 - Long id (API용)")
        void getStoreInfo_withLongId_success() {
            // given
            Long storeId = 1L;
            given(storeRepository.findByStoreInfo(storeId)).willReturn(Optional.of(testStore));

            // when
            StoreDto.ApiStoreInfo result = storeService.getStoreInfo(storeId);

            // then
            assertThat(result.getStoreName()).isEqualTo("테스트매장");
            verify(storeRepository).findByStoreInfo(storeId);
        }
    }

    @Nested
    @DisplayName("판매처 등급 조회")
    class GetStoreGrade {

        @Test
        @DisplayName("성공")
        void getStoreGrade_success() {
            // given
            String storeId = "1";
            given(storeRepository.findByCommonOptionOptionLevel(1L)).willReturn(OptionLevel.ONE);

            // when
            String result = storeService.getStoreGrade(storeId);

            // then
            assertThat(result).isEqualTo(OptionLevel.ONE.getGrade());
        }
    }

    @Nested
    @DisplayName("판매처 삭제")
    class DeleteStore {

        @Test
        @DisplayName("성공")
        void deleteStore_success() {
            // given
            String accessToken = "test-token";
            String storeId = "1";

            given(storeRepository.findById(1L)).willReturn(Optional.of(testStore));
            given(authorityUserRoleUtil.verification(accessToken)).willReturn(true);

            // when
            storeService.deleteStore(accessToken, storeId);

            // then
            verify(storeRepository).delete(testStore);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 판매처")
        void deleteStore_notFound() {
            // given
            String accessToken = "test-token";
            String storeId = "999";

            given(storeRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.deleteStore(accessToken, storeId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("판매처 해리 업데이트")
    class UpdateStoreHarry {

        @Test
        @DisplayName("성공")
        void updateStoreHarry_success() {
            // given
            String accessToken = "test-token";
            String storeId = "1";
            String harryId = "2";

            GoldHarry newGoldHarry = GoldHarry.builder()
                    .goldHarryLoss(new BigDecimal("1.20"))
                    .build();

            given(storeRepository.findById(1L)).willReturn(Optional.of(testStore));
            given(authorityUserRoleUtil.verification(accessToken)).willReturn(true);
            given(goldHarryRepository.findById(2L)).willReturn(Optional.of(newGoldHarry));

            // when
            storeService.updateStoreHarry(accessToken, storeId, harryId);

            // then
            verify(goldHarryRepository).findById(2L);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 해리")
        void updateStoreHarry_harryNotFound() {
            // given
            String accessToken = "test-token";
            String storeId = "1";
            String harryId = "999";

            given(storeRepository.findById(1L)).willReturn(Optional.of(testStore));
            given(authorityUserRoleUtil.verification(accessToken)).willReturn(true);
            given(goldHarryRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.updateStoreHarry(accessToken, storeId, harryId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("판매처 등급 업데이트")
    class UpdateStoreGrade {

        @Test
        @DisplayName("성공")
        void updateStoreGrade_success() {
            // given
            String accessToken = "test-token";
            String storeId = "1";
            String grade = "TWO";

            given(storeRepository.findById(1L)).willReturn(Optional.of(testStore));
            given(authorityUserRoleUtil.verification(accessToken)).willReturn(true);

            // when
            storeService.updateStoreGrade(accessToken, storeId, grade);

            // then
            verify(storeRepository).findById(1L);
        }
    }
}
