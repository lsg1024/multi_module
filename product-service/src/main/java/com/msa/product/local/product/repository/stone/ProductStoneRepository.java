package com.msa.product.local.product.repository.stone;

import com.msa.product.local.product.entity.ProductStone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductStoneRepository extends JpaRepository<ProductStone, Long>, CustomProductStoneRepository {
    @Query("select ps from ProductStone ps " +
            "left join fetch ps.stone " +
            "where ps.productStoneId in :productStoneIds")
    List<ProductStone> findByProductStoneIds(@Param("productStoneIds") List<Long> productStoneIds);

    Optional<ProductStone> findByProductProductIdAndStoneStoneId(Long productId, Long stoneId);
}
