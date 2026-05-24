package com.msa.jewelry.order.internal.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "상태 이력 응답 DTO — 사용자 표시용으로 변환된 한 건의 상태 변화 레코드.")
public class StatusHistoryDto {
    @Schema(description = "비즈니스 단계 표시명", example = "대기")
    private String phase;
    @Schema(description = "변경 종류 표시명 (생성/수정/삭제)", example = "수정")
    private String kind;
    @Schema(description = "이력 상세 내용", example = "재고로 입고됨")
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    @Schema(description = "이력 발생 시각 (KST)", example = "2026-05-16 14:30:00")
    private LocalDateTime statusCreateAt;
    @Schema(description = "이력 발생자", example = "홍길동")
    private String statusCreateBy;

    public StatusHistoryDto(String phase, String kind, String content, LocalDateTime statusCreateAt, String statusCreateBy) {
        this.phase = phase;
        this.kind = kind;
        this.content = content;
        this.statusCreateAt = statusCreateAt;
        this.statusCreateBy = statusCreateBy;
    }
}
