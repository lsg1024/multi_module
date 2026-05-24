package com.msa.jewelry.local.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "상품 공임 정책 그룹 DTO — 색상별로 묶이는 공임 정책 묶음")
public class ProductWorkGradePolicyGroupDto {

    @Schema(description = "공임 정책 그룹 ID (신규면 null)", example = "9001")
    private String productGroupId;
    @Schema(description = "해당 색상의 상품 매입가 (원)", example = "300000")
    private Integer productPurchasePrice;
    @NotBlank(message = "필수 입력 값입니다.")
    @Schema(description = "색상 ID", example = "1")
    private String colorId;
    @Schema(description = "기본 정책 그룹 여부 — TRUE 면 색상 미지정 시 기본", example = "true")
    private boolean defaultProductPolicy;
    @Valid
    @Schema(description = "그룹에 속한 등급별 공임 정책 목록")
    private List<ProductWorkGradePolicyDto> policyDtos;
    @Schema(description = "그룹 비고", example = "옐로골드 전용")
    private String note;

    @Getter
    @NoArgsConstructor
    @Schema(description = "공임 정책 그룹 응답 DTO")
    public static class Response {
        @Schema(description = "공임 정책 그룹 ID", example = "9001")
        private String productGroupId;
        @Schema(description = "해당 색상의 상품 매입가 (원)", example = "300000")
        private Integer productPurchasePrice;
        @Schema(description = "색상 ID", example = "1")
        private String colorId;
        @Schema(description = "색상명", example = "옐로골드")
        private String colorName;
        @Schema(description = "기본 정책 그룹 여부", example = "true")
        private boolean defaultProductPolicy;
        @Schema(description = "등급별 공임 정책 목록")
        private List<ProductWorkGradePolicyDto.Response> policyDtos;
        @Schema(description = "그룹 비고", example = "옐로골드 전용")
        private String note;

        @Builder
        @QueryProjection
        public Response(String productGroupId, Integer productPurchasePrice, String colorId, String colorName, boolean defaultProductPolicy, List<ProductWorkGradePolicyDto.Response> policyDtos, String note) {
            this.productGroupId = productGroupId;
            this.productPurchasePrice = productPurchasePrice != null ? productPurchasePrice : 0;
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
    @Schema(description = "공임 정책 그룹 수정 요청 DTO")
    public static class Request {
        @Schema(description = "공임 정책 그룹 ID (신규면 null)", example = "9001")
        private String productGroupId;
        @Schema(description = "해당 색상의 상품 매입가 (원)", example = "300000")
        private Integer productPurchasePrice;
        @Schema(description = "색상 ID", example = "1")
        private String colorId;
        @Schema(description = "등급별 공임 정책 요청 목록")
        private List<ProductWorkGradePolicyDto.Request> policyDtos;
        @Schema(description = "그룹 비고", example = "옐로골드 전용")
        private String note;
    }

}
