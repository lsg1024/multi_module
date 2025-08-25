package com.msa.order.local.stock.repository;

import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.stock.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    boolean existsByOrder(Orders order);

    boolean existsByFlowCode(Long flowCode);

    @Query("select s from Stock s " +
            "left join fetch s.orderStones os " +
            "where s.flowCode= :id")
    Optional<Stock> findByFlowCode(@Param("id") Long flowCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.flowCode = :flowCode")
    Optional<Stock> findByFlowCodeForUpdate(@Param("flowCode") Long flowCode);
}
