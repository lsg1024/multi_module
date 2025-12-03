package com.msa.product.local.stone.stone.repository;

import com.msa.product.local.stone.stone.entity.Stone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoneRepository extends JpaRepository<Stone, Long>, CustomStoneRepository {
    boolean existsByStoneName(String stoneName);
    boolean existsByStoneId(Long stoneId);
    Optional<Stone> findByStoneName(String stoneName);
}
