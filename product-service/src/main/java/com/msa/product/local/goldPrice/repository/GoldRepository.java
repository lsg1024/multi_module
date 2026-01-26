package com.msa.product.local.goldPrice.repository;

import com.msa.product.local.goldPrice.entity.Gold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoldRepository extends JpaRepository<Gold, Long> {
    Optional<Gold> findTopByOrderByGoldIdDesc();
    List<Gold> findAllByOrderByCreateDateDesc();
}
