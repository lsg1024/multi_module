package com.msa.account.local.factory.domain.dto;

import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.account.global.domain.dto.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.account.global.domain.dto.util.ExchangeEnumUtil.getTradeTypeTitle;

public class FactoryDto {

    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";


    @Getter
    @NoArgsConstructor
    public static class FactoryResponse {
        private Long factoryId;
        private String factoryName;
        private String factoryOwnerName;
        private String factoryPhoneNumber;
        private String factoryContactNumber1;
        private String factoryContactNumber2;
        private String factoryFaxNumber;
        private String factoryNote;
        private String address;
        private String tradeType;
        private String level;
        private String goldHarryLoss;

        @QueryProjection
        public FactoryResponse(Long factoryId, String factoryName, String factoryOwnerName, String factoryPhoneNumber, String factoryContactNumber1, String factoryContactNumber2, String factoryFaxNumber, String factoryNote, String address, String tradeType, String level, String goldHarryLoss) {
            this.factoryId = factoryId;
            this.factoryName = factoryName;
            this.factoryOwnerName = factoryOwnerName;
            this.factoryPhoneNumber = factoryPhoneNumber;
            this.factoryContactNumber1 = factoryContactNumber1;
            this.factoryContactNumber2 = factoryContactNumber2;
            this.factoryFaxNumber = factoryFaxNumber;
            this.factoryNote = factoryNote;
            this.address = address;
            this.tradeType = getTradeTypeTitle(tradeType);
            this.level = getLevelTypeTitle(level);
            this.goldHarryLoss = goldHarryLoss;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class FactorySingleResponse {
        private String factoryId;
        private String factoryName;
        private String factoryOwnerName;
        private String factoryPhoneNumber;
        private String factoryContactNumber1;
        private String factoryContactNumber2;
        private String factoryFaxNumber;
        private String factoryNote;
        private String addressId;
        private String addressZipCode;
        private String addressBasic;
        private String addressAdd;
        private String commonOptionId;
        private String tradeType;
        private String level;
        private String goldHarryId;
        private String goldHarryLoss;

        @QueryProjection
        public FactorySingleResponse(String factoryId, String factoryName, String factoryOwnerName, String factoryPhoneNumber, String factoryContactNumber1, String factoryContactNumber2, String factoryFaxNumber, String factoryNote, String addressId, String addressZipCode, String addressBasic, String addressAdd, String commonOptionId, String tradeType, String level, String goldHarryId, String goldHarryLoss) {
            this.factoryId = factoryId;
            this.factoryName = factoryName;
            this.factoryOwnerName = factoryOwnerName;
            this.factoryPhoneNumber = factoryPhoneNumber;
            this.factoryContactNumber1 = factoryContactNumber1;
            this.factoryContactNumber2 = factoryContactNumber2;
            this.factoryFaxNumber = factoryFaxNumber;
            this.factoryNote = factoryNote;
            this.addressId = addressId;
            this.addressZipCode = addressZipCode;
            this.addressBasic = addressBasic;
            this.addressAdd = addressAdd;
            this.commonOptionId = commonOptionId;
            this.tradeType = getTradeTypeTitle(tradeType);
            this.level = getLevelTypeTitle(level);;
            this.goldHarryId = goldHarryId;
            this.goldHarryLoss = goldHarryLoss;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class FactoryRequest {
        @Valid
        @NotNull(message = "공장 정보는 필수입니다.")
        private FactoryInfo factoryInfo;

        @Valid
        @NotNull(message = "기본 옵션 정보는 필수입니다.")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;

        @Valid
        private AddressDto.AddressInfo addressInfo;

        public Factory toEntity(GoldHarry goldHarry) {
            return Factory.builder()
                    .factoryName(factoryInfo.factoryName)
                    .factoryOwnerName(factoryInfo.factoryOwnerName)
                    .factoryPhoneNumber(factoryInfo.factoryPhoneNumber)
                    .factoryContactNumber1(factoryInfo.factoryContactNumber1)
                    .factoryContactNumber2(factoryInfo.factoryContactNumber2)
                    .factoryFaxNumber(factoryInfo.factoryFaxNumber)
                    .factoryNote(factoryInfo.factoryNote)
                    .address(addressInfo.toEntity())
                    .commonOption(commonOptionInfo.toEntity(goldHarry))
                    .build();
        }

    }

    @Getter
    @NoArgsConstructor
    public static class FactoryUpdate {
        @Valid
        @NotNull(message = "공장 정보는 필수입니다.")
        private FactoryInfo factoryInfo;
        @NotNull(message = "기본 옵션 정보는 필수입니다.")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        @Valid
        private AddressDto.AddressInfo addressInfo;
    }

    @Getter
    @NoArgsConstructor
    public static class FactoryInfo {
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String factoryName;

        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String factoryOwnerName;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String factoryPhoneNumber;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String factoryContactNumber1;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String factoryContactNumber2;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String factoryFaxNumber;

        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String factoryNote;

        @Builder
        @QueryProjection
        public FactoryInfo(String factoryName, String factoryOwnerName, String factoryPhoneNumber, String factoryContactNumber1, String factoryContactNumber2, String factoryFaxNumber, String factoryNote) {
            this.factoryName = factoryName;
            this.factoryOwnerName = factoryOwnerName;
            this.factoryPhoneNumber = factoryPhoneNumber;
            this.factoryContactNumber1 = factoryContactNumber1;
            this.factoryContactNumber2 = factoryContactNumber2;
            this.factoryFaxNumber = factoryFaxNumber;
            this.factoryNote = factoryNote;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiFactoryInfo {
        private Long factoryId;
        private String factoryName;
        private String factoryHarry;
    }
}
