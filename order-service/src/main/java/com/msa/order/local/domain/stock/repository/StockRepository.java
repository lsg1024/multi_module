package com.msa.order.local.domain.stock.repository;

import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.stock.entity.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    boolean existsByOrder(Orders order);

    boolean existsByFlowCode(Long flowCode);
    Optional<Stock> findByFlowCode(Long flowCode);
}
