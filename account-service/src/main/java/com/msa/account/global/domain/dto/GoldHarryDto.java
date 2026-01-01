package com.msa.account.global.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

public class GoldHarryDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String goldHarryId;
        private String goldHarry;
    }

    @Getter
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "필수 입력값 입니다.")
        @DecimalMin(value = "0.0", message = "0.0 이상이어야 합니다.")
        private BigDecimal goldHarry;
    }

    @Getter
    @AllArgsConstructor
    public static class Update {
        private String goldHarry;
    }
}
