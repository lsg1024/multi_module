package com.msa.product.local.color.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ColorDto {
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;
    private String note;

    @Getter
    @NoArgsConstructor
    public static class ResponseSingle {
        private String colorId;
        private String colorName;
        private String colorNote;

        @Builder
        @QueryProjection
        public ResponseSingle(String colorId, String colorName, String colorNote) {
            this.colorId = colorId;
            this.colorName = colorName;
            this.colorNote = colorNote;
        }
    }
}
