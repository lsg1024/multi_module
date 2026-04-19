package com.msa.order.local.stock.repository;

import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.stock.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    boolean existsByOrder(Orders order);

    boolean existsByFlowCode(Long flowCode);

    @Query("select s from Stock s " +
            "left join fetch s.orderStones os " +
            "where s.flowCode= :id")
    Optional<Stock> findByFlowCode(@Param("id") Long flowCode);
    @Query("select s from Stock s " +
            "left join fetch s.orderStones os " +
            "where s.flowCode in :ids")
    List<Stock> findByFlowCodeIn(@Param("ids") List<Long> flowCodes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.flowCode = :flowCode")
    Optional<Stock> findByFlowCodeForUpdate(@Param("flowCode") Long flowCode);

    /**
     * 상품명 목록에 해당하는 활성 재고 수량을 상품명별로 집계한다.
     * 활성 재고: STOCK, NORMAL, RENTAL 상태이며 삭제되지 않은 것
     */
    @Query("SELECT s.product.productName, COUNT(s) FROM Stock s " +
            "WHERE s.product.productName IN :productNames " +
            "AND s.stockDeleted = false " +
            "AND s.orderStatus IN (com.msa.order.local.order.entity.order_enum.OrderStatus.STOCK, " +
            "com.msa.order.local.order.entity.order_enum.OrderStatus.NORMAL, " +
            "com.msa.order.local.order.entity.order_enum.OrderStatus.RENTAL) " +
            "GROUP BY s.product.productName")
    List<Object[]> countByProductNames(@Param("productNames") List<String> productNames);
}
