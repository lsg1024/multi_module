package com.msa.jewelry.local.goldharry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Schema(description = "금시세 정책(해리/손모율) DTO 묶음")
public class GoldHarryDto {

    @Getter
    @AllArgsConstructor
    @Schema(description = "금시세 정책 응답")
    public static class Response {
        @Schema(description = "금시세 정책 PK", example = "1")
        private String goldHarryId;
        @Schema(description = "금 손모율 (문자열)", example = "1.05")
        private String goldHarry;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "금시세 정책 등록 요청")
    public static class Request {
        @NotBlank(message = "필수 입력값 입니다.")
        @DecimalMin(value = "0.0", message = "0.0 이상이어야 합니다.")
        @Schema(description = "금 손모율 (0.0 이상)", example = "1.05")
        private BigDecimal goldHarry;
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "금시세 정책 수정 요청")
    public static class Update {
        @Schema(description = "금 손모율 (문자열)", example = "1.10")
        private String goldHarry;
    }
}
