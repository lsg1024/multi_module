package com.msa.jewelry.order.internal.order.migration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "주문 마이그레이션 결과 — 성공 건수와 실패 행 목록을 묶은 응답.")
public class MigrationResult {
    @Schema(description = "성공 처리 건수", example = "1000")
    private final int successCount;
    @Schema(description = "실패 행 목록")
    private final List<FailedOrderRow> failures;

    public int getFailureCount() {
        return failures.size();
    }

    public int getTotalCount() {
        return successCount + failures.size();
    }
}
