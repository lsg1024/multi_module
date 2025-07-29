package com.msa.product.local.material.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDto {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;
    private String goldPurityPercent;

    @Getter
    @NoArgsConstructor
    public static class ResponseSingle {
        private String materialId;
        private String materialName;
        private String materialGoldPurityPercent;

        @Builder
        @QueryProjection
        public ResponseSingle(String materialId, String materialName, String materialGoldPurityPercent) {
            this.materialId = materialId;
            this.materialName = materialName;
            this.materialGoldPurityPercent = materialGoldPurityPercent;
        }
    }
}
