package com.msa.jewelry.order.internal.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "출고/배송 예정일 변경 요청 DTO.")
public class DateDto {
    @Schema(description = "변경할 출고 예정 일시", example = "2026-05-20T10:00:00")
    private LocalDateTime deliveryDate;
}
