package com.msa.jewelry.product.internal.stone.assistantStone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "보조석(AssistantStone) DTO 묶음")
public class AssistantStoneDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "보조석 응답 DTO")
    public static class Response {
        @Schema(description = "보조석 ID", example = "201")
        private Long assistantStoneId;
        @Schema(description = "보조석 이름", example = "큐빅 0.05ct")
        private String assistantStoneName;
        @Schema(description = "보조석 비고", example = "큐빅 지르코니아")
        private String assistantStoneNote;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "보조석 생성/수정 요청 DTO")
    public static class Request {
        @Schema(description = "보조석 이름", example = "큐빅 0.05ct")
        private String assistantStoneName;
        @Schema(description = "보조석 비고", example = "큐빅 지르코니아")
        private String assistantStoneNote;
    }

}
