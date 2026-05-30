package com.msa.jewelry.local.priority.repository;

import com.msa.jewelry.local.priority.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriorityRepository extends JpaRepository<Priority, Long> {
    Optional<Priority> findByPriorityName(String priorityName);
}
