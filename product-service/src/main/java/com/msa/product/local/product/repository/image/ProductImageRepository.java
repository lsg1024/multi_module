package com.msa.product.local.product.repository.image;

import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long>, CustomProductImageRepository {
    List<ProductImage> findByProduct(Product product);

    void deleteAllByProduct(Product product);
    boolean existsByProduct_ProductId(Long productId);
}
