package com.msa.order.local.domain.stock.repository;

import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.stock.entity.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
