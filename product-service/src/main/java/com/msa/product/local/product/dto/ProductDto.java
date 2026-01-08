package com.msa.product.local.product.dto;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.set.dto.SetTypeDto;
import com.querydsl.core.annotations.QueryProjection;
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
public class ProductDto {

    @NotNull(message = "필수 입력 값입니다.")
    @Positive(message = "factoryId는 1 이상의 정수여야 합니다.")
    private Long factoryId;
    @NotBlank(message = "제조사 품명은 필수 입력값입니다.")
    private String productFactoryName;
    @NotBlank(message = "제품명은 필수 입력값입니다.")
    private String productName;
    private String setType; // setId
    private String classification; // classificationId
    private String material; // materialId
    private String standardWeight;
    private String productNote;
    private String productRelatedNumber;
    @Valid
    private List<ProductWorkGradePolicyGroupDto> productWorkGradePolicyGroupDto;
    private List<ProductStoneDto> productStoneDtos; //상품용 <- stone 호출

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Detail {
        private String productId;
        private Long factoryId;
        private String factoryName;
        private String productFactoryName;
        private String productName;
        private String standardWeight;
        private String productRelatedNumber;
        private String productNote;
        private SetTypeDto.ResponseSingle setTypeDto;
        private ClassificationDto.ResponseSingle classificationDto;
        private MaterialDto.ResponseSingle materialDto;
        private List<ProductWorkGradePolicyGroupDto.Response> productWorkGradePolicyGroupDto;
        private List<ProductStoneDto.Response> productStoneDtos;
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
    public static class Update {
        private Long factoryId;
        @NotBlank(message = "제조사 품명은 필수 입력값입니다.")
        private String productFactoryName;
        @NotBlank(message = "제품명은 필수 입력값입니다.")
        private String productName;
        private String setType; // setId
        private String classification; // classificationId
        private String material; // materialId
        private String standardWeight;
        private String productRelatedNumber;
        private String productNote;
        private List<ProductWorkGradePolicyGroupDto.Request> productWorkGradePolicyGroupDto; // 상품 판매 공임
        private List<ProductStoneDto.Request> productStoneDtos; //상품용 <- stone 호출

    }

    @Getter
    @NoArgsConstructor
    public static class Page {
        private String productId;
        private String productName;
        private String productFactoryName;
        private String factoryId;
        private String factoryName;
        private String productWeight;
        private String productMaterial;
        private String productColor;
        private String productNote;
        private String productPurchaseCost;
        private String productLaborCost;
        private Integer productGoldPrice;
        private ProductImageDto.Response image;
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

}
