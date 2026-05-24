package com.msa.jewelry.product.internal.catalog.dto;

import com.msa.jewelry.product.internal.classification.dto.ClassificationDto;
import com.msa.jewelry.product.internal.material.dto.MaterialDto;
import com.msa.jewelry.product.internal.product.dto.ProductImageDto;
import com.msa.jewelry.product.internal.set.dto.SetTypeDto;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "카탈로그(매장이 보는 상품) DTO 묶음 — 가격 정보 및 제조사 정보 제외")
public class CatalogProductDto {

    /**
     * 카탈로그 목록 페이지용 DTO
     * 제외 정보: 가격(productPurchaseCost, productLaborCost, productGoldPrice), 제조사(factoryId, factoryName, productFactoryName)
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "카탈로그 목록 페이지 DTO — 가격/제조사 정보 제외")
    public static class Page {
        @Schema(description = "상품 ID", example = "1001")
        private String productId;
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
        private String productName;
        @Schema(description = "상품 무게(그램)", example = "3.50")
        private String productWeight;
        @Schema(description = "재질명", example = "18K")
        private String productMaterial;
        @Schema(description = "색상명", example = "옐로골드")
        private String productColor;
        @Schema(description = "상품 비고", example = "메인 0.3ct")
        private String productNote;
        @Schema(description = "대표 이미지")
        private ProductImageDto.Response image;
        @Schema(description = "스톤 목록(목록용, 가격 제외)")
        private List<CatalogStoneDto.PageResponse> productStones;
        @Setter
        @Schema(description = "현재 재고 수량", example = "10")
        private int stockCount;

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
            this.stockCount = 0;
        }
    }

    /**
     * 카탈로그 상세 페이지용 DTO
     * 제외 정보: 가격, 제조사(factoryId, factoryName, productFactoryName)
     */
    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "카탈로그 상세 페이지 DTO — 가격/제조사 정보 제외")
    public static class Detail {
        @Schema(description = "상품 ID", example = "1001")
        private String productId;
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
        private String productName;
        @Schema(description = "기준 무게(그램)", example = "3.50")
        private String standardWeight;
        @Schema(description = "관련번호", example = "R-2024-SERIES")
        private String productRelatedNumber;
        @Schema(description = "상품 비고", example = "메인 0.3ct")
        private String productNote;
        @Schema(description = "세트 타입 정보")
        private SetTypeDto.ResponseSingle setTypeDto;
        @Schema(description = "분류 정보")
        private ClassificationDto.ResponseSingle classificationDto;
        @Schema(description = "재질 정보")
        private MaterialDto.ResponseSingle materialDto;
        @Schema(description = "스톤 목록 (가격 제외)")
        private List<CatalogStoneDto.Response> productStoneDtos;
        @Schema(description = "상품 이미지 목록")
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
    @Schema(description = "카탈로그 관련 상품 DTO — 같은 모델군 상품 요약")
    public static class RelatedProduct {
        @Schema(description = "상품 ID", example = "1001")
        private Long productId;
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
        private String productName;
        @Schema(description = "대표 이미지 경로", example = "https://cdn.example.com/products/abc.jpg")
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
