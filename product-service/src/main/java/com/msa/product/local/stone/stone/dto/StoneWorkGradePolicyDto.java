package com.msa.product.local.stone.stone.dto;

import com.msa.product.local.grade.WorkGrade;
import com.msa.product.global.exception.EnumValue;
import com.msa.product.local.stone.stone.entity.StoneWorkGradePolicy;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoneWorkGradePolicyDto {
    @EnumValue(enumClass = WorkGrade.class, message = "잘못된 입력 양식입니다.")
    private String grade;
    private Integer laborCost;

    @Builder
    public static class Request {
        private String grade;
        private Integer laborCost;
    }

    @Getter
    @Builder
    public static class Response {
        private String workGradePolicyId;
        private String grade;
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
