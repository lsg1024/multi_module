package com.msa.account.local.expense.repository;

import com.msa.account.local.expense.domain.entity.ExpenseRecord;
import com.msa.common.global.common_enum.expense_enum.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRecordRepository extends JpaRepository<ExpenseRecord, Long> {

    @Query("SELECT er FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.recordDate BETWEEN :startDate AND :endDate")
    Page<ExpenseRecord> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    @Query("SELECT er FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.recordDate BETWEEN :startDate AND :endDate " +
           "AND er.expenseType = :expenseType")
    Page<ExpenseRecord> findByDateRangeAndExpenseType(@Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate,
                                                      @Param("expenseType") ExpenseType expenseType,
                                                      Pageable pageable);

    @Query("SELECT er FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.recordDate BETWEEN :startDate AND :endDate " +
           "AND er.bankType.expenseBankTypeId = :bankTypeId")
    Page<ExpenseRecord> findByDateRangeAndBankType(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("bankTypeId") Long bankTypeId,
                                                   Pageable pageable);

    @Query("SELECT er FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.recordDate BETWEEN :startDate AND :endDate " +
           "AND er.counterparty LIKE %:counterparty%")
    Page<ExpenseRecord> findByDateRangeAndCounterparty(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate,
                                                       @Param("counterparty") String counterparty,
                                                       Pageable pageable);

    @Query("SELECT er FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.recordDate BETWEEN :startDate AND :endDate " +
           "AND er.expenseType = :expenseType " +
           "AND er.bankType.expenseBankTypeId = :bankTypeId")
    Page<ExpenseRecord> findByDateRangeAndExpenseTypeAndBankType(@Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate,
                                                                 @Param("expenseType") ExpenseType expenseType,
                                                                 @Param("bankTypeId") Long bankTypeId,
                                                                 Pageable pageable);

    @Query("SELECT COALESCE(SUM(er.weight), 0) FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.expenseType = 'INCOME' " +
           "AND er.recordDate BETWEEN :startDate AND :endDate")
    BigDecimal sumIncomeWeight(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(er.supplyAmount), 0) FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.expenseType = 'INCOME' " +
           "AND er.recordDate BETWEEN :startDate AND :endDate")
    Long sumIncomeAmount(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(er.weight), 0) FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.expenseType = 'EXPENSE' " +
           "AND er.recordDate BETWEEN :startDate AND :endDate")
    BigDecimal sumExpenseWeight(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(er.supplyAmount), 0) FROM ExpenseRecord er " +
           "WHERE er.deleted = false " +
           "AND er.expenseType = 'EXPENSE' " +
           "AND er.recordDate BETWEEN :startDate AND :endDate")
    Long sumExpenseAmount(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    List<ExpenseRecord> findByDeletedFalse();
}
