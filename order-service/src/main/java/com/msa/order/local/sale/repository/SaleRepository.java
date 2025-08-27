package com.msa.order.local.sale.repository;

import com.msa.order.local.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findByStoreIdAndSaleDate(Long storeId, LocalDate saleDate);

    Optional<Sale> findBySaleCode(Long saleCode);
}
