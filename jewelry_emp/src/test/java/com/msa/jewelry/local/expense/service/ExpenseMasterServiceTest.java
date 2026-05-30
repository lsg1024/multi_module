package com.msa.jewelry.local.expense.service;

import com.msa.jewelry.local.expense.dto.ExpenseBankTypeDto;
import com.msa.jewelry.local.expense.dto.ExpenseExpenseAccountDto;
import com.msa.jewelry.local.expense.dto.ExpenseIncomeAccountDto;
import com.msa.jewelry.local.expense.entity.ExpenseBankType;
import com.msa.jewelry.local.expense.entity.ExpenseExpenseAccount;
import com.msa.jewelry.local.expense.entity.ExpenseIncomeAccount;
import com.msa.jewelry.local.expense.repository.ExpenseBankTypeRepository;
import com.msa.jewelry.local.expense.repository.ExpenseExpenseAccountRepository;
import com.msa.jewelry.local.expense.repository.ExpenseIncomeAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExpenseMasterService 단위 테스트")
class ExpenseMasterServiceTest {

    private static final Long BANK_ID    = 10L;
    private static final Long INCOME_ID  = 20L;
    private static final Long EXPENSE_ID = 30L;

    @Mock ExpenseBankTypeRepository bankTypeRepository;
    @Mock ExpenseIncomeAccountRepository incomeAccountRepository;
    @Mock ExpenseExpenseAccountRepository expenseAccountRepository;

    @InjectMocks
    ExpenseMasterService service;

    // =======================================================================
    // Bank Type
    // =======================================================================
    @Nested
    @DisplayName("getAllBankTypes")
    class GetAllBankTypes {

        @Test
        @DisplayName("정상 — 2건 반환, 응답 변환 확인")
        void 정상() {
            ExpenseBankType bt1 = mockBankType(1L, "국민은행");
            ExpenseBankType bt2 = mockBankType(2L, "신한은행");
            given(bankTypeRepository.findAllByDeletedFalse()).willReturn(List.of(bt1, bt2));

            List<ExpenseBankTypeDto.Response> result = service.getAllBankTypes();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("국민은행");
            assertThat(result.get(1).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("빈 결과 — 빈 리스트")
        void 빈결과() {
            given(bankTypeRepository.findAllByDeletedFalse()).willReturn(Collections.emptyList());

            assertThat(service.getAllBankTypes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBankTypeById")
    class GetBankTypeById {

        @Test
        @DisplayName("정상 — Response 반환")
        void 정상() {
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType(BANK_ID, "국민은행")));

            ExpenseBankTypeDto.Response resp = service.getBankTypeById(BANK_ID);

            assertThat(resp.getId()).isEqualTo(BANK_ID);
            assertThat(resp.getName()).isEqualTo("국민은행");
        }

        @Test
        @DisplayName("미존재 — IllegalArgumentException")
        void 없음() {
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBankTypeById(BANK_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank type not found");
        }
    }

    @Nested
    @DisplayName("createBankType")
    class CreateBankType {

        @Test
        @DisplayName("정상 — 저장 후 Response 반환")
        void 정상() {
            ExpenseBankTypeDto.Request req = mock(ExpenseBankTypeDto.Request.class);
            given(req.getName()).willReturn("국민은행");
            given(req.getNote()).willReturn("법인 계좌");

            given(bankTypeRepository.save(any(ExpenseBankType.class)))
                    .willReturn(mockBankType(BANK_ID, "국민은행"));

            ExpenseBankTypeDto.Response resp = service.createBankType(req);

            assertThat(resp.getId()).isEqualTo(BANK_ID);
            verify(bankTypeRepository).save(any(ExpenseBankType.class));
        }

        @Test
        @DisplayName("중복 이름 — DataIntegrityViolationException 전파")
        void 중복() {
            ExpenseBankTypeDto.Request req = mock(ExpenseBankTypeDto.Request.class);
            given(req.getName()).willReturn("국민은행");

            willThrow(new DataIntegrityViolationException("dup"))
                    .given(bankTypeRepository).save(any(ExpenseBankType.class));

            assertThatThrownBy(() -> service.createBankType(req))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("updateBankType")
    class UpdateBankType {

        @Test
        @DisplayName("정상 — bankType.update() 호출 후 save")
        void 정상() {
            ExpenseBankType existing = mockBankType(BANK_ID, "옛이름");
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(existing));
            given(bankTypeRepository.save(existing)).willReturn(existing);

            ExpenseBankTypeDto.Request req = mock(ExpenseBankTypeDto.Request.class);
            given(req.getName()).willReturn("새이름");
            given(req.getNote()).willReturn("새 비고");

            service.updateBankType(BANK_ID, req);

            verify(existing).update("새이름", "새 비고");
            verify(bankTypeRepository).save(existing);
        }

        @Test
        @DisplayName("미존재 → IllegalArgumentException")
        void 없음() {
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.empty());
            ExpenseBankTypeDto.Request req = mock(ExpenseBankTypeDto.Request.class);

            assertThatThrownBy(() -> service.updateBankType(BANK_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank type not found");

            verify(bankTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("동시성 충돌 — OptimisticLockingFailureException 전파")
        void 낙관락() {
            ExpenseBankType existing = mockBankType(BANK_ID, "옛이름");
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(existing));
            willThrow(new OptimisticLockingFailureException("lock"))
                    .given(bankTypeRepository).save(any(ExpenseBankType.class));

            ExpenseBankTypeDto.Request req = mock(ExpenseBankTypeDto.Request.class);
            given(req.getName()).willReturn("새이름");

            assertThatThrownBy(() -> service.updateBankType(BANK_ID, req))
                    .isInstanceOf(OptimisticLockingFailureException.class);
        }
    }

    @Nested
    @DisplayName("deleteBankType")
    class DeleteBankType {

        @Test
        @DisplayName("정상 — softDelete 후 save")
        void 정상() {
            ExpenseBankType existing = mockBankType(BANK_ID, "삭제대상");
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(existing));
            given(bankTypeRepository.save(existing)).willReturn(existing);

            service.deleteBankType(BANK_ID);

            verify(existing).softDelete();
            verify(bankTypeRepository).save(existing);
        }

        @Test
        @DisplayName("미존재 — IllegalArgumentException")
        void 없음() {
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteBankType(BANK_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank type not found");

            verify(bankTypeRepository, never()).save(any());
        }
    }

    // =======================================================================
    // Income Account
    // =======================================================================
    @Nested
    @DisplayName("getAllIncomeAccounts")
    class GetAllIncomeAccounts {

        @Test
        @DisplayName("정상")
        void 정상() {
            given(incomeAccountRepository.findAllByDeletedFalse()).willReturn(
                    List.of(mockIncomeAccount(1L, "판매수입"), mockIncomeAccount(2L, "이자수입")));

            List<ExpenseIncomeAccountDto.Response> result = service.getAllIncomeAccounts();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("판매수입");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(incomeAccountRepository.findAllByDeletedFalse()).willReturn(Collections.emptyList());
            assertThat(service.getAllIncomeAccounts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getIncomeAccountById")
    class GetIncomeAccountById {

        @Test
        @DisplayName("정상")
        void 정상() {
            given(incomeAccountRepository.findById(INCOME_ID))
                    .willReturn(Optional.of(mockIncomeAccount(INCOME_ID, "판매수입")));

            ExpenseIncomeAccountDto.Response resp = service.getIncomeAccountById(INCOME_ID);

            assertThat(resp.getId()).isEqualTo(INCOME_ID);
        }

        @Test
        @DisplayName("미존재 → IllegalArgumentException")
        void 없음() {
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getIncomeAccountById(INCOME_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Income account not found");
        }
    }

    @Nested
    @DisplayName("createIncomeAccount")
    class CreateIncomeAccount {

        @Test
        @DisplayName("정상")
        void 정상() {
            ExpenseIncomeAccountDto.Request req = mock(ExpenseIncomeAccountDto.Request.class);
            given(req.getName()).willReturn("판매수입");
            given(req.getNote()).willReturn("일반 판매");
            given(incomeAccountRepository.save(any(ExpenseIncomeAccount.class)))
                    .willReturn(mockIncomeAccount(INCOME_ID, "판매수입"));

            ExpenseIncomeAccountDto.Response resp = service.createIncomeAccount(req);

            assertThat(resp.getId()).isEqualTo(INCOME_ID);
            verify(incomeAccountRepository).save(any(ExpenseIncomeAccount.class));
        }

        @Test
        @DisplayName("저장 실패 — 예외 전파")
        void 저장실패() {
            ExpenseIncomeAccountDto.Request req = mock(ExpenseIncomeAccountDto.Request.class);
            willThrow(new DataIntegrityViolationException("dup"))
                    .given(incomeAccountRepository).save(any(ExpenseIncomeAccount.class));

            assertThatThrownBy(() -> service.createIncomeAccount(req))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("updateIncomeAccount")
    class UpdateIncomeAccount {

        @Test
        @DisplayName("정상 — entity.update + save")
        void 정상() {
            ExpenseIncomeAccount existing = mockIncomeAccount(INCOME_ID, "옛이름");
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.of(existing));
            given(incomeAccountRepository.save(existing)).willReturn(existing);

            ExpenseIncomeAccountDto.Request req = mock(ExpenseIncomeAccountDto.Request.class);
            given(req.getName()).willReturn("새이름");
            given(req.getNote()).willReturn("새 비고");

            service.updateIncomeAccount(INCOME_ID, req);

            verify(existing).update("새이름", "새 비고");
            verify(incomeAccountRepository).save(existing);
        }

        @Test
        @DisplayName("미존재 → 예외")
        void 없음() {
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.empty());
            ExpenseIncomeAccountDto.Request req = mock(ExpenseIncomeAccountDto.Request.class);

            assertThatThrownBy(() -> service.updateIncomeAccount(INCOME_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Income account not found");
        }
    }

    @Nested
    @DisplayName("deleteIncomeAccount")
    class DeleteIncomeAccount {

        @Test
        @DisplayName("정상 — softDelete 호출")
        void 정상() {
            ExpenseIncomeAccount existing = mockIncomeAccount(INCOME_ID, "삭제대상");
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.of(existing));
            given(incomeAccountRepository.save(existing)).willReturn(existing);

            service.deleteIncomeAccount(INCOME_ID);

            verify(existing).softDelete();
            verify(incomeAccountRepository).save(existing);
        }

        @Test
        @DisplayName("미존재 → 예외")
        void 없음() {
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteIncomeAccount(INCOME_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Income account not found");

            verify(incomeAccountRepository, never()).save(any());
        }
    }

    // =======================================================================
    // Expense Account
    // =======================================================================
    @Nested
    @DisplayName("getAllExpenseAccounts")
    class GetAllExpenseAccounts {

        @Test
        @DisplayName("정상")
        void 정상() {
            given(expenseAccountRepository.findAllByDeletedFalse()).willReturn(
                    List.of(mockExpenseAccount(1L, "사무용품비"), mockExpenseAccount(2L, "임차료")));

            List<ExpenseExpenseAccountDto.Response> result = service.getAllExpenseAccounts();

            assertThat(result).hasSize(2);
            assertThat(result.get(1).getName()).isEqualTo("임차료");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(expenseAccountRepository.findAllByDeletedFalse()).willReturn(Collections.emptyList());
            assertThat(service.getAllExpenseAccounts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getExpenseAccountById")
    class GetExpenseAccountById {

        @Test
        @DisplayName("정상")
        void 정상() {
            given(expenseAccountRepository.findById(EXPENSE_ID))
                    .willReturn(Optional.of(mockExpenseAccount(EXPENSE_ID, "사무용품비")));

            ExpenseExpenseAccountDto.Response resp = service.getExpenseAccountById(EXPENSE_ID);

            assertThat(resp.getId()).isEqualTo(EXPENSE_ID);
            assertThat(resp.getName()).isEqualTo("사무용품비");
        }

        @Test
        @DisplayName("미존재 → 예외")
        void 없음() {
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getExpenseAccountById(EXPENSE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expense account not found");
        }
    }

    @Nested
    @DisplayName("createExpenseAccount")
    class CreateExpenseAccount {

        @Test
        @DisplayName("정상")
        void 정상() {
            ExpenseExpenseAccountDto.Request req = mock(ExpenseExpenseAccountDto.Request.class);
            given(req.getName()).willReturn("사무용품비");
            given(req.getNote()).willReturn("운영 잡비");
            given(expenseAccountRepository.save(any(ExpenseExpenseAccount.class)))
                    .willReturn(mockExpenseAccount(EXPENSE_ID, "사무용품비"));

            ExpenseExpenseAccountDto.Response resp = service.createExpenseAccount(req);

            assertThat(resp.getId()).isEqualTo(EXPENSE_ID);
        }

        @Test
        @DisplayName("저장 실패 — 예외 전파")
        void 저장실패() {
            ExpenseExpenseAccountDto.Request req = mock(ExpenseExpenseAccountDto.Request.class);
            willThrow(new DataIntegrityViolationException("dup"))
                    .given(expenseAccountRepository).save(any(ExpenseExpenseAccount.class));

            assertThatThrownBy(() -> service.createExpenseAccount(req))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("updateExpenseAccount")
    class UpdateExpenseAccount {

        @Test
        @DisplayName("정상 — entity.update + save")
        void 정상() {
            ExpenseExpenseAccount existing = mockExpenseAccount(EXPENSE_ID, "옛이름");
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.of(existing));
            given(expenseAccountRepository.save(existing)).willReturn(existing);

            ExpenseExpenseAccountDto.Request req = mock(ExpenseExpenseAccountDto.Request.class);
            given(req.getName()).willReturn("새이름");
            given(req.getNote()).willReturn("새 비고");

            service.updateExpenseAccount(EXPENSE_ID, req);

            verify(existing).update("새이름", "새 비고");
        }

        @Test
        @DisplayName("미존재 → 예외")
        void 없음() {
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.empty());
            ExpenseExpenseAccountDto.Request req = mock(ExpenseExpenseAccountDto.Request.class);

            assertThatThrownBy(() -> service.updateExpenseAccount(EXPENSE_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expense account not found");

            verify(expenseAccountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteExpenseAccount")
    class DeleteExpenseAccount {

        @Test
        @DisplayName("정상 — softDelete 호출")
        void 정상() {
            ExpenseExpenseAccount existing = mockExpenseAccount(EXPENSE_ID, "삭제대상");
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.of(existing));
            given(expenseAccountRepository.save(existing)).willReturn(existing);

            service.deleteExpenseAccount(EXPENSE_ID);

            verify(existing).softDelete();
            verify(expenseAccountRepository).save(existing);
        }

        @Test
        @DisplayName("미존재 → 예외")
        void 없음() {
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteExpenseAccount(EXPENSE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expense account not found");

            verify(expenseAccountRepository, never()).save(any());
        }
    }

    // =======================================================================
    // 헬퍼
    // =======================================================================
    private static ExpenseBankType mockBankType(Long id, String name) {
        ExpenseBankType bt = mock(ExpenseBankType.class);
        given(bt.getExpenseBankTypeId()).willReturn(id);
        given(bt.getBankTypeName()).willReturn(name);
        given(bt.getBankTypeNote()).willReturn("note-" + id);
        return bt;
    }

    private static ExpenseIncomeAccount mockIncomeAccount(Long id, String name) {
        ExpenseIncomeAccount acc = mock(ExpenseIncomeAccount.class);
        given(acc.getExpenseIncomeAccountId()).willReturn(id);
        given(acc.getIncomeAccountName()).willReturn(name);
        given(acc.getIncomeAccountNote()).willReturn("note-" + id);
        return acc;
    }

    private static ExpenseExpenseAccount mockExpenseAccount(Long id, String name) {
        ExpenseExpenseAccount acc = mock(ExpenseExpenseAccount.class);
        given(acc.getExpenseExpenseAccountId()).willReturn(id);
        given(acc.getExpenseAccountName()).willReturn(name);
        given(acc.getExpenseAccountNote()).willReturn("note-" + id);
        return acc;
    }
}
