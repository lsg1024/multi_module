package com.msa.order.local.sale.repository;

import com.msa.order.local.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findBySaleCode(Long saleCode);
    @Query("select s from Sale s " +
            "where s.accountId= :accountId " +
            "and s.createDate between :start and :end " +
            "order by s.createDate desc " +
            "limit 1")
    Optional<Sale> findLatestSaleByAccountIdAndDate(@Param("accountId") Long accountId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    @Query("select s.saleCode from Sale s " +
            "where s.accountId = :accountId " +
            "and s.createDate between :start and :end " +
            "order by s.createDate desc " +
            "limit 1")
    Optional<Long> findSaleCodeByAccountIdAndDate(@Param("accountId") Long accountId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
