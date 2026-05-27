package com.msa.jewelry.local.transaction_history.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.factory.repository.FactoryRepository;
import com.msa.jewelry.local.store.repository.StoreRepository;
import com.msa.jewelry.local.transaction_history.dto.PurchaseDto;
import com.msa.jewelry.local.transaction_history.dto.TransactionDto;
import com.msa.jewelry.local.transaction_history.dto.TransactionPage;
import com.msa.jewelry.local.transaction_history.entity.TransactionHistory;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransactionHistoryService 단위 테스트")
class TransactionHistoryServiceTest {

    private static final Long   STORE_ID   = 100L;
    private static final Long   FACTORY_ID = 200L;
    private static final String STORE_NAME   = "강남금은방";
    private static final String FACTORY_NAME = "한국제조";

    @Mock StoreRepository storeRepository;
    @Mock FactoryRepository factoryRepository;
    @Mock TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    TransactionHistoryService service;

    // -----------------------------------------------------------------------
    // getCurrentBalance
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getCurrentBalance")
    class GetCurrentBalance {

        @Test
        @DisplayName("type=store — storeRepository 위임, factoryRepository 호출 없음")
        void store_분기() {
            TransactionDto dto = new TransactionDto("12.345", "1500000");
            given(storeRepository.findByStoreIdAndStoreName(STORE_ID, STORE_NAME)).willReturn(dto);

            TransactionDto result = service.getCurrentBalance("store", STORE_ID.toString(), STORE_NAME);

            assertThat(result).isEqualTo(dto);
            assertThat(result.currentGoldBalance()).isEqualTo("12.345");
            verify(storeRepository).findByStoreIdAndStoreName(STORE_ID, STORE_NAME);
            verifyNoInteractions(factoryRepository);
        }

        @Test
        @DisplayName("type=factory — factoryRepository 위임")
        void factory_분기() {
            TransactionDto dto = new TransactionDto("3.000", "200000");
            given(factoryRepository.findByFactoryIdAndFactoryName(FACTORY_ID, FACTORY_NAME)).willReturn(dto);

            TransactionDto result = service.getCurrentBalance("factory", FACTORY_ID.toString(), FACTORY_NAME);

            assertThat(result).isEqualTo(dto);
            verify(factoryRepository).findByFactoryIdAndFactoryName(FACTORY_ID, FACTORY_NAME);
            verifyNoInteractions(storeRepository);
        }

        @Test
        @DisplayName("type 이 store 가 아닌 임의 문자열(unknown) → factory 분기로 폴백 (else 처리)")
        void unknown_type_factory_폴백() {
            TransactionDto dto = new TransactionDto("0", "0");
            given(factoryRepository.findByFactoryIdAndFactoryName(eq(FACTORY_ID), any())).willReturn(dto);

            TransactionDto result = service.getCurrentBalance("anything", FACTORY_ID.toString(), FACTORY_NAME);

            assertThat(result).isEqualTo(dto);
            verify(factoryRepository).findByFactoryIdAndFactoryName(FACTORY_ID, FACTORY_NAME);
        }

        @Test
        @DisplayName("ID 가 숫자로 파싱 불가 → NumberFormatException")
        void 잘못된_ID() {
            assertThatThrownBy(() -> service.getCurrentBalance("store", "not-a-number", STORE_NAME))
                    .isInstanceOf(NumberFormatException.class);

            verifyNoInteractions(storeRepository, factoryRepository);
        }

        @Test
        @DisplayName("결과가 null 이어도 그대로 반환 (불변 조회)")
        void null_허용() {
            given(storeRepository.findByStoreIdAndStoreName(STORE_ID, STORE_NAME)).willReturn(null);

            assertThat(service.getCurrentBalance("store", STORE_ID.toString(), STORE_NAME)).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // findAccountPurchase
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findAccountPurchase")
    class FindAccountPurchase {

        @Test
        @DisplayName("정상 — 레포에 위임")
        void 정상() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistory(
                    "2026-05-01", "2026-05-31", "store", "강남금은방", pageable))
                    .willReturn(empty);

            CustomPage<TransactionPage> result = service.findAccountPurchase(
                    "2026-05-01", "2026-05-31", "store", "강남금은방", pageable);

            assertThat(result.getContent()).isEmpty();
            verify(transactionHistoryRepository).findTransactionHistory(
                    "2026-05-01", "2026-05-31", "store", "강남금은방", pageable);
        }

        @Test
        @DisplayName("빈 검색 조건 — 레포가 알아서 처리")
        void 빈_조건() {
            Pageable pageable = PageRequest.of(0, 10);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistory(
                    any(), any(), any(), any(), eq(pageable))).willReturn(empty);

            CustomPage<TransactionPage> result = service.findAccountPurchase(
                    "", "", "", "", pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("페이징 경계 — 큰 페이지 번호 그대로 전달")
        void 큰_페이지() {
            Pageable pageable = PageRequest.of(999, 5);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistory(any(), any(), any(), any(), eq(pageable)))
                    .willReturn(empty);

            service.findAccountPurchase("2026-05-01", "2026-05-31", "store", "x", pageable);

            verify(transactionHistoryRepository).findTransactionHistory(
                    "2026-05-01", "2026-05-31", "store", "x", pageable);
        }
    }

    // -----------------------------------------------------------------------
    // findFactoryPurchase
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findFactoryPurchase")
    class FindFactoryPurchase {

        @Test
        @DisplayName("정상 — factory 전용 레포 메서드 위임")
        void 정상() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistoryFactory(
                    "2026-05-01", "2026-05-31", "factory", "한국제조", pageable))
                    .willReturn(empty);

            CustomPage<TransactionPage> result = service.findFactoryPurchase(
                    "2026-05-01", "2026-05-31", "factory", "한국제조", pageable);

            assertThat(result.getContent()).isEmpty();
            verify(transactionHistoryRepository).findTransactionHistoryFactory(
                    "2026-05-01", "2026-05-31", "factory", "한국제조", pageable);
        }

        @Test
        @DisplayName("findAccountPurchase 와 별개의 메서드 — 위임 분리 확인")
        void 메서드_분리() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistoryFactory(any(), any(), any(), any(), any()))
                    .willReturn(empty);

            service.findFactoryPurchase("2026-05-01", "2026-05-31", "factory", "한국제조", pageable);

            verify(transactionHistoryRepository).findTransactionHistoryFactory(any(), any(), any(), any(), any());
            verify(transactionHistoryRepository, never()).findTransactionHistory(any(), any(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // savePurchase
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("savePurchase")
    class SavePurchase {

        @Test
        @DisplayName("정상 — 빌더 매핑 후 save, transactionDate 있으면 updateTransactionDate 호출")
        void 정상() throws Exception {
            PurchaseDto dto = purchaseDto("SALE", new BigDecimal("3.333"), 500_000L,
                    "445823472384938240", LocalDateTime.of(2026, 5, 16, 14, 30));

            service.savePurchase(dto);

            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());

            TransactionHistory saved = captor.getValue();
            assertThat(saved.getTransactionType()).isEqualTo(SaleStatus.SALE);
            assertThat(saved.getGoldAmount()).isEqualByComparingTo("3.333");
            assertThat(saved.getMoneyAmount()).isEqualTo(500_000L);
            assertThat(saved.getAccountSaleCode()).isEqualTo(445_823_472_384_938_240L);
            assertThat(saved.getTransactionDate()).isEqualTo(LocalDateTime.of(2026, 5, 16, 14, 30));
        }

        @Test
        @DisplayName("transactionDate=null — updateTransactionDate 호출 안 됨 (이력 그대로 저장)")
        void 날짜_null() {
            PurchaseDto dto = purchaseDto("PAYMENT", new BigDecimal("0"), 1_000L,
                    "100", null);

            service.savePurchase(dto);

            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());

            // 생성 직후 호출이므로 onCreate() 가 안 불려서 transactionDate 가 null 인 상태가 정상
            assertThat(captor.getValue().getTransactionDate()).isNull();
            assertThat(captor.getValue().getTransactionType()).isEqualTo(SaleStatus.PAYMENT);
        }

        @Test
        @DisplayName("잘못된 transactionType 문자열 → IllegalArgumentException(enum)")
        void 잘못된_type() {
            PurchaseDto dto = purchaseDto("NOT_A_TYPE", BigDecimal.ZERO, 0L, "1", null);

            assertThatThrownBy(() -> service.savePurchase(dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(transactionHistoryRepository);
        }

        @Test
        @DisplayName("saleCode 가 숫자가 아닌 경우 → NumberFormatException")
        void 잘못된_saleCode() {
            PurchaseDto dto = purchaseDto("SALE", BigDecimal.ZERO, 0L, "abc", null);

            assertThatThrownBy(() -> service.savePurchase(dto))
                    .isInstanceOf(NumberFormatException.class);

            verifyNoInteractions(transactionHistoryRepository);
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static PurchaseDto purchaseDto(String type, BigDecimal gold, Long money,
                                           String saleCode, LocalDateTime txDate) {
        PurchaseDto dto = PurchaseDto.builder()
                .transactionType(type)
                .goldAmount(gold)
                .moneyAmount(money)
                .saleCode(saleCode)
                .accountId("10")
                .transactionNote("단위테스트")
                .build();

        if (txDate != null) {
            // reflection 으로 transactionDate 세팅 (DTO setter 없음, builder 에도 없음)
            setField(dto, "transactionDate", txDate);
        }
        return dto;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
