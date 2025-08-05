package com.msa.product.local.product.repository;

import com.msa.product.local.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, CustomProductRepository {
    @Query("select p.productName from Product p where p.productId= :productId")
    String findByProductName(Long productId);
    boolean existsByProductName(String productName);

    boolean existsByProductNameAndProductIdNot(String productName, Long productId);
    @Query("select p from Product p " +
            "join fetch p.setType " +
            "join fetch p.material " +
            "join fetch p.classification " +
            "where p.productId = :id")
    Optional<Product> findWithAllOptionsById(@Param("id") Long id);
}
