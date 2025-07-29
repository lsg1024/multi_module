package com.msa.product.local.set.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SetTypeDto {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;
    private String note;

    @Getter
    @NoArgsConstructor
    public static class ResponseSingle {
        private String setTypeId;
        private String setTypeName;
        private String setTypeNote;

        @Builder
        @QueryProjection
        public ResponseSingle(String setTypeId, String setTypeName, String setTypeNote) {
            this.setTypeId = setTypeId;
            this.setTypeName = setTypeName;
            this.setTypeNote = setTypeNote;
        }
    }

}
