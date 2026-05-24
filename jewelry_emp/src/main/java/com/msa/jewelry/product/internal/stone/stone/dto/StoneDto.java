package com.msa.jewelry.product.internal.stone.stone.dto;

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
@AllArgsConstructor
@Schema(description = "스톤(보석) 요청 DTO — 스톤 마스터 생성/수정")
public class StoneDto {

    public static final String NO_MESSAGE = "필수 입력값 입니다.";

    @NotBlank(message = NO_MESSAGE)
    @Schema(description = "스톤명 (보통 type+shape+size 합성)", example = "다이아 라운드 0.3ct")
    private String stoneName;
    @Schema(description = "스톤 비고", example = "VS1 등급")
    private String stoneNote;
    @Schema(description = "스톤 무게(캐럿/그램, 문자열)", example = "0.30")
    private String stoneWeight;
    @Schema(description = "스톤 매입 단가 (원)", example = "150000")
    private Integer stonePurchasePrice;
    @Valid
    @Schema(description = "스톤 등급별 공임 정책 목록")
    private List<StoneWorkGradePolicyDto> stoneWorkGradePolicyDto;

    @Getter
    @NoArgsConstructor
    @Schema(description = "스톤 단건 응답 DTO")
    public static class ResponseSingle {
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "스톤 비고", example = "VS1 등급")
        private String stoneNote;
        @Schema(description = "스톤 무게", example = "0.30")
        private String stoneWeight;
        @Schema(description = "스톤 매입 단가 (원)", example = "150000")
        private Integer stonePurchasePrice;
        @Schema(description = "등급별 공임 정책 목록")
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto;

        @Builder
        @QueryProjection
        public ResponseSingle(String stoneId, String stoneName, String stoneNote, String stoneWeight, Integer stonePurchasePrice, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto) {
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneNote = stoneNote;
            this.stoneWeight = stoneWeight;
            this.stonePurchasePrice = stonePurchasePrice;
            this.stoneWorkGradePolicyDto = stoneWorkGradePolicyDto;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "스톤 페이지 목록용 DTO — 사용 중인 상품 수 포함")
    public static class PageDto {
        @Schema(description = "스톤 ID", example = "301")
        private String stoneId;
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "스톤 비고", example = "VS1 등급")
        private String stoneNote;
        @Schema(description = "스톤 무게", example = "0.30")
        private String stoneWeight;
        @Schema(description = "스톤 매입 단가 (원)", example = "150000")
        private Integer stonePurchasePrice;
        @Schema(description = "등급별 공임 정책 목록")
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto;
        @Schema(description = "해당 스톤을 사용 중인 상품 수", example = "12")
        private Integer productCount;
        @Schema(description = "해당 스톤을 사용 중인 상품 요약 목록")
        private List<ProductInfo> productInfos;

        @Builder
        @QueryProjection
        public PageDto(String stoneId, String stoneName, String stoneNote, String stoneWeight, Integer stonePurchasePrice, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto, Integer productCount, List<ProductInfo> productInfos) {
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneNote = stoneNote;
            this.stoneWeight = stoneWeight;
            this.stonePurchasePrice = stonePurchasePrice;
            this.stoneWorkGradePolicyDto = stoneWorkGradePolicyDto;
            this.productCount = productCount;
            this.productInfos = productInfos;
        }
    }

    @Getter
    @Builder
    @Schema(description = "스톤을 사용 중인 상품 요약 DTO")
    public static class ProductInfo {
        @Schema(description = "상품 ID", example = "1001")
        private Long productId;
        @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
        private String productName;
        @Schema(description = "대표 이미지 경로", example = "https://cdn.example.com/products/abc.jpg")
        private String imagePath;
    }
}
