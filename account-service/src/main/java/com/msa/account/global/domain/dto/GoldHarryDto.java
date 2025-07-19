package com.msa.account.global.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class GoldHarryDto {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Update {
        private String goldHarryLoss;
    }
}
