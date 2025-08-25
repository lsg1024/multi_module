package com.msa.order.local.sale.repository;

import com.msa.order.local.sale.entity.SaleItem;
import com.msa.order.local.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    boolean existsByStock(Stock stock);
}
