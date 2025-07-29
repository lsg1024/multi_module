package com.msa.product.local.classification.repository;

import com.msa.product.local.classification.entity.Classification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassificationRepository extends JpaRepository<Classification, Long>, CustomClassificationRepository {
    boolean existsByClassificationName(String name);
}
