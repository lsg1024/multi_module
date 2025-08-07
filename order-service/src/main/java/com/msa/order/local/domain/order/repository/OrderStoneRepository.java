package com.msa.order.local.domain.order.repository;

import com.msa.order.local.domain.order.entity.OrderStone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStoneRepository extends JpaRepository<OrderStone, Long> {
}
