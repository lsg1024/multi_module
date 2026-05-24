package com.msa.jewelry.product.internal.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "Product 모듈에서 사용하는 제조사(Factory) 관련 DTO 묶음")
public class FactoryDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "제조사 조회 요청 DTO")
    public static class Request {
        @Schema(description = "제조사 ID", example = "5")
        private Long factoryId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "제조사 응답 DTO — 수수료(harry) 포함")
    public static class Response {
        @Schema(description = "제조사 ID", example = "5")
        private Long factoryId;
        @Schema(description = "제조사명", example = "한국주얼리")
        private String factoryName;
        @Schema(description = "제조사 수수료(허리)", example = "1.20")
        private String factoryHarry;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "제조사 일괄 조회 응답 DTO — ID/이름만 포함")
    public static class ResponseBatch {
        @Schema(description = "제조사 ID", example = "5")
        private Long factoryId;
        @Schema(description = "제조사명", example = "한국주얼리")
        private String factoryName;
    }
}
