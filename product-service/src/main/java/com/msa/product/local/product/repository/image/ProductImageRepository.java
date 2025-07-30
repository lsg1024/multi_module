package com.msa.product.local.product.repository.image;

import com.msa.product.local.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long>, CustomProductImageRepository {
}
