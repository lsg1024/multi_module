package com.msa.account.local.ledger.repository;

import com.msa.account.local.ledger.domain.entity.AssetType;
import com.msa.account.local.ledger.domain.entity.Ledger;
import com.msa.account.local.ledger.domain.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    Page<Ledger> findByAssetTypeAndLedgerDateBetweenOrderByLedgerDateDesc(
            AssetType assetType, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Ledger> findByLedgerDateBetweenOrderByLedgerDateDesc(
            LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.transactionType = 'INCOME' THEN l.goldAmount ELSE l.goldAmount * -1 END), 0) " +
            "FROM Ledger l WHERE l.assetType = 'GOLD'")
    BigDecimal calculateGoldBalance();

    @Query("SELECT COALESCE(SUM(CASE WHEN l.transactionType = 'INCOME' THEN l.moneyAmount ELSE l.moneyAmount * -1 END), 0) " +
            "FROM Ledger l WHERE l.assetType = 'MONEY'")
    Long calculateMoneyBalance();
}
