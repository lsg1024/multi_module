package com.msa.jewelry.local.product.dto;

import com.msa.jewelry.local.classification.dto.ClassificationDto;
import com.msa.jewelry.local.material.dto.MaterialDto;
import com.msa.jewelry.local.set.dto.SetTypeDto;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "상품 등록 요청 DTO — 신규 상품 생성")
public class ProductDto {

    @NotNull(message = "필수 입력 값입니다.")
    @Positive(message = "factoryId는 1 이상의 정수여야 합니다.")
    @Schema(description = "제조사 ID", example = "5")
    private Long factoryId;
    @NotBlank(message = "제조사 품명은 필수 입력값입니다.")
    @Schema(description = "제조사가 부여한 제품명/모델명", example = "R-2024-001")
    private String productFactoryName;
    @NotBlank(message = "제품명은 필수 입력값입니다.")
    @Schema(description = "상품명 (시스템 고유 이름)", example = "프로포즈 솔리테어 반지")
    private String productName;
    @Schema(description = "세트 타입 ID (문자열)", example = "1")
    private String setType; // setId
    @Schema(description = "분류 ID (문자열)", example = "1")
    private String classification; // classificationId
    @Schema(description = "재질 ID (문자열)", example = "1")
    private String material; // materialId
    @Schema(description = "기준 무게(그램, 문자열)", example = "3.50")
    private String standardWeight;
    @Schema(description = "상품 비고", example = "메인 스톤 0.3ct")
    private String productNote;
    @Schema(description = "관련번호 — 같은 모델군 묶음용", example = "R-2024-SERIES")
    private String productRelatedNumber;
    @Valid
    @Schema(description = "색상별 등급 공임 정책 그룹 목록")
    private List<ProductWorkGradePolicyGroupDto> productWorkGradePolicyGroupDto;
    @Schema(description = "상품에 매핑할 스톤 목록")
    private List<ProductStoneDto> productStoneDtos; //상품용 <- stone 호출

    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 상세 응답 DTO (가격 정보 포함)")
    public static class Detail {
        @Schema(description = "상품 ID", example = "1001")
        private String productId;
        @Schema(description = "제조사 ID", example = "5")
        private Long factoryId;
        @Schema(description = "제조사명", example = "한국주얼리")
        private String factoryName;
        @Schema(description = "제조사가 부여한 제품명", example = "R-2024-001")
        private String productFactoryName;
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
        private String productName;
        @Schema(description = "기준 무게(그램)", example = "3.50")
        private String standardWeight;
        @Schema(description = "관련번호 (같은 모델군 묶음용)", example = "R-2024-SERIES")
        private String productRelatedNumber;
        @Schema(description = "상품 비고", example = "메인 스톤 0.3ct")
        private String productNote;
        @Schema(description = "세트 타입 정보")
        private SetTypeDto.ResponseSingle setTypeDto;
        @Schema(description = "분류 정보")
        private ClassificationDto.ResponseSingle classificationDto;
        @Schema(description = "재질 정보")
        private MaterialDto.ResponseSingle materialDto;
        @Schema(description = "공임 정책 그룹 목록 (색상별)")
        private List<ProductWorkGradePolicyGroupDto.Response> productWorkGradePolicyGroupDto;
        @Schema(description = "상품 스톤 매핑 목록")
        private List<ProductStoneDto.Response> productStoneDtos;
        @Schema(description = "상품 이미지 목록")
        private List<ProductImageDto.Response> productImageDtos;

        @Builder
        @QueryProjection
        public Detail(String productId, Long factoryId, String factoryName, String productFactoryName, String productName, String standardWeight, String productRelatedNumber, String productNote, SetTypeDto.ResponseSingle setTypeDto, ClassificationDto.ResponseSingle classificationDto, MaterialDto.ResponseSingle materialDto, List<ProductWorkGradePolicyGroupDto.Response> productWorkGradePolicyGroupDto, List<ProductStoneDto.Response> productStoneDtos, List<ProductImageDto.Response> productImageDtos) {
            this.productId = productId;
            this.factoryId = factoryId;
            this.factoryName = factoryName;
            this.productFactoryName = productFactoryName;
            this.productName = productName;
            this.standardWeight = standardWeight;
            this.productRelatedNumber = productRelatedNumber;
            this.productNote = productNote;
            this.setTypeDto = setTypeDto;
            this.classificationDto = classificationDto;
            this.materialDto = materialDto;
            this.productWorkGradePolicyGroupDto = productWorkGradePolicyGroupDto;
            this.productStoneDtos = productStoneDtos;
            this.productImageDtos = productImageDtos;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 부분 수정 요청 DTO")
    public static class Update {
        @Schema(description = "제조사 ID", example = "5")
        private Long factoryId;
        @NotBlank(message = "제조사 품명은 필수 입력값입니다.")
        @Schema(description = "제조사가 부여한 제품명", example = "R-2024-001")
        private String productFactoryName;
        @NotBlank(message = "제품명은 필수 입력값입니다.")
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
        private String productName;
        @Schema(description = "세트 타입 ID (문자열)", example = "1")
        private String setType; // setId
        @Schema(description = "분류 ID (문자열)", example = "1")
        private String classification; // classificationId
        @Schema(description = "재질 ID (문자열)", example = "1")
        private String material; // materialId
        @Schema(description = "기준 무게 (그램)", example = "3.50")
        private String standardWeight;
        @Schema(description = "관련번호", example = "R-2024-SERIES")
        private String productRelatedNumber;
        @Schema(description = "상품 비고", example = "메인 스톤 0.3ct")
        private String productNote;
        @Schema(description = "공임 정책 그룹 요청 목록 (색상별)")
        private List<ProductWorkGradePolicyGroupDto.Request> productWorkGradePolicyGroupDto; // 상품 판매 공임
        @Schema(description = "상품 스톤 매핑 요청 목록")
        private List<ProductStoneDto.Request> productStoneDtos; //상품용 <- stone 호출

    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 페이지 목록용 응답 DTO (목록 화면)")
    public static class Page {
        @Schema(description = "상품 ID", example = "1001")
        private String productId;
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
        private String productName;
        @Schema(description = "제조사가 부여한 제품명", example = "R-2024-001")
        private String productFactoryName;
        @Schema(description = "제조사 ID", example = "5")
        private String factoryId;
        @Schema(description = "제조사명", example = "한국주얼리")
        private String factoryName;
        @Schema(description = "상품 무게 (그램)", example = "3.50")
        private String productWeight;
        @Schema(description = "재질명", example = "18K")
        private String productMaterial;
        @Schema(description = "색상명", example = "옐로골드")
        private String productColor;
        @Schema(description = "상품 비고", example = "메인 0.3ct")
        private String productNote;
        @Schema(description = "상품 매입가 (원)", example = "300000")
        private String productPurchaseCost;
        @Schema(description = "상품 공임 (원)", example = "50000")
        private String productLaborCost;
        @Schema(description = "조회 시점 금시세 단가 (원)", example = "350000")
        private Integer productGoldPrice;
        @Schema(description = "대표 이미지")
        private ProductImageDto.Response image;
        @Schema(description = "상품 스톤 목록(목록용)")
        private List<ProductStoneDto.PageResponse> productStones;

        @Builder
        @QueryProjection
        public Page(String productId, String productName, String productFactoryName, String productWeight, String productMaterial, String productColor, String productNote, String productPurchaseCost, String productLaborCost, String factoryId, String factoryName, ProductImageDto.Response image) {
            this.productId = productId;
            this.productName = productName;
            this.productFactoryName = productFactoryName;
            this.productWeight = productWeight;
            this.productMaterial = productMaterial;
            this.productColor = productColor;
            this.productNote = productNote;
            this.productPurchaseCost = productPurchaseCost;
            this.productLaborCost = productLaborCost;
            this.factoryId = factoryId;
            this.factoryName = factoryName;
            this.image = image;
            this.productStones = new ArrayList<>();
        }

        public void updateGoldPrice(Integer goldPrice) {
            this.productGoldPrice = goldPrice;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "관련 상품 응답 DTO — 같은 모델군에 속한 상품 요약")
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
