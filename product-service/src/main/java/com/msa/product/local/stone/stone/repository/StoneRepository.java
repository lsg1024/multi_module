package com.msa.product.local.stone.stone.repository;

import com.msa.product.local.stone.stone.entity.Stone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoneRepository extends JpaRepository<Stone, Long>, CustomStoneRepository {
    boolean existsByStoneName(String stoneName);
}
