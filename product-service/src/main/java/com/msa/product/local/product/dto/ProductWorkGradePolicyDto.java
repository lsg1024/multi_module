package com.msa.product.local.product.dto;

import com.msa.product.global.exception.EnumValue;
import com.msa.product.local.grade.WorkGrade;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
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
        private Long groupId;

        @Builder
        @QueryProjection
        public Response(String workGradePolicyId, String grade, Integer laborCost, String note, Long groupId) {
            this.workGradePolicyId = workGradePolicyId;
            this.grade = grade;
            this.laborCost = laborCost;
            this.note = note;
            this.groupId = groupId;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String workGradePolicyId;
        private String grade;
        private Integer laborCost;
        private String note;
        private Long groupId;
    }
}
