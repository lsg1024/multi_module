package com.msa.product.local.product.dto;

import com.msa.product.local.grade.WorkGrade;
import com.msa.product.global.exception.EnumValue;
import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductWorkGradePolicyDto {
    @EnumValue(enumClass = WorkGrade.class, message = "잘못된 입력 양식입니다.")
    private String grade;
    private Integer laborCost;
    private String note;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String workGradePolicyId;
        private String grade;
        private Integer laborCost;
        private String note;

        @Builder
        @QueryProjection
        public Response(String workGradePolicyId, String grade, Integer laborCost, String note) {
            this.workGradePolicyId = workGradePolicyId;
            this.grade = grade;
            this.laborCost = laborCost;
            this.note = note;
        }

        public static ProductWorkGradePolicyDto.Response fromEntity(ProductWorkGradePolicy policy) {
            return Response.builder()
                    .workGradePolicyId(policy.getProductWorkGradePolicyId().toString())
                    .grade(policy.getGrade().name())
                    .laborCost(policy.getLaborCost())
                    .note(policy.getProductPolicyNote())
                    .build();
        }
    }
}
