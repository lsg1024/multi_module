package com.msa.product.local.product.dto;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.set.dto.SetTypeDto;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private List<ProductWorkGradePolicyDto> gradePolicyDtos; // 상품 판매 공임
    private List<ProductStoneDto> productStoneDtos; //상품용 <- stone 호출

    @Setter
    @Getter
    @NoArgsConstructor
    public static class Detail {
        private String productId;
        private Long factoryId;
        private String productFactoryName;
        private String productName;
        private String standardWeight;
        private String productNote;
        private SetTypeDto.ResponseSingle setTypeDto;
        private ClassificationDto.ResponseSingle classificationDto;
        private MaterialDto.ResponseSingle materialDto;
        private List<ProductWorkGradePolicyDto.Response> gradePolicyDtos;
        private List<ProductStoneDto.Response> productStoneDtos; //상품용 <- stone 호출
        private List<ProductImageDto.Response> productImageDtos; //상품용 이미지

        @Builder
        @QueryProjection
        public Detail(String productId, Long factoryId, String productFactoryName, String productName, String standardWeight, String productNote, SetTypeDto.ResponseSingle setTypeDto, ClassificationDto.ResponseSingle classificationDto, MaterialDto.ResponseSingle materialDto, List<ProductWorkGradePolicyDto.Response> gradePolicyDtos, List<ProductStoneDto.Response> productStoneDtos, List<ProductImageDto.Response> productImageDtos) {
            this.productId = productId;
            this.factoryId = factoryId;
            this.productFactoryName = productFactoryName;
            this.productName = productName;
            this.standardWeight = standardWeight;
            this.productNote = productNote;
            this.setTypeDto = setTypeDto;
            this.classificationDto = classificationDto;
            this.materialDto = materialDto;
            this.gradePolicyDtos = gradePolicyDtos;
            this.productStoneDtos = productStoneDtos;
            this.productImageDtos = productImageDtos;
        }
    }
}
