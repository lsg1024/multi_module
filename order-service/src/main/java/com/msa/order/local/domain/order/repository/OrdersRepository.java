package com.msa.order.local.domain.order.repository;

import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    boolean existsByOrderIdAndProductStatusIn(Long orderId, List<ProductStatus> statuses);
    @Query("""
    select distinct o from Orders o
     left join fetch o.orderProduct op
     left join fetch o.priority p
     left join o.statusHistory sh
     where o.orderCode = :id
    """)
    Optional<Orders> findAggregate(@Param("id") Long orderId);
}
