package com.msa.jewelry.account.internal.global.domain.dto;

import com.msa.jewelry.account.internal.store.domain.entity.AdditionalOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "거래처 부가 옵션 DTO 묶음")
public class AdditionalOptionDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 부가 옵션 정보 — 과거 판매분 적용 여부, 특수 재질 매핑")
    public static class AdditionalOptionInfo {
        @Schema(description = "과거 판매분 적용 여부", example = "false")
        private boolean additionalApplyPastSales;
        @Schema(description = "부가 옵션 재질 ID", example = "10")
        private String additionalMaterialId;
        @Schema(description = "부가 옵션 재질명", example = "18K")
        private String additionalMaterialName;

        @Builder
        public AdditionalOptionInfo(boolean additionalApplyPastSales, String additionalMaterialId, String additionalMaterialName) {
            this.additionalApplyPastSales = additionalApplyPastSales;
            this.additionalMaterialId = additionalMaterialId;
            this.additionalMaterialName = additionalMaterialName;
        }

        public AdditionalOption toEntity() {
            return AdditionalOption.builder()
                    .optionApplyPastSales(this.additionalApplyPastSales)
                    .optionMaterialId(this.additionalMaterialId)
                    .optionMaterialName(this.additionalMaterialName)
                    .build();
        }
    }

}
