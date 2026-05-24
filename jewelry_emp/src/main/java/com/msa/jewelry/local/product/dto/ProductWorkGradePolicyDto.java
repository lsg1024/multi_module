package com.msa.jewelry.local.product.dto;

import com.msa.jewelry.global.exception.EnumValue;
import com.msa.jewelry.local.grade.entity.WorkGrade;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상품 등급별 공임 정책 DTO")
public class ProductWorkGradePolicyDto {
    @EnumValue(enumClass = WorkGrade.class, message = "잘못된 입력 양식입니다.")
    @Schema(description = "공임 등급 (GRADE_1 ~ GRADE_4)", example = "GRADE_1")
    private String grade;
    @Schema(description = "공임 금액 (원)", example = "50000")
    private Integer laborCost;

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 공임 정책 응답 DTO")
    public static class Response {
        @Schema(description = "공임 정책 ID", example = "8001")
        private String workGradePolicyId;
        @Schema(description = "공임 등급", example = "GRADE_1")
        private String grade;
        @Schema(description = "공임 금액 (원)", example = "50000")
        private Integer laborCost;
        @Schema(description = "소속 공임 정책 그룹 ID", example = "9001")
        private Long groupId;

        @Builder
        @QueryProjection
        public Response(String workGradePolicyId, String grade, Integer laborCost, Long groupId) {
            this.workGradePolicyId = workGradePolicyId;
            this.grade = grade;
            this.laborCost = laborCost != null ? laborCost : 0;
            this.groupId = groupId;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 공임 정책 수정 요청 DTO")
    public static class Request {
        @Schema(description = "공임 정책 ID (신규면 null)", example = "8001")
        private String workGradePolicyId;
        @Schema(description = "공임 등급", example = "GRADE_1")
        private String grade;
        @Schema(description = "공임 금액 (원)", example = "50000")
        private Integer laborCost;

    }
}
