package com.msa.order.local.order.repository;

import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    boolean existsByFlowCodeAndProductStatusIn(Long flowCode, List<ProductStatus> statuses);
    @Query("""
    select distinct o from Orders o
     left join fetch o.orderProduct op
     left join fetch o.orderStones os
     left join fetch o.priority p
     where o.flowCode= :id
    """)
    Optional<Orders> findByFlowCode(@Param("id") Long flowCode);
}
