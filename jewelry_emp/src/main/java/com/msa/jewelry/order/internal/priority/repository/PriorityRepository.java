package com.msa.jewelry.order.internal.priority.repository;

import com.msa.jewelry.order.internal.priority.entitiy.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriorityRepository extends JpaRepository<Priority, Long> {
    Optional<Priority> findByPriorityName(String priorityName);
}
