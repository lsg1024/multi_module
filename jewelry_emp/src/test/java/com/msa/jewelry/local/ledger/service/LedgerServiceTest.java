package com.msa.jewelry.local.ledger.service;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.ledger.dto.LedgerDto;
import com.msa.jewelry.local.ledger.entity.AssetType;
import com.msa.jewelry.local.ledger.entity.Ledger;
import com.msa.jewelry.local.ledger.entity.TransactionType;
import com.msa.jewelry.local.ledger.repository.LedgerRepository;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LedgerService 단위 테스트")
class LedgerServiceTest {

    private static final Long      LEDGER_ID = 1_001L;
    private static final String    USER_ID   = "tester";
    private static final LocalDate START     = LocalDate.of(2026, 5, 1);
    private static final LocalDate END       = LocalDate.of(2026, 5, 31);

    @Mock LedgerRepository ledgerRepository;

    @InjectMocks
    LedgerService ledgerService;

    // -----------------------------------------------------------------------
    // createLedger
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createLedger")
    class CreateLedger {

        @Test
        @DisplayName("정상 — 엔티티 빌더로 생성 후 save")
        void 정상() {
            LedgerDto.CreateRequest req = mockCreateRequest(
                    AssetType.GOLD, TransactionType.INCOME,
                    new BigDecimal("12.345"), 1_500_000L, "금 입고");

            ledgerService.createLedger(req, USER_ID);

            ArgumentCaptor<Ledger> captor = ArgumentCaptor.forClass(Ledger.class);
            verify(ledgerRepository).save(captor.capture());

            Ledger saved = captor.getValue();
            assertThat(saved.getAssetType()).isEqualTo(AssetType.GOLD);
            assertThat(saved.getTransactionType()).isEqualTo(TransactionType.INCOME);
            assertThat(saved.getGoldAmount()).isEqualByComparingTo("12.345");
            assertThat(saved.getMoneyAmount()).isEqualTo(1_500_000L);
            assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
            assertThat(saved.getDescription()).isEqualTo("금 입고");
        }

        @Test
        @DisplayName("EXPENSE 거래 — 음수가 아닌 양수로 저장 (잔액 계산은 쿼리에서 부호 반전)")
        void 출금_정상() {
            LedgerDto.CreateRequest req = mockCreateRequest(
                    AssetType.MONEY, TransactionType.EXPENSE,
                    null, 500_000L, "현금 출금");

            ledgerService.createLedger(req, USER_ID);

            ArgumentCaptor<Ledger> captor = ArgumentCaptor.forClass(Ledger.class);
            verify(ledgerRepository).save(captor.capture());
            assertThat(captor.getValue().getTransactionType()).isEqualTo(TransactionType.EXPENSE);
            assertThat(captor.getValue().getGoldAmount()).isNull();
        }

        @Test
        @DisplayName("save 실패 — DB 예외 전파")
        void 저장실패() {
            LedgerDto.CreateRequest req = mockCreateRequest(
                    AssetType.GOLD, TransactionType.INCOME, BigDecimal.ONE, 0L, "test");
            willThrow(new RuntimeException("DB down"))
                    .given(ledgerRepository).save(any(Ledger.class));

            assertThatThrownBy(() -> ledgerService.createLedger(req, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB down");
        }
    }

    // -----------------------------------------------------------------------
    // updateLedger
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateLedger")
    class UpdateLedger {

        @Test
        @DisplayName("정상 — entity.update 호출 (save 없음, dirty checking)")
        void 정상() {
            Ledger ledger = mock(Ledger.class);
            given(ledgerRepository.findById(LEDGER_ID)).willReturn(Optional.of(ledger));

            LedgerDto.UpdateRequest req = mockUpdateRequest(
                    TransactionType.EXPENSE, new BigDecimal("5.000"), 1_000_000L, "수정됨");

            ledgerService.updateLedger(LEDGER_ID, req);

            verify(ledger).update(
                    any(LocalDate.class),
                    eq(TransactionType.EXPENSE),
                    eq(new BigDecimal("5.000")),
                    eq(1_000_000L),
                    eq("수정됨")
            );
            verify(ledgerRepository, never()).save(any());
        }

        @Test
        @DisplayName("미존재 → NotFoundException(ID 메시지 포함)")
        void 미존재() {
            given(ledgerRepository.findById(LEDGER_ID)).willReturn(Optional.empty());
            LedgerDto.UpdateRequest req = mock(LedgerDto.UpdateRequest.class);

            assertThatThrownBy(() -> ledgerService.updateLedger(LEDGER_ID, req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(String.valueOf(LEDGER_ID))
                    .hasMessageContaining("가계부");
        }

        @Test
        @DisplayName("동시성 — entity.update 도중 OptimisticLockingFailureException 전파")
        void 낙관락_충돌() {
            Ledger ledger = mock(Ledger.class);
            given(ledgerRepository.findById(LEDGER_ID)).willReturn(Optional.of(ledger));
            willThrow(new OptimisticLockingFailureException("conflict"))
                    .given(ledger).update(any(), any(), any(), any(), any());

            LedgerDto.UpdateRequest req = mockUpdateRequest(
                    TransactionType.INCOME, BigDecimal.ONE, 100L, "n");

            assertThatThrownBy(() -> ledgerService.updateLedger(LEDGER_ID, req))
                    .isInstanceOf(OptimisticLockingFailureException.class);
        }
    }

    // -----------------------------------------------------------------------
    // deleteLedger
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteLedger")
    class DeleteLedger {

        @Test
        @DisplayName("정상 — repository.delete(ledger) 호출")
        void 정상() {
            Ledger ledger = mock(Ledger.class);
            given(ledgerRepository.findById(LEDGER_ID)).willReturn(Optional.of(ledger));

            ledgerService.deleteLedger(LEDGER_ID);

            verify(ledgerRepository).delete(ledger);
        }

        @Test
        @DisplayName("미존재 → NotFoundException, delete 호출 안 함")
        void 미존재() {
            given(ledgerRepository.findById(LEDGER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ledgerService.deleteLedger(LEDGER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(String.valueOf(LEDGER_ID));

            verify(ledgerRepository, never()).delete(any());
        }
    }

    // -----------------------------------------------------------------------
    // getLedgerList
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getLedgerList")
    class GetLedgerList {

        @Test
        @DisplayName("assetType null → 일자만 필터하는 메서드 호출")
        void assetType_null() {
            Pageable pageable = PageRequest.of(0, 20);
            given(ledgerRepository.findByLedgerDateBetweenOrderByLedgerDateDesc(START, END, pageable))
                    .willReturn(emptyPage(pageable));

            CustomPage<LedgerDto.LedgerResponse> result =
                    ledgerService.getLedgerList(null, START, END, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(ledgerRepository).findByLedgerDateBetweenOrderByLedgerDateDesc(START, END, pageable);
            verify(ledgerRepository, never()).findByAssetTypeAndLedgerDateBetweenOrderByLedgerDateDesc(
                    any(), any(), any(), any());
        }

        @Test
        @DisplayName("assetType GOLD — 자산별 필터 호출")
        void assetType_GOLD() {
            Pageable pageable = PageRequest.of(0, 20);
            given(ledgerRepository.findByAssetTypeAndLedgerDateBetweenOrderByLedgerDateDesc(
                    AssetType.GOLD, START, END, pageable))
                    .willReturn(emptyPage(pageable));

            ledgerService.getLedgerList(AssetType.GOLD, START, END, pageable);

            verify(ledgerRepository).findByAssetTypeAndLedgerDateBetweenOrderByLedgerDateDesc(
                    AssetType.GOLD, START, END, pageable);
            verify(ledgerRepository, never()).findByLedgerDateBetweenOrderByLedgerDateDesc(any(), any(), any());
        }

        @Test
        @DisplayName("결과 1건 — LedgerResponse.from 변환")
        void 결과_변환() {
            Pageable pageable = PageRequest.of(0, 20);
            Ledger ledger = mockLedger();
            given(ledgerRepository.findByLedgerDateBetweenOrderByLedgerDateDesc(START, END, pageable))
                    .willReturn(new PageImpl<>(List.of(ledger), pageable, 1));

            CustomPage<LedgerDto.LedgerResponse> result =
                    ledgerService.getLedgerList(null, START, END, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getLedgerId()).isEqualTo(LEDGER_ID);
            assertThat(result.getContent().get(0).getAssetType()).isEqualTo(AssetType.GOLD);
        }

        @Test
        @DisplayName("페이징 경계 — 빈 결과여도 PageRequest 정보 보존")
        void 페이징_경계() {
            Pageable pageable = PageRequest.of(99, 5);
            given(ledgerRepository.findByLedgerDateBetweenOrderByLedgerDateDesc(START, END, pageable))
                    .willReturn(emptyPage(pageable));

            CustomPage<LedgerDto.LedgerResponse> result =
                    ledgerService.getLedgerList(null, START, END, pageable);

            assertThat(result.getNumber()).isEqualTo(99);
            assertThat(result.getSize()).isEqualTo(5);
        }
    }

    // -----------------------------------------------------------------------
    // getLedger
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getLedger")
    class GetLedger {

        @Test
        @DisplayName("정상 — LedgerResponse.from 변환")
        void 정상() {
            given(ledgerRepository.findById(LEDGER_ID)).willReturn(Optional.of(mockLedger()));

            LedgerDto.LedgerResponse resp = ledgerService.getLedger(LEDGER_ID);

            assertThat(resp.getLedgerId()).isEqualTo(LEDGER_ID);
            assertThat(resp.getAssetType()).isEqualTo(AssetType.GOLD);
            assertThat(resp.getGoldAmount()).isEqualByComparingTo("12.345");
        }

        @Test
        @DisplayName("미존재 → NotFoundException")
        void 미존재() {
            given(ledgerRepository.findById(LEDGER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ledgerService.getLedger(LEDGER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining(String.valueOf(LEDGER_ID));
        }
    }

    // -----------------------------------------------------------------------
    // getBalance
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("정상 — 금/현금 잔액 둘 다 반환")
        void 정상() {
            given(ledgerRepository.calculateGoldBalance()).willReturn(new BigDecimal("30.500"));
            given(ledgerRepository.calculateMoneyBalance()).willReturn(5_000_000L);

            LedgerDto.BalanceResponse resp = ledgerService.getBalance();

            assertThat(resp.getTotalGold()).isEqualByComparingTo("30.500");
            assertThat(resp.getTotalMoney()).isEqualTo(5_000_000L);
        }

        @Test
        @DisplayName("잔액 부족(음수) — 그대로 노출")
        void 음수_잔액() {
            given(ledgerRepository.calculateGoldBalance()).willReturn(new BigDecimal("-1.250"));
            given(ledgerRepository.calculateMoneyBalance()).willReturn(-100_000L);

            LedgerDto.BalanceResponse resp = ledgerService.getBalance();

            assertThat(resp.getTotalGold()).isEqualByComparingTo("-1.250");
            assertThat(resp.getTotalMoney()).isEqualTo(-100_000L);
        }

        @Test
        @DisplayName("저장 데이터 없을 때 0 으로 반환 (쿼리에서 COALESCE)")
        void 빈_장부() {
            given(ledgerRepository.calculateGoldBalance()).willReturn(BigDecimal.ZERO);
            given(ledgerRepository.calculateMoneyBalance()).willReturn(0L);

            LedgerDto.BalanceResponse resp = ledgerService.getBalance();

            assertThat(resp.getTotalGold()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(resp.getTotalMoney()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Page<Ledger> emptyPage(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private static Ledger mockLedger() {
        Ledger ledger = mock(Ledger.class);
        given(ledger.getLedgerId()).willReturn(LEDGER_ID);
        given(ledger.getLedgerDate()).willReturn(LocalDate.of(2026, 5, 16));
        given(ledger.getAssetType()).willReturn(AssetType.GOLD);
        given(ledger.getTransactionType()).willReturn(TransactionType.INCOME);
        given(ledger.getGoldAmount()).willReturn(new BigDecimal("12.345"));
        given(ledger.getMoneyAmount()).willReturn(0L);
        given(ledger.getDescription()).willReturn("금 입고");
        return ledger;
    }

    private static LedgerDto.CreateRequest mockCreateRequest(
            AssetType asset, TransactionType trans, BigDecimal gold, Long money, String desc) {
        LedgerDto.CreateRequest req = mock(LedgerDto.CreateRequest.class);
        given(req.getLedgerDate()).willReturn(LocalDate.of(2026, 5, 16));
        given(req.getAssetType()).willReturn(asset);
        given(req.getTransactionType()).willReturn(trans);
        given(req.getGoldAmount()).willReturn(gold);
        given(req.getMoneyAmount()).willReturn(money);
        given(req.getDescription()).willReturn(desc);
        return req;
    }

    private static LedgerDto.UpdateRequest mockUpdateRequest(
            TransactionType trans, BigDecimal gold, Long money, String desc) {
        LedgerDto.UpdateRequest req = mock(LedgerDto.UpdateRequest.class);
        given(req.getLedgerDate()).willReturn(LocalDate.of(2026, 5, 17));
        given(req.getTransactionType()).willReturn(trans);
        given(req.getGoldAmount()).willReturn(gold);
        given(req.getMoneyAmount()).willReturn(money);
        given(req.getDescription()).willReturn(desc);
        return req;
    }
}
