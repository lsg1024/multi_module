package com.msa.account.global.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class GoldHarryDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String goldHarryId;
        private String goldHarry;
    }

    @Getter
    @AllArgsConstructor
    public static class Update {
        private String goldHarryLoss;
    }
}
