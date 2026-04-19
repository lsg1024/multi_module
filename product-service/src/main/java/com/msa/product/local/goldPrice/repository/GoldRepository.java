package com.msa.product.local.goldPrice.repository;

import com.msa.product.local.goldPrice.entity.Gold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GoldRepository extends JpaRepository<Gold, Long> {
    Optional<Gold> findTopByOrderByGoldIdDesc();
    List<Gold> findTop365ByOrderByCreateDateDesc();

    @Query(value = "SELECT * FROM gold ORDER BY create_date DESC LIMIT 365", nativeQuery = true)
    List<Gold> findAllByOrderByCreateDateDesc();
}
