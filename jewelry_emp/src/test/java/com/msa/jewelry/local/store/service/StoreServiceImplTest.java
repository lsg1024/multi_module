package com.msa.jewelry.local.store.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.global.excel.dto.ReceivableExcelDto;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.global.exception.NotAuthorityException;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.common_option.dto.AdditionalOptionDto;
import com.msa.jewelry.local.common_option.dto.CommonOptionDto;
import com.msa.jewelry.local.common_option.entity.CommonOption;
import com.msa.jewelry.local.common_option.entity.OptionLevel;
import com.msa.jewelry.local.common_option.entity.OptionTradeType;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.goldharry.repository.GoldHarryRepository;
import com.msa.jewelry.local.store.dto.StoreDto;
import com.msa.jewelry.local.store.dto.StorePhoneView;
import com.msa.jewelry.local.store.dto.StoreReceivableLogView;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.entity.AdditionalOption;
import com.msa.jewelry.local.store.entity.Store;
import com.msa.jewelry.local.store.repository.StoreRepository;
import com.msa.jewelry.local.transaction_history.entity.SaleLog;
import com.msa.jewelry.local.transaction_history.entity.TransactionHistory;
import com.msa.jewelry.local.transaction_history.repository.SaleLogRepository;
import com.msa.jewelry.local.transaction_history.repository.TransactionHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StoreServiceImpl 단위 테스트")
class StoreServiceImplTest {

    private static final String TOKEN      = "Bearer test-token";
    private static final String EVENT_ID   = "evt-store-unit-001";
    private static final Long   STORE_ID   = 100L;
    private static final String STORE_NAME = "강남금은방";
    private static final Long   SALE_CODE  = 5550L;
    private static final Long   HARRY_ID   = 1L;

    @Mock AuthorityUserRoleUtil authorityUserRoleUtil;
    @Mock StoreRepository storeRepository;
    @Mock SaleLogRepository saleLogRepository;
    @Mock GoldHarryRepository goldHarryRepository;
    @Mock TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    StoreServiceImpl storeService;

    // -----------------------------------------------------------------------
    // getStoreRecentActivity
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreRecentActivity")
    class GetStoreRecentActivity {

        @Test
        @DisplayName("정상 — 트랜잭션 + 결제 집계 합본 응답 반환")
        void 정상_조회() {
            given(transactionHistoryRepository.findRecentSalesByStore(STORE_ID, 20))
                    .willReturn(Collections.emptyList());
            AccountDto.PaymentSummary summary = mock(AccountDto.PaymentSummary.class);
            given(transactionHistoryRepository.findPaymentSummaryByStore(STORE_ID))
                    .willReturn(summary);

            AccountDto.RecentActivityResponse resp = storeService.getStoreRecentActivity(STORE_ID, 20);

            assertThat(resp).isNotNull();
            assertThat(resp.getRecentTransactions()).isEmpty();
            assertThat(resp.getPaymentSummary()).isSameAs(summary);
            verify(transactionHistoryRepository).findRecentSalesByStore(STORE_ID, 20);
            verify(transactionHistoryRepository).findPaymentSummaryByStore(STORE_ID);
        }

        @Test
        @DisplayName("limit=0 도 그대로 전달 — 빈 결과 보장")
        void limit_0_전달() {
            given(transactionHistoryRepository.findRecentSalesByStore(STORE_ID, 0))
                    .willReturn(Collections.emptyList());
            given(transactionHistoryRepository.findPaymentSummaryByStore(STORE_ID))
                    .willReturn(null);

            AccountDto.RecentActivityResponse resp = storeService.getStoreRecentActivity(STORE_ID, 0);

            assertThat(resp.getRecentTransactions()).isEmpty();
            verify(transactionHistoryRepository).findRecentSalesByStore(STORE_ID, 0);
        }
    }

    // -----------------------------------------------------------------------
    // getStoreInfo(String)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreInfo(String)")
    class GetStoreInfoString {

        @Test
        @DisplayName("정상 — repository 결과 그대로 반환")
        void 정상() {
            AccountDto.AccountSingleResponse single = mock(AccountDto.AccountSingleResponse.class);
            given(storeRepository.findByStoreId(STORE_ID)).willReturn(Optional.of(single));

            AccountDto.AccountSingleResponse result = storeService.getStoreInfo(STORE_ID.toString());

            assertThat(result).isSameAs(single);
        }

        @Test
        @DisplayName("없으면 NotFoundException — NOT_FOUND_STORE")
        void 없음() {
            given(storeRepository.findByStoreId(STORE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.getStoreInfo(STORE_ID.toString()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_STORE);
        }

        @Test
        @DisplayName("storeId 가 숫자가 아니면 NumberFormatException 위로 전파")
        void 잘못된_id_포맷() {
            assertThatThrownBy(() -> storeService.getStoreInfo("NaN"))
                    .isInstanceOf(NumberFormatException.class);
            verifyNoInteractions(storeRepository);
        }
    }

    // -----------------------------------------------------------------------
    // getStoreList (페이징)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreList")
    class GetStoreList {

        @Test
        @DisplayName("정상 — repository 위임")
        void 위임_검증() {
            Pageable pageable = PageRequest.of(0, 20);
            @SuppressWarnings("unchecked")
            CustomPage<StoreDto.StoreResponse> page = mock(CustomPage.class);
            given(storeRepository.findAllStore("강남", "name", "createDate", "desc", pageable)).willReturn(page);

            CustomPage<StoreDto.StoreResponse> result =
                    storeService.getStoreList("강남", "name", "createDate", "desc", pageable);

            assertThat(result).isSameAs(page);
            verify(storeRepository).findAllStore("강남", "name", "createDate", "desc", pageable);
        }

        @Test
        @DisplayName("name=null 도 그대로 위임 (전체 조회)")
        void name_null() {
            Pageable pageable = PageRequest.of(0, 10);
            @SuppressWarnings("unchecked")
            CustomPage<StoreDto.StoreResponse> page = mock(CustomPage.class);
            given(page.getContent()).willReturn(Collections.emptyList());
            given(storeRepository.findAllStore(null, null, null, null, pageable)).willReturn(page);

            CustomPage<StoreDto.StoreResponse> result =
                    storeService.getStoreList(null, null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getStoreReceivable
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreReceivable")
    class GetStoreReceivable {

        @Test
        @DisplayName("빈 결과 — repository 호출 후 빈 페이지 반환")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            @SuppressWarnings("unchecked")
            CustomPage<AccountDto.AccountResponse> empty = mock(CustomPage.class);
            given(empty.getContent()).willReturn(Collections.emptyList());
            given(storeRepository.findAllStoreAndReceivable(null, null, null, pageable)).willReturn(empty);

            CustomPage<AccountDto.AccountResponse> result =
                    storeService.getStoreReceivable(null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(storeRepository).findAllStoreAndReceivable(null, null, null, pageable);
        }

        @Test
        @DisplayName("페이징 경계 — 큰 페이지 번호도 그대로 위임")
        void 페이징_경계() {
            Pageable pageable = PageRequest.of(999, 50);
            @SuppressWarnings("unchecked")
            CustomPage<AccountDto.AccountResponse> page = mock(CustomPage.class);
            given(storeRepository.findAllStoreAndReceivable("강남", "balance", "desc", pageable))
                    .willReturn(page);

            assertThat(storeService.getStoreReceivable("강남", "balance", "desc", pageable)).isSameAs(page);
        }
    }

    // -----------------------------------------------------------------------
    // getStoreReceivableDetail
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreReceivableDetail")
    class GetStoreReceivableDetail {

        @Test
        @DisplayName("정상 — repository 위임")
        void 정상() {
            AccountDto.AccountResponse resp = mock(AccountDto.AccountResponse.class);
            given(storeRepository.findByStoreIdAndReceivable(STORE_ID)).willReturn(resp);

            assertThat(storeService.getStoreReceivableDetail(STORE_ID.toString())).isSameAs(resp);
        }

        @Test
        @DisplayName("null 반환도 그대로 — 가드 없음 (있는 그대로 검증)")
        void null_반환() {
            given(storeRepository.findByStoreIdAndReceivable(STORE_ID)).willReturn(null);

            assertThat(storeService.getStoreReceivableDetail(STORE_ID.toString())).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // getStoreReceivableLogDetail
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreReceivableLogDetail")
    class GetStoreReceivableLogDetail {

        @Test
        @DisplayName("정상 — 시작/종료 로그 합쳐서 balance update 호출")
        void 정상() {
            AccountDto.AccountSaleLogResponse logResp = mock(AccountDto.AccountSaleLogResponse.class);
            given(storeRepository.findByStoreIdAndReceivableByLog(STORE_ID)).willReturn(logResp);

            SaleLog first = mock(SaleLog.class);
            given(first.getPreviousGoldBalance()).willReturn(new BigDecimal("1.000"));
            given(first.getPreviousMoneyBalance()).willReturn(100L);
            SaleLog last = mock(SaleLog.class);
            given(last.getAfterGoldBalance()).willReturn(new BigDecimal("3.500"));
            given(last.getAfterMoneyBalance()).willReturn(500L);

            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateAsc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.of(first));
            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateDesc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.of(last));

            AccountDto.AccountSaleLogResponse result =
                    storeService.getStoreReceivableLogDetail(STORE_ID.toString(), SALE_CODE.toString());

            assertThat(result).isSameAs(logResp);
            verify(logResp).updateBalance(
                    new BigDecimal("1.000"), 100L,
                    new BigDecimal("3.500"), 500L);
        }

        @Test
        @DisplayName("시작 로그 없음 → IllegalArgumentException(NOT_FOUND + Start Log)")
        void 시작로그_없음() {
            AccountDto.AccountSaleLogResponse logResp = mock(AccountDto.AccountSaleLogResponse.class);
            given(storeRepository.findByStoreIdAndReceivableByLog(STORE_ID)).willReturn(logResp);
            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateAsc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.getStoreReceivableLogDetail(STORE_ID.toString(), SALE_CODE.toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ExceptionMessage.NOT_FOUND)
                    .hasMessageContaining("Start Log");
        }

        @Test
        @DisplayName("끝 로그 없음 → IllegalArgumentException")
        void 끝로그_없음() {
            AccountDto.AccountSaleLogResponse logResp = mock(AccountDto.AccountSaleLogResponse.class);
            given(storeRepository.findByStoreIdAndReceivableByLog(STORE_ID)).willReturn(logResp);

            SaleLog first = mock(SaleLog.class);
            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateAsc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.of(first));
            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateDesc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.getStoreReceivableLogDetail(STORE_ID.toString(), SALE_CODE.toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("storeId/saleCode 포맷 오류 → NumberFormatException")
        void 잘못된_포맷() {
            assertThatThrownBy(() -> storeService.getStoreReceivableLogDetail("ABC", SALE_CODE.toString()))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    // -----------------------------------------------------------------------
    // createStore
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createStore")
    class CreateStore {

        @Test
        @DisplayName("정상 — save 호출")
        void 정상() {
            StoreDto.StoreRequest req = mock(StoreDto.StoreRequest.class);
            AccountDto.AccountInfo accountInfo = mock(AccountDto.AccountInfo.class);
            given(accountInfo.getAccountName()).willReturn(STORE_NAME);
            given(req.getAccountInfo()).willReturn(accountInfo);

            CommonOptionDto.CommonOptionInfo commonInfo = mock(CommonOptionDto.CommonOptionInfo.class);
            given(commonInfo.getGoldHarryId()).willReturn(HARRY_ID.toString());
            given(req.getCommonOptionInfo()).willReturn(commonInfo);

            given(storeRepository.existsByStoreName(STORE_NAME)).willReturn(false);

            GoldHarry harry = mock(GoldHarry.class);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));

            Store newStore = Store.builder().storeName(STORE_NAME).build();
            given(req.toEntity(harry)).willReturn(newStore);

            storeService.createStore(req);

            verify(storeRepository).save(newStore);
        }

        @Test
        @DisplayName("이미 존재하는 이름 → NotFoundException(ALREADY_EXIST_STORE)")
        void 중복_이름() {
            StoreDto.StoreRequest req = mock(StoreDto.StoreRequest.class);
            AccountDto.AccountInfo accountInfo = mock(AccountDto.AccountInfo.class);
            given(accountInfo.getAccountName()).willReturn(STORE_NAME);
            given(req.getAccountInfo()).willReturn(accountInfo);
            given(storeRepository.existsByStoreName(STORE_NAME)).willReturn(true);

            assertThatThrownBy(() -> storeService.createStore(req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.ALREADY_EXIST_STORE);

            verify(storeRepository, never()).save(any());
        }

        @Test
        @DisplayName("GoldHarry 미존재 → NotFoundException(WRONG_HARRY)")
        void 해리_없음() {
            StoreDto.StoreRequest req = mock(StoreDto.StoreRequest.class);
            AccountDto.AccountInfo accountInfo = mock(AccountDto.AccountInfo.class);
            given(accountInfo.getAccountName()).willReturn(STORE_NAME);
            given(req.getAccountInfo()).willReturn(accountInfo);

            CommonOptionDto.CommonOptionInfo commonInfo = mock(CommonOptionDto.CommonOptionInfo.class);
            given(commonInfo.getGoldHarryId()).willReturn(HARRY_ID.toString());
            given(req.getCommonOptionInfo()).willReturn(commonInfo);

            given(storeRepository.existsByStoreName(STORE_NAME)).willReturn(false);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.createStore(req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.WRONG_HARRY);

            verify(storeRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // updateStore
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateStore")
    class UpdateStore {

        @Test
        @DisplayName("정상 — 이름 변경 없음 + 옵션/주소 모두 업데이트")
        void 정상_업데이트() {
            Store store = realStore(STORE_NAME);
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            AccountDto.AccountUpdate update = mock(AccountDto.AccountUpdate.class);
            AccountDto.AccountInfo info = mock(AccountDto.AccountInfo.class);
            given(info.getAccountName()).willReturn(STORE_NAME); // 같음 → isNameChanged=false
            given(update.getAccountInfo()).willReturn(info);

            CommonOptionDto.CommonOptionInfo common = mock(CommonOptionDto.CommonOptionInfo.class);
            given(common.getGoldHarryId()).willReturn(HARRY_ID.toString());
            given(common.getTradeType()).willReturn(OptionTradeType.WEIGHT.name());
            given(common.getGrade()).willReturn(OptionLevel.ONE.name());
            given(update.getCommonOptionInfo()).willReturn(common);

            AdditionalOptionDto.AdditionalOptionInfo additional =
                    mock(AdditionalOptionDto.AdditionalOptionInfo.class);
            given(additional.isAdditionalApplyPastSales()).willReturn(true);
            given(update.getAdditionalOptionInfo()).willReturn(additional);

            given(update.getAddressInfo()).willReturn(null);

            GoldHarry harry = mock(GoldHarry.class);
            given(harry.getGoldHarryLoss()).willReturn(new BigDecimal("1.05"));
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));

            storeService.updateStore(TOKEN, STORE_ID.toString(), update);

            // 이름이 같으므로 existsByStoreName 호출 안 됨
            verify(storeRepository, never()).existsByStoreName(any());
        }

        @Test
        @DisplayName("Store 미존재 → NotFoundException(NOT_FOUND_STORE)")
        void store_없음() {
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.empty());

            AccountDto.AccountUpdate update = mock(AccountDto.AccountUpdate.class);
            assertThatThrownBy(() -> storeService.updateStore(TOKEN, STORE_ID.toString(), update))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_STORE);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException(NO_ROLE)")
        void 권한_없음() {
            Store store = realStore(STORE_NAME);
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            AccountDto.AccountUpdate update = mock(AccountDto.AccountUpdate.class);
            assertThatThrownBy(() -> storeService.updateStore(TOKEN, STORE_ID.toString(), update))
                    .isInstanceOf(NotAuthorityException.class);

            verify(storeRepository, never()).existsByStoreName(any());
            verify(goldHarryRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("이름 변경 + 중복 → NotFoundException(ALREADY_EXIST_STORE)")
        void 이름변경_중복() {
            Store store = realStore("기존이름");
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            AccountDto.AccountUpdate update = mock(AccountDto.AccountUpdate.class);
            AccountDto.AccountInfo info = mock(AccountDto.AccountInfo.class);
            given(info.getAccountName()).willReturn("새이름");
            given(update.getAccountInfo()).willReturn(info);

            given(storeRepository.existsByStoreName("새이름")).willReturn(true);

            assertThatThrownBy(() -> storeService.updateStore(TOKEN, STORE_ID.toString(), update))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.ALREADY_EXIST_STORE);
        }

        @Test
        @DisplayName("GoldHarry 미존재 → NotFoundException(WRONG_HARRY)")
        void 해리_없음() {
            Store store = realStore(STORE_NAME);
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            AccountDto.AccountUpdate update = mock(AccountDto.AccountUpdate.class);
            AccountDto.AccountInfo info = mock(AccountDto.AccountInfo.class);
            given(info.getAccountName()).willReturn(STORE_NAME);
            given(update.getAccountInfo()).willReturn(info);

            CommonOptionDto.CommonOptionInfo common = mock(CommonOptionDto.CommonOptionInfo.class);
            given(common.getGoldHarryId()).willReturn(HARRY_ID.toString());
            given(update.getCommonOptionInfo()).willReturn(common);

            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.updateStore(TOKEN, STORE_ID.toString(), update))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.WRONG_HARRY);
        }
    }

    // -----------------------------------------------------------------------
    // deleteStore
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteStore")
    class DeleteStore {

        @Test
        @DisplayName("정상 — 기본 매장이 아니고 권한 있으면 삭제")
        void 정상() {
            Store store = realStore(STORE_NAME); // storeDefault = false (default)
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            storeService.deleteStore(TOKEN, STORE_ID.toString());

            verify(storeRepository).delete(store);
        }

        @Test
        @DisplayName("Store 미존재 → NotFoundException")
        void store_없음() {
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.deleteStore(TOKEN, STORE_ID.toString()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_STORE);
        }

        @Test
        @DisplayName("기본 매장이면 IllegalArgumentException — 삭제 불가")
        void 기본매장_삭제불가() {
            Store store = mock(Store.class);
            given(store.isStoreDefault()).willReturn(true);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));

            assertThatThrownBy(() -> storeService.deleteStore(TOKEN, STORE_ID.toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("기본 값은 삭제가 불가능");

            verify(storeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException")
        void 권한_없음() {
            Store store = mock(Store.class);
            given(store.isStoreDefault()).willReturn(false);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> storeService.deleteStore(TOKEN, STORE_ID.toString()))
                    .isInstanceOf(NotAuthorityException.class);

            verify(storeRepository, never()).delete(any());
        }
    }

    // -----------------------------------------------------------------------
    // getStoreInfo(Long) — ApiStoreInfo 반환
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreInfo(Long)")
    class GetStoreInfoLong {

        @Test
        @DisplayName("정상 — additionalOption 없음 → applyPastSales=false")
        void 정상_옵션없음() {
            Store store = realStoreWithCommonOption(STORE_NAME, null);
            given(storeRepository.findByStoreInfo(STORE_ID)).willReturn(Optional.of(store));

            StoreDto.ApiStoreInfo result = storeService.getStoreInfo(STORE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStoreId()).isEqualTo(store.getStoreId());
            assertThat(result.getStoreName()).isEqualTo(STORE_NAME);
            assertThat(result.isOptionApplyPastSales()).isFalse();
        }

        @Test
        @DisplayName("정상 — additionalOption 켜져있음 → applyPastSales=true")
        void 정상_옵션있음() {
            AdditionalOption additional = AdditionalOption.builder()
                    .optionApplyPastSales(true)
                    .build();
            Store store = realStoreWithCommonOption(STORE_NAME, additional);
            given(storeRepository.findByStoreInfo(STORE_ID)).willReturn(Optional.of(store));

            StoreDto.ApiStoreInfo result = storeService.getStoreInfo(STORE_ID);

            assertThat(result.isOptionApplyPastSales()).isTrue();
        }

        @Test
        @DisplayName("Store 미존재 → IllegalArgumentException(NOT_FOUND)")
        void 없음() {
            given(storeRepository.findByStoreInfo(STORE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.getStoreInfo(STORE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }
    }

    // -----------------------------------------------------------------------
    // getStoreGrade
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreGrade")
    class GetStoreGrade {

        @Test
        @DisplayName("정상 — OptionLevel.getGrade() 반환")
        void 정상() {
            given(storeRepository.findByCommonOptionOptionLevel(STORE_ID))
                    .willReturn(OptionLevel.ONE);

            assertThat(storeService.getStoreGrade(STORE_ID.toString())).isEqualTo("1");
        }

        @Test
        @DisplayName("null 반환 시 NullPointerException 위로 전파 — 가드 없음")
        void null_NPE() {
            given(storeRepository.findByCommonOptionOptionLevel(STORE_ID)).willReturn(null);

            assertThatThrownBy(() -> storeService.getStoreGrade(STORE_ID.toString()))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateStoreHarry
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateStoreHarry")
    class UpdateStoreHarry {

        @Test
        @DisplayName("정상 — commonOption.updateGoldHarry 호출")
        void 정상() {
            Store store = mock(Store.class);
            CommonOption co = mock(CommonOption.class);
            given(store.getCommonOption()).willReturn(co);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            GoldHarry harry = mock(GoldHarry.class);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));

            storeService.updateStoreHarry(TOKEN, STORE_ID.toString(), HARRY_ID.toString());

            verify(co).updateGoldHarry(harry);
        }

        @Test
        @DisplayName("Store 미존재 → NotFoundException")
        void store_없음() {
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.updateStoreHarry(TOKEN, STORE_ID.toString(), HARRY_ID.toString()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_STORE);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException, harry 조회 안 함")
        void 권한_없음() {
            Store store = mock(Store.class);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> storeService.updateStoreHarry(TOKEN, STORE_ID.toString(), HARRY_ID.toString()))
                    .isInstanceOf(NotAuthorityException.class);

            verify(goldHarryRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("GoldHarry 미존재 → NotFoundException(WRONG_HARRY)")
        void 해리_없음() {
            Store store = mock(Store.class);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.updateStoreHarry(TOKEN, STORE_ID.toString(), HARRY_ID.toString()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.WRONG_HARRY);
        }
    }

    // -----------------------------------------------------------------------
    // updateStoreGrade
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateStoreGrade")
    class UpdateStoreGrade {

        @Test
        @DisplayName("정상 — commonOption.updateOptionLevel 호출")
        void 정상() {
            Store store = mock(Store.class);
            CommonOption co = mock(CommonOption.class);
            given(store.getCommonOption()).willReturn(co);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            storeService.updateStoreGrade(TOKEN, STORE_ID.toString(), "ONE");

            verify(co).updateOptionLevel("ONE");
        }

        @Test
        @DisplayName("Store 미존재 → NotFoundException")
        void store_없음() {
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.updateStoreGrade(TOKEN, STORE_ID.toString(), "ONE"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_STORE);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException")
        void 권한_없음() {
            Store store = mock(Store.class);
            given(storeRepository.findById(STORE_ID)).willReturn(Optional.of(store));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> storeService.updateStoreGrade(TOKEN, STORE_ID.toString(), "ONE"))
                    .isInstanceOf(NotAuthorityException.class);

            verify(store, never()).getCommonOption();
        }
    }

    // -----------------------------------------------------------------------
    // getExcel
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExcel")
    class GetExcel {

        @Test
        @DisplayName("정상 — 권한 있으면 엑셀 DTO 리스트 반환")
        void 정상() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            List<AccountExcelDto> dtos = Collections.emptyList();
            given(storeRepository.findAllStoreExcel()).willReturn(dtos);

            assertThat(storeService.getExcel(TOKEN)).isSameAs(dtos);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException, 레포 호출 안 함")
        void 권한_없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> storeService.getExcel(TOKEN))
                    .isInstanceOf(NotAuthorityException.class);

            verify(storeRepository, never()).findAllStoreExcel();
        }
    }

    // -----------------------------------------------------------------------
    // getStorePhones
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStorePhones")
    class GetStorePhones {

        @Test
        @DisplayName("정상 — 일부는 존재, 일부는 미존재 → 존재하는 것만 필터링")
        void 부분존재() {
            Long id1 = 1L, id2 = 2L, id3 = 3L;
            Store s1 = realStorePhone(id1, "A상점", "010-1111-1111");
            Store s3 = realStorePhone(id3, "C상점", "010-3333-3333");
            given(storeRepository.findById(id1)).willReturn(Optional.of(s1));
            given(storeRepository.findById(id2)).willReturn(Optional.empty());
            given(storeRepository.findById(id3)).willReturn(Optional.of(s3));

            List<StoreDto.StorePhoneInfo> result = storeService.getStorePhones(List.of(id1, id2, id3));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(StoreDto.StorePhoneInfo::getStoreName).containsExactly("A상점", "C상점");
        }

        @Test
        @DisplayName("빈 ID 리스트 → 빈 결과")
        void 빈_리스트() {
            List<StoreDto.StorePhoneInfo> result = storeService.getStorePhones(Collections.emptyList());

            assertThat(result).isEmpty();
            verify(storeRepository, never()).findById(any());
        }

        @Test
        @DisplayName("모두 미존재 → 빈 결과")
        void 모두_없음() {
            given(storeRepository.findById(anyLong())).willReturn(Optional.empty());

            List<StoreDto.StorePhoneInfo> result = storeService.getStorePhones(List.of(1L, 2L));

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getStoreInfoByName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreInfoByName")
    class GetStoreInfoByName {

        @Test
        @DisplayName("정상 — 첫 번째 매장 정보 변환 반환")
        void 정상() {
            Store store = realStoreWithCommonOption(STORE_NAME, null);
            given(storeRepository.findByStoreNameIgnoreCase(STORE_NAME)).willReturn(List.of(store));

            StoreDto.ApiStoreInfo result = storeService.getStoreInfoByName(STORE_NAME);

            assertThat(result.getStoreName()).isEqualTo(STORE_NAME);
            assertThat(result.isOptionApplyPastSales()).isFalse();
        }

        @Test
        @DisplayName("빈 결과 → NotFoundException(NOT_FOUND_STORE)")
        void 없음() {
            given(storeRepository.findByStoreNameIgnoreCase(STORE_NAME)).willReturn(Collections.emptyList());

            assertThatThrownBy(() -> storeService.getStoreInfoByName(STORE_NAME))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_STORE);
        }

        @Test
        @DisplayName("동명이인 매장 여러 개 → 가장 앞 매장만 반환")
        void 다중_결과_첫번째() {
            Store store1 = realStoreWithCommonOption(STORE_NAME, null);
            Store store2 = realStoreWithCommonOption(STORE_NAME, null);
            given(storeRepository.findByStoreNameIgnoreCase(STORE_NAME)).willReturn(List.of(store1, store2));

            StoreDto.ApiStoreInfo result = storeService.getStoreInfoByName(STORE_NAME);

            assertThat(result).isNotNull();
            assertThat(result.getStoreName()).isEqualTo(STORE_NAME);
        }
    }

    // -----------------------------------------------------------------------
    // getReceivableExcel
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getReceivableExcel")
    class GetReceivableExcel {

        @Test
        @DisplayName("정상 — 권한 있으면 엑셀 byte[] 생성 (길이 > 0)")
        void 정상() throws IOException {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(storeRepository.findAllReceivableExcel("강남"))
                    .willReturn(Collections.<ReceivableExcelDto>emptyList());

            byte[] bytes = storeService.getReceivableExcel(TOKEN, "강남");

            assertThat(bytes).isNotEmpty();
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException, 레포 호출 안 함")
        void 권한_없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> storeService.getReceivableExcel(TOKEN, "강남"))
                    .isInstanceOf(NotAuthorityException.class);

            verify(storeRepository, never()).findAllReceivableExcel(any());
        }
    }

    // -----------------------------------------------------------------------
    // getStoreInfoView
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreInfoView")
    class GetStoreInfoView {

        @Test
        @DisplayName("정상 — StoreView 변환 (commonOption + additionalOption 모두 보유)")
        void 정상_옵션_모두() {
            CommonOption co = CommonOption.builder()
                    .optionTradeType(OptionTradeType.WEIGHT)
                    .optionLevel(OptionLevel.ONE)
                    .goldHarryLoss("1.05")
                    .build();
            AdditionalOption ao = AdditionalOption.builder()
                    .optionApplyPastSales(true)
                    .build();
            Store store = Store.builder()
                    .storeName(STORE_NAME)
                    .commonOption(co)
                    .additionalOption(ao)
                    .build();
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.of(store));

            StoreView view = storeService.getStoreInfoView(STORE_ID);

            assertThat(view).isNotNull();
            assertThat(view.storeName()).isEqualTo(STORE_NAME);
            assertThat(view.storeGrade()).isEqualTo("ONE");
            assertThat(view.storeHarry()).isEqualTo("1.05");
            assertThat(view.tradeType()).isEqualTo("WEIGHT");
            assertThat(view.applyPastSales()).isTrue();
        }

        @Test
        @DisplayName("commonOption 이 null 이어도 NPE 없이 변환")
        void commonOption_null() {
            Store store = Store.builder().storeName(STORE_NAME).build();
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.of(store));

            StoreView view = storeService.getStoreInfoView(STORE_ID);

            assertThat(view.storeHarry()).isNull();
            assertThat(view.storeGrade()).isNull();
            assertThat(view.tradeType()).isNull();
            assertThat(view.applyPastSales()).isFalse();
        }

        @Test
        @DisplayName("Store 미존재 → NotFoundException")
        void 없음() {
            given(storeRepository.findWithAllOptionsById(STORE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.getStoreInfoView(STORE_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("거래처 미존재");
        }
    }

    // -----------------------------------------------------------------------
    // findStoreByNameView
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findStoreByNameView")
    class FindStoreByNameView {

        @Test
        @DisplayName("정상 — 첫 번째 매장을 StoreView 로 변환")
        void 정상() {
            Store store = realStoreWithCommonOption(STORE_NAME, null);
            given(storeRepository.findByStoreNameIgnoreCase(STORE_NAME)).willReturn(List.of(store));

            StoreView view = storeService.findStoreByNameView(STORE_NAME);

            assertThat(view).isNotNull();
            assertThat(view.storeName()).isEqualTo(STORE_NAME);
        }

        @Test
        @DisplayName("결과 없음 → null 반환 (예외 안 던짐)")
        void 없음_null() {
            given(storeRepository.findByStoreNameIgnoreCase("없는이름")).willReturn(Collections.emptyList());

            assertThat(storeService.findStoreByNameView("없는이름")).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // getStorePhonesView
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStorePhonesView")
    class GetStorePhonesView {

        @Test
        @DisplayName("정상 — StorePhoneInfo → StorePhoneView 매핑")
        void 정상() {
            Store s = realStorePhone(1L, "A상점", "010-1111-2222");
            given(storeRepository.findById(1L)).willReturn(Optional.of(s));

            List<StorePhoneView> result = storeService.getStorePhonesView(List.of(1L));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).storeId()).isEqualTo(1L);
            assertThat(result.get(0).storeName()).isEqualTo("A상점");
            assertThat(result.get(0).storePhoneNumber()).isEqualTo("010-1111-2222");
        }

        @Test
        @DisplayName("빈 입력 → 빈 결과")
        void 빈_입력() {
            assertThat(storeService.getStorePhonesView(Collections.emptyList())).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getReceivableLog
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getReceivableLog")
    class GetReceivableLog {

        @Test
        @DisplayName("정상 — 위임 메서드 결과를 StoreReceivableLogView 로 매핑")
        void 정상() {
            AccountDto.AccountSaleLogResponse logResp = mock(AccountDto.AccountSaleLogResponse.class);
            given(logResp.getPreviousGoldBalance()).willReturn("1.000");
            given(logResp.getPreviousMoneyBalance()).willReturn("100");
            given(logResp.getAfterGoldBalance()).willReturn("3.500");
            given(logResp.getAfterMoneyBalance()).willReturn("500");
            given(logResp.getLastSaleDate()).willReturn("2026-05-16");

            given(storeRepository.findByStoreIdAndReceivableByLog(STORE_ID)).willReturn(logResp);

            SaleLog first = mock(SaleLog.class);
            given(first.getPreviousGoldBalance()).willReturn(new BigDecimal("1.000"));
            given(first.getPreviousMoneyBalance()).willReturn(100L);
            SaleLog last = mock(SaleLog.class);
            given(last.getAfterGoldBalance()).willReturn(new BigDecimal("3.500"));
            given(last.getAfterMoneyBalance()).willReturn(500L);

            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateAsc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.of(first));
            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateDesc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.of(last));

            StoreReceivableLogView view = storeService.getReceivableLog(STORE_ID, SALE_CODE.toString());

            assertThat(view).isNotNull();
            assertThat(view.previousGoldBalance()).isEqualTo("1.000");
            assertThat(view.afterMoneyBalance()).isEqualTo("500");
            assertThat(view.lastSaleDate()).isEqualTo("2026-05-16");
        }

        @Test
        @DisplayName("내부 호출에서 시작로그 없음 → IllegalArgumentException 전파")
        void 시작로그_없음_전파() {
            AccountDto.AccountSaleLogResponse logResp = mock(AccountDto.AccountSaleLogResponse.class);
            given(storeRepository.findByStoreIdAndReceivableByLog(STORE_ID)).willReturn(logResp);
            given(saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateAsc(SALE_CODE, STORE_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.getReceivableLog(STORE_ID, SALE_CODE.toString()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // applyDelta — 잔액 변동
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("applyDelta")
    class ApplyDelta {

        @Test
        @DisplayName("정상 — store.updateBalance + TransactionHistory.save 호출")
        void 정상() {
            Store store = mock(Store.class);
            given(storeRepository.findByIdWithLock(STORE_ID)).willReturn(Optional.of(store));

            storeService.applyDelta(STORE_ID, new BigDecimal("1.500"), 100_000L,
                    EVENT_ID, SaleStatus.SALE.name(), "18K", SALE_CODE, "정상거래");

            verify(store).updateBalance(new BigDecimal("1.500"), 100_000L);

            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());

            TransactionHistory saved = captor.getValue();
            assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
            assertThat(saved.getAccountSaleCode()).isEqualTo(SALE_CODE);
            assertThat(saved.getMaterial()).isEqualTo("18K");
            assertThat(saved.getGoldAmount()).isEqualByComparingTo("1.500");
            assertThat(saved.getMoneyAmount()).isEqualTo(100_000L);
            assertThat(saved.getTransactionType()).isEqualTo(SaleStatus.SALE);
            assertThat(saved.getStore()).isSameAs(store);
            assertThat(saved.getFactory()).isNull();
            assertThat(saved.getTransactionHistoryNote()).isEqualTo("정상거래");
        }

        @Test
        @DisplayName("Store 미존재 → NotFoundException, transaction 저장 안 함")
        void store_없음() {
            given(storeRepository.findByIdWithLock(STORE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> storeService.applyDelta(STORE_ID, BigDecimal.ONE, 100L,
                    EVENT_ID, "SALE", "18K", SALE_CODE, "note"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Store not found")
                    .hasMessageContaining(STORE_ID.toString());

            verify(transactionHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("goldDelta=null / moneyDelta=null → 각각 ZERO/0L 로 대체")
        void null_델타_기본값() {
            Store store = mock(Store.class);
            given(storeRepository.findByIdWithLock(STORE_ID)).willReturn(Optional.of(store));

            storeService.applyDelta(STORE_ID, null, null, EVENT_ID, "SALE", "18K", SALE_CODE, "null delta");

            verify(store).updateBalance(BigDecimal.ZERO, 0L);

            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getGoldAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(captor.getValue().getMoneyAmount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("음수 델타(반품) — 그대로 적용 (가드 없음)")
        void 음수_델타() {
            Store store = mock(Store.class);
            given(storeRepository.findByIdWithLock(STORE_ID)).willReturn(Optional.of(store));

            storeService.applyDelta(STORE_ID, new BigDecimal("-2.000"), -50_000L,
                    EVENT_ID, SaleStatus.RETURN.name(), "18K", SALE_CODE, "반품");

            verify(store).updateBalance(new BigDecimal("-2.000"), -50_000L);

            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getTransactionType()).isEqualTo(SaleStatus.RETURN);
        }

        @Test
        @DisplayName("transactionType 이 알 수 없는 값 → null 로 저장 (parseSaleStatus 폴백)")
        void 잘못된_타입() {
            Store store = mock(Store.class);
            given(storeRepository.findByIdWithLock(STORE_ID)).willReturn(Optional.of(store));

            storeService.applyDelta(STORE_ID, BigDecimal.ONE, 100L,
                    EVENT_ID, "NOT_A_REAL_STATUS", "18K", SALE_CODE, "note");

            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getTransactionType()).isNull();
        }

        @Test
        @DisplayName("transactionType 이 빈 문자열 → null 저장")
        void 빈_타입() {
            Store store = mock(Store.class);
            given(storeRepository.findByIdWithLock(STORE_ID)).willReturn(Optional.of(store));

            storeService.applyDelta(STORE_ID, BigDecimal.ONE, 100L,
                    EVENT_ID, "   ", "18K", SALE_CODE, "note");

            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getTransactionType()).isNull();
        }

        @Test
        @DisplayName("멱등성 — 동일 eventId 로 두 번 호출하면 두 번 모두 처리 (서비스는 가드 없음, DB 제약 위임)")
        void 멱등성_중복_호출() {
            // applyDelta 자체에는 eventId 중복 가드 없음 → save 가 2번 호출되며, 진짜 멱등성은 DB unique 가 보장.
            Store store = mock(Store.class);
            given(storeRepository.findByIdWithLock(STORE_ID)).willReturn(Optional.of(store));

            storeService.applyDelta(STORE_ID, BigDecimal.ONE, 100L,
                    EVENT_ID, "SALE", "18K", SALE_CODE, "1차");
            storeService.applyDelta(STORE_ID, BigDecimal.ONE, 100L,
                    EVENT_ID, "SALE", "18K", SALE_CODE, "2차");

            verify(storeRepository, times(2)).findByIdWithLock(STORE_ID);
            verify(transactionHistoryRepository, times(2)).save(any(TransactionHistory.class));
            verify(store, times(2)).updateBalance(BigDecimal.ONE, 100L);
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Store realStore(String name) {
        return Store.builder().storeName(name).build();
    }

    private static Store realStoreWithCommonOption(String name, AdditionalOption additional) {
        CommonOption co = CommonOption.builder()
                .optionTradeType(OptionTradeType.WEIGHT)
                .optionLevel(OptionLevel.ONE)
                .goldHarryLoss("1.05")
                .build();
        return Store.builder()
                .storeName(name)
                .commonOption(co)
                .additionalOption(additional)
                .build();
    }

    private static Store realStorePhone(Long storeId, String name, String phone) {
        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(storeId);
        given(store.getStoreName()).willReturn(name);
        given(store.getStorePhoneNumber()).willReturn(phone);
        return store;
    }
}
