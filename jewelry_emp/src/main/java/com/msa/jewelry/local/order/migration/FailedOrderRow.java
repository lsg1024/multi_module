package com.msa.jewelry.local.order.migration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "주문 마이그레이션 실패 행 — 마이그레이션 처리 실패한 한 행과 실패 사유.")
public class FailedOrderRow {
    @Schema(description = "실패한 CSV 행 원본")
    private final OrderCsvRow row;
    @Schema(description = "실패 사유 메시지", example = "거래처 매핑 실패")
    private final String reason;
}
