package com.msa.jewelry.order.internal.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "스톤 DTO 컨테이너 — 주문/재고 요청에서 공통으로 사용하는 스톤 입력 구조.")
public class StoneDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스톤 정보 — 메인/보조 스톤 한 건의 입력 단위.")
    public static class StoneInfo {
        @NotBlank(message = "스톤 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 ID는 숫자여야 합니다.")
        @Schema(description = "스톤 ID (숫자 문자열)", example = "101")
        private String stoneId;
        @Schema(description = "스톤 이름 (스냅샷)", example = "다이아몬드")
        private String stoneName;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.250")
        private String stoneWeight;
        @Schema(description = "스톤 매입 비용", example = "100000")
        private Integer purchaseCost;
        @Schema(description = "스톤 판매(공임) 비용", example = "150000")
        private Integer laborCost; // 판매 비용
        @Schema(description = "스톤 추가 공임", example = "20000")
        private Integer addLaborCost; // 추가 비용
        @Schema(description = "스톤 개수", example = "1")
        private Integer quantity;
        @Schema(description = "메인 스톤 여부 (true=메인, false=보조)", example = "true")
        private boolean mainStone; // 판매비용 메인인지 보조인지 판단
        @Schema(description = "스톤 포함 거래 여부", example = "true")
        private boolean includeStone;
    }
}
