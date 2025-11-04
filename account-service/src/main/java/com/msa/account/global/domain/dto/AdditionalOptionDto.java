package com.msa.account.global.domain.dto;

import com.msa.account.local.store.domain.entity.AdditionalOption;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdditionalOptionDto {

    @Getter
    @NoArgsConstructor
    public static class AdditionalOptionInfo {
        private boolean additionalApplyPastSales;
        private String additionalMaterialId;
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
