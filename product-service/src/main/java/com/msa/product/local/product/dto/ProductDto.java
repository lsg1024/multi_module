package com.msa.product.local.product.dto;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.set.dto.SetTypeDto;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class ProductDto {

    private Long factoryId;
    private String productFactoryName;
    private String productName;
    private String setType; // setId
    private String classification; // classificationId
    private String material; // materialId
    private String standardWeight;
    private String productNote;
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
        private String productNote;
        private SetTypeDto.ResponseSingle setTypeDto;
        private ClassificationDto.ResponseSingle classificationDto;
        private MaterialDto.ResponseSingle materialDto;
        private List<ProductWorkGradePolicyGroupDto.Response> productWorkGradePolicyGroupDto;
        private List<ProductStoneDto.Response> productStoneDtos; //상품용 <- stone 호출
        private List<ProductImageDto.Response> productImageDtos; //상품용 이미지

        @Builder
        @QueryProjection
        public Detail(String productId, Long factoryId, String factoryName, String productFactoryName, String productName, String standardWeight, String productNote, SetTypeDto.ResponseSingle setTypeDto, ClassificationDto.ResponseSingle classificationDto, MaterialDto.ResponseSingle materialDto, List<ProductWorkGradePolicyGroupDto.Response> productWorkGradePolicyGroupDto, List<ProductStoneDto.Response> productStoneDtos, List<ProductImageDto.Response> productImageDtos) {
            this.productId = productId;
            this.factoryId = factoryId;
            this.factoryName = factoryName;
            this.productFactoryName = productFactoryName;
            this.productName = productName;
            this.standardWeight = standardWeight;
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
        private String productFactoryName;
        private String productName;
        private String setType; // setId
        private String classification; // classificationId
        private String material; // materialId
        private String standardWeight;
        private String productNote;
        private List<ProductWorkGradePolicyGroupDto.Request> productWorkGradePolicyGroupDto; // 상품 판매 공임
        private List<ProductStoneDto.Request> productStoneDtos; //상품용 <- stone 호출
    }

    @Getter
    @NoArgsConstructor
    public static class Page {
        private String productId;
        private String productName;
        private String productWeight;
        private String productMaterial;
        private String productColor;
        private String productNote;
        private String productLaborCost;
        private String productPriceInfo; // productStandard + (stoneGrade_1Price * count) * stoneCount
        private String productImagePath;
        private List<ProductStoneDto.Response> productStones;

        @Builder
        @QueryProjection
        public Page(String productId, String productName, String productWeight, String productMaterial, String productColor, String productNote, String productLaborCost, String productPriceInfo, String productImagePath, List<ProductStoneDto.Response> productStones) {
            this.productId = productId;
            this.productName = productName;
            this.productWeight = productWeight;
            this.productMaterial = productMaterial;
            this.productColor = productColor;
            this.productNote = productNote;
            this.productLaborCost = productLaborCost;
            this.productPriceInfo = productPriceInfo;
            this.productImagePath = productImagePath;
            this.productStones = (productStones != null)
                    ? new ArrayList<>(productStones)
                    : new ArrayList<>();
        }
    }
}
