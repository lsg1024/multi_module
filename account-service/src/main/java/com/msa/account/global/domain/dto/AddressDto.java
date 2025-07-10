package com.msa.account.global.domain.dto;

import com.msa.account.global.domain.entity.Address;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AddressDto {

    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    @Getter
    @NoArgsConstructor
    public static class AddressInfo {
        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String addressZipCode;
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String addressBasic;
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String addressAdd;

        @Builder
        public AddressInfo(String addressZipCode, String addressBasic, String addressAdd) {
            this.addressZipCode = addressZipCode;
            this.addressBasic = addressBasic;
            this.addressAdd = addressAdd;
        }

        public Address toEntity() {
            return Address.builder()
                    .addressZipCode(this.addressZipCode)
                    .addressBasic(this.addressBasic)
                    .addressAdd(this.addressAdd)
                    .build();
        }
    }

}
