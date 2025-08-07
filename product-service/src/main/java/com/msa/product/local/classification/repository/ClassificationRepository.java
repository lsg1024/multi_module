package com.msa.product.local.classification.repository;

import com.msa.product.local.classification.entity.Classification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassificationRepository extends JpaRepository<Classification, Long>, CustomClassificationRepository {
    @Query("select c.classificationName from Classification c where c.classificationId= :id")
    String findByClassificationName(Long id);
    boolean existsByClassificationName(String name);
}
