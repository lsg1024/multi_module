package com.msa.order.local.order.external_client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
public class ProductDetailDto {
    private Long productId;
    private String productName;
    private String materialName;
    private String colorName;
    private Integer purchaseCost;
    private Integer laborCost;
    private List<StoneInfo> stoneInfos;

    @Getter
    @NoArgsConstructor
    public static class StoneInfo {
        @NotBlank(message = "스톤 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 ID는 숫자여야 합니다.")
        private String stoneId;
        private String stoneName;
        private String stoneWeight;
        private Integer purchaseCost;
        private Integer laborCost;
        private Integer quantity;
        private boolean isMainStone;
        private boolean isIncludeStone;
    }
}
