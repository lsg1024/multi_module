package com.msa.product.local.catalog.dto;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.set.dto.SetTypeDto;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 카탈로그용 상품 DTO (가격 정보 제외)
 */
@Getter
@NoArgsConstructor
public class CatalogProductDto {

    /**
     * 카탈로그 목록 페이지용 DTO
     * 제외 정보: 가격(productPurchaseCost, productLaborCost, productGoldPrice), 제조사(factoryId, factoryName, productFactoryName)
     */
    @Getter
    @NoArgsConstructor
    public static class Page {
        private String productId;
        private String productName;
        private String productWeight;
        private String productMaterial;
        private String productColor;
        private String productNote;
        private ProductImageDto.Response image;
        private List<CatalogStoneDto.PageResponse> productStones;

        @Builder
        @QueryProjection
        public Page(String productId, String productName,
                    String productWeight, String productMaterial, String productColor,
                    String productNote, ProductImageDto.Response image) {
            this.productId = productId;
            this.productName = productName;
            this.productWeight = productWeight;
            this.productMaterial = productMaterial;
            this.productColor = productColor;
            this.productNote = productNote;
            this.image = image;
            this.productStones = new ArrayList<>();
        }
    }

    /**
     * 카탈로그 상세 페이지용 DTO
     * 제외 정보: 가격, 제조사(factoryId, factoryName, productFactoryName)
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class Detail {
        private String productId;
        private String productName;
        private String standardWeight;
        private String productRelatedNumber;
        private String productNote;
        private SetTypeDto.ResponseSingle setTypeDto;
        private ClassificationDto.ResponseSingle classificationDto;
        private MaterialDto.ResponseSingle materialDto;
        private List<CatalogStoneDto.Response> productStoneDtos;
        private List<ProductImageDto.Response> productImageDtos;

        @Builder
        public Detail(String productId, String productName, String standardWeight,
                      String productRelatedNumber, String productNote,
                      SetTypeDto.ResponseSingle setTypeDto,
                      ClassificationDto.ResponseSingle classificationDto,
                      MaterialDto.ResponseSingle materialDto,
                      List<CatalogStoneDto.Response> productStoneDtos,
                      List<ProductImageDto.Response> productImageDtos) {
            this.productId = productId;
            this.productName = productName;
            this.standardWeight = standardWeight;
            this.productRelatedNumber = productRelatedNumber;
            this.productNote = productNote;
            this.setTypeDto = setTypeDto;
            this.classificationDto = classificationDto;
            this.materialDto = materialDto;
            this.productStoneDtos = productStoneDtos;
            this.productImageDtos = productImageDtos;
        }

        @QueryProjection
        public Detail(String productId, String productName, String standardWeight,
                      String productRelatedNumber, String productNote,
                      SetTypeDto.ResponseSingle setTypeDto,
                      ClassificationDto.ResponseSingle classificationDto,
                      MaterialDto.ResponseSingle materialDto) {
            this.productId = productId;
            this.productName = productName;
            this.standardWeight = standardWeight;
            this.productRelatedNumber = productRelatedNumber;
            this.productNote = productNote;
            this.setTypeDto = setTypeDto;
            this.classificationDto = classificationDto;
            this.materialDto = materialDto;
        }
    }

    /**
     * 관련 상품 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class RelatedProduct {
        private Long productId;
        private String productName;
        private String imagePath;

        @Builder
        @QueryProjection
        public RelatedProduct(Long productId, String productName, String imagePath) {
            this.productId = productId;
            this.productName = productName;
            this.imagePath = imagePath;
        }
    }
}
