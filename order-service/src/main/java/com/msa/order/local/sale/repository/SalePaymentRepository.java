package com.msa.order.local.sale.repository;

import com.msa.order.local.sale.entity.Sale;
import com.msa.order.local.sale.entity.SalePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {
    Optional<SalePayment> findByIdempotencyKey(String idempKey);

    @Query("select sp from SalePayment sp " +
            "left join fetch sp.sale s " +
            "where sp.flowCode= :flowCode")
    Optional<SalePayment> findByFlowCode(Long flowCode);
    Optional<SalePayment> findTopByStoreIdOrderByCreateDateDesc(Long storeId);

    boolean existsBySale(Sale sale);
}
