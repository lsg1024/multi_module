package com.msa.jewelry.local.stone.repository;

import com.msa.jewelry.local.stone.entity.Stone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoneRepository extends JpaRepository<Stone, Long>, CustomStoneRepository {
    boolean existsByStoneName(String stoneName);
    boolean existsByStoneNameAndStoneIdNot(String stoneName, Long stoneId);
    boolean existsByStoneId(Long stoneId);
    Optional<Stone> findByStoneName(String stoneName);
    Optional<Stone> findByStoneNameIgnoreCase(String stoneName);
}
