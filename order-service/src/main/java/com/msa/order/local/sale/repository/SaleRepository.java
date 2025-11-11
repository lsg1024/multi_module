package com.msa.order.local.sale.repository;

import com.msa.order.local.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Query("select s from Sale s " +
            "left join fetch s.salePayments " +
            "left join fetch s.items " +
            "where s.saleCode= :id")
    Optional<Sale> findBySaleCode(@Param("id") Long saleCode);
    Optional<Sale> findByAccountIdAndCreateDate(Long storeId, LocalDateTime saleDate);
}
