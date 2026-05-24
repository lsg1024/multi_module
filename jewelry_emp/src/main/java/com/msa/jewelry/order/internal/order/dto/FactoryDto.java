package com.msa.jewelry.order.internal.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "제조사(공장) DTO 컨테이너 — order 모듈이 account 모듈에 제조사 정보를 조회할 때 사용.")
public class FactoryDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "제조사 조회 요청 — factoryId 단건.")
    public static class Request {
        @Schema(description = "제조사 ID", example = "5")
        private Long factoryId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "제조사 응답 — 제조사 이름과 수수료(허리).")
    public static class Response {
        @Schema(description = "제조사 ID", example = "5")
        private Long factoryId;
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "제조사 수수료(허리, 문자열)", example = "1.20")
        private String factoryHarry;
    }
}
