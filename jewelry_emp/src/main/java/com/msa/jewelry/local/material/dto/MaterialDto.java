package com.msa.jewelry.local.material.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "재질 요청 DTO — 14K/18K 등 재질 마스터 생성/수정")
public class MaterialDto {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    @Schema(description = "재질명", example = "18K")
    private String name;
    @Schema(description = "금 함량 퍼센트 (문자열)", example = "75.00")
    private String goldPurityPercent;

    @Getter
    @NoArgsConstructor
    @Schema(description = "재질 단건 응답 DTO")
    public static class ResponseSingle {
        @Schema(description = "재질 ID", example = "1")
        private String materialId;
        @Schema(description = "재질명", example = "18K")
        private String materialName;
        @Schema(description = "금 함량 퍼센트", example = "75.00")
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
