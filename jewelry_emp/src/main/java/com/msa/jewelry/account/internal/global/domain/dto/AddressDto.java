package com.msa.jewelry.account.internal.global.domain.dto;

import com.msa.jewelry.account.internal.global.domain.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "주소 DTO 묶음")
public class AddressDto {

    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    @Getter
    @NoArgsConstructor
    @Schema(description = "주소 정보 — 우편번호 + 기본 주소 + 상세 주소")
    public static class AddressInfo {
        @Pattern(regexp = "^$|^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "우편번호 (숫자만)", example = "06236")
        private String addressZipCode;
        @Pattern(regexp = "^$|^[A-Za-z0-9가-힣\\s,\\-]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 123")
        private String addressBasic;
        @Pattern(regexp = "^$|^[A-Za-z0-9가-힣\\s,\\-]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "상세 주소", example = "4층 401호")
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
