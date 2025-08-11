package com.msa.order.local.domain.priority.repository;

import com.msa.order.local.domain.priority.entitiy.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriorityRepository extends JpaRepository<Priority, Long> {
    Optional<Priority> findByPriorityName(String priorityName);
}
