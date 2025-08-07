package com.msa.order.local.domain.priority.repository;

import com.msa.order.local.domain.priority.entitiy.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriorityRepository extends JpaRepository<Priority, Long> {
    Priority findByPriorityName(String priorityName);
}
