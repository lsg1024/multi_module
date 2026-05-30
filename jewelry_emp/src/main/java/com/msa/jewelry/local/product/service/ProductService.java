package com.msa.jewelry.local.product.service;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.product.dto.ProductDetailDto;
import com.msa.jewelry.local.product.dto.ProductDetailView;
import com.msa.jewelry.local.product.dto.ProductDto;
import com.msa.jewelry.local.product.dto.ProductImageView;
import com.msa.jewelry.local.product.dto.ProductView;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Long saveProduct(String token, ProductDto productDto);

    ProductDto.Detail getProduct(Long productId);

    CustomPage<ProductDto.Page> getProducts(String search, String searchField, String searchMin, String searchMax, String sortField, String sortOrder, String grade,
                                                    String setTypeFilter, String classificationFilter, String factoryFilter, Pageable pageable);

    void updateProduct(String accessToken, Long productId, ProductDto.Update updateDto);

    void deletedProduct(String accessToken, Long productId);

    ProductDetailDto getProductInfoByName(String productName);

    List<ProductDetailDto.StoneInfo> getProductStonesByName(String productName);

    void updateProductFactoryName(Long productId, String productFactoryName);

    ProductDetailDto getProductInfo(Long id, String grade);

    List<ProductDto.RelatedProduct> getRelatedProducts(Long productId);

    ProductView findProductByName(String productName);

    ProductDetailView getProductDetail(Long productId, String grade);

    ProductDetailView findProductDetailByName(String productName);

    Map<Long, ProductImageView> getProductImages(List<Long> productIds);

}
