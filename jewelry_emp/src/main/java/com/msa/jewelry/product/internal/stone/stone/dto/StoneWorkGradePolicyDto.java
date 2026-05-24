package com.msa.jewelry.product.internal.stone.stone.dto;

import com.msa.jewelry.product.internal.grade.WorkGrade;
import com.msa.jewelry.product.internal.global.exception.EnumValue;
import com.msa.jewelry.product.internal.stone.stone.entity.StoneWorkGradePolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "스톤 등급별 공임 정책 DTO")
public class StoneWorkGradePolicyDto {
    @EnumValue(enumClass = WorkGrade.class, message = "잘못된 입력 양식입니다.")
    @Schema(description = "공임 등급 (GRADE_1 ~ GRADE_4)", example = "GRADE_1")
    private String grade;
    @Schema(description = "공임 금액 (원)", example = "30000")
    private Integer laborCost;

    @Getter
    @Builder
    @Schema(description = "스톤 공임 정책 응답 DTO")
    public static class Response {
        @Schema(description = "공임 정책 ID", example = "401")
        private String workGradePolicyId;
        @Schema(description = "공임 등급", example = "GRADE_1")
        private String grade;
        @Schema(description = "공임 금액 (원)", example = "30000")
        private Integer laborCost;

        public static Response fromEntity(StoneWorkGradePolicy policy) {
            return Response.builder()
                    .workGradePolicyId(policy.getStoneWorkGradePolicyId().toString())
                    .grade(policy.getGrade().name())
                    .laborCost(policy.getLaborCost())
                    .build();
        }
    }

}
