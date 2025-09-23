package com.msa.order.global.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StoneDto {

    @Getter
    @NoArgsConstructor
    public static class StoneInfo {
        @NotBlank(message = "스톤 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 ID는 숫자여야 합니다.")
        private String stoneId;
        private String stoneName;
        private String stoneWeight;
        private Integer purchaseCost;
        private Integer laborCost; // 판매 비용
        private Integer addLaborCost; // 추가 비용
        private Integer quantity;
        private boolean mainStone; // 판매비용 메인인지 보조인지 판단
        private boolean includeStone;

        @Override
        public String toString() {
            return "StoneInfo{" +
                    "stoneId='" + stoneId + '\'' +
                    ", stoneName='" + stoneName + '\'' +
                    ", stoneWeight='" + stoneWeight + '\'' +
                    ", purchaseCost=" + purchaseCost +
                    ", laborCost=" + laborCost +
                    ", addLaborCost=" + addLaborCost +
                    ", quantity=" + quantity +
                    ", mainStone=" + mainStone +
                    ", includeStone=" + includeStone +
                    '}';
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoneResponse {
        private String stoneId;
        private String stoneName;
        private String stoneWeight;
        private Integer purchaseCost;
        private Integer laborCost; // 판매 비용
        private Integer addLaborCost; // 추가 비용
        private Integer quantity;
        private boolean mainStone; // 판매비용 메인인지 보조인지 판단
        private boolean includeStone;
    }
}
