package com.msa.order.local.domain.order.repository;

import com.msa.order.local.domain.order.entity.OrderStatus;
import com.msa.order.local.domain.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    boolean existsByOrderIdAndOrderStatusIn(Long orderId, List<OrderStatus> statuses);
    @Query("select distinct o from Orders o " +
            "left join fetch o.orderProduct op " +
            "left join fetch o.orderStones s " +
            "where o.orderId= :id")
    Optional<Orders> findAggregate(@Param("id") Long id);
}
