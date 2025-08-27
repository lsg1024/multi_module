package com.msa.order.local.sale.repository;

import com.msa.order.local.sale.entity.Sale;
import com.msa.order.local.sale.entity.SaleItem;
import com.msa.order.local.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    boolean existsByStock(Stock stock);

    boolean existsBySale(Sale sale);

    @Query("select si from SaleItem si " +
            "left join fetch Stock " +
            "where si.saleCode= :saleCode")
    Optional<SaleItem> findBySaleCode(Long saleCode);
}
