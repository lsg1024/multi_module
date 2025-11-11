package com.msa.order.local.sale.repository;

import com.msa.order.local.sale.entity.SaleItem;
import com.msa.order.local.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    boolean existsByStock(Stock stock);

    @Query("select si from SaleItem si " +
            "left join fetch si.stock s " +
            "left join fetch s.orderStones os " +
            "where si.flowCode= :flowCode")
    Optional<SaleItem> findByFlowCode(Long flowCode);
}
