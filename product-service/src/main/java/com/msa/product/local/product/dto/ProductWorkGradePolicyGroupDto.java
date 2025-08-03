package com.msa.product.local.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductWorkGradePolicyGroupDto {

    private String productGroupId;
    private String colorId;
    @Valid
    private List<ProductWorkGradePolicyDto> policyDtos;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String productGroupId;
        private String colorName;
        private List<ProductWorkGradePolicyDto.Response> gradePolicyDtos;

        @Builder
        @QueryProjection
        public Response(String productGroupId, String colorName, List<ProductWorkGradePolicyDto.Response> gradePolicyDtos) {
            this.productGroupId = productGroupId;
            this.colorName = colorName;
            this.gradePolicyDtos = gradePolicyDtos;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String productGroupId;
        private String colorId;
        private String colorName;
        private List<ProductWorkGradePolicyDto.Request> gradePolicyDtos;
    }

}
