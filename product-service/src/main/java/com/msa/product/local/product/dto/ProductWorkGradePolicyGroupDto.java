package com.msa.product.local.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductWorkGradePolicyGroupDto {

    private String productGroupId;
    private Integer productPurchasePrice;
    @NotBlank(message = "필수 입력 값입니다.")
    private String colorId;
    private boolean defaultProductPolicy;
    @Valid
    private List<ProductWorkGradePolicyDto> policyDtos;
    private String note;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String productGroupId;
        private Integer productPurchasePrice;
        private String colorId;
        private String colorName;
        private boolean defaultProductPolicy;
        private List<ProductWorkGradePolicyDto.Response> policyDtos;
        private String note;

        @Builder
        @QueryProjection
        public Response(String productGroupId, Integer productPurchasePrice, String colorId, String colorName, boolean defaultProductPolicy, List<ProductWorkGradePolicyDto.Response> policyDtos, String note) {
            this.productGroupId = productGroupId;
            this.productPurchasePrice = productPurchasePrice;
            this.colorId = colorId;
            this.colorName = colorName;
            this.defaultProductPolicy = defaultProductPolicy;
            this.policyDtos = policyDtos;
            this.note = note;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String productGroupId;
        private Integer productPurchasePrice;
        private String colorId;
        private List<ProductWorkGradePolicyDto.Request> policyDtos;
        private String note;
    }

}
