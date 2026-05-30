package com.msa.jewelry.local.assistant_stone.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보조석 view — 다른 모듈에서 보조석 정보를 조회할 때 반환")
public record AssistantStoneView(
        @Schema(description = "보조석 ID", example = "201") Long assistantStoneId,
        @Schema(description = "보조석 이름", example = "큐빅 0.05ct") String assistantStoneName,
        @Schema(description = "보조석 비고", example = "큐빅 지르코니아") String assistantStoneNote
) {
}
