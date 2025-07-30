package com.msa.product.local.product.repository.stone;

import com.msa.product.local.product.entity.ProductStone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStoneRepository extends JpaRepository<ProductStone, Long>, CustomProductStoneRepository {
}
