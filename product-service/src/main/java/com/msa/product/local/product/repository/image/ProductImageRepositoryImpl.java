package com.msa.product.local.product.repository.image;

import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.dto.QProductImageDto_Response;
import com.msa.product.local.product.entity.QProductImage;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.List;

import static com.msa.product.local.product.entity.QProduct.product;
import static com.msa.product.local.product.entity.QProductImage.productImage;

public class ProductImageRepositoryImpl implements CustomProductImageRepository {

    private final JPAQueryFactory query;

    public ProductImageRepositoryImpl(EntityManager em) {
        this.query = new JPAQueryFactory(em);
    }


    @Override
    public List<ProductImageDto.Response> findImagesByProductId(Long productId) {
        return query
                .select(new QProductImageDto_Response(
                        productImage.imagePath
                ))
                .from(productImage)
                .join(productImage.product, product)
                .where(productImage.product.productId.eq(productId))
                .orderBy(productImage.product.productId.asc())
                .fetch();
    }
}
