package com.msa.account.domain.factory.dto;

import com.msa.account.domain.factory.entity.Factory;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.entity.OptionTradeType;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.Pattern;
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
        public FactoryResponse(String factoryName, String factoryOwnerName, String factoryPhoneNumber, String factoryContactNumber1, String factoryContactNumber2, String factoryFaxNumber, String factoryNote, String address, String tradeType, String level, String goldHarryLoss) {
            this.factoryName = factoryName;
            this.factoryOwnerName = factoryOwnerName;
            this.factoryPhoneNumber = factoryPhoneNumber;
            this.factoryContactNumber1 = factoryContactNumber1;
            this.factoryContactNumber2 = factoryContactNumber2;
            this.factoryFaxNumber = factoryFaxNumber;
            this.factoryNote = factoryNote;
            this.address = address;
            this.tradeType = tradeType;
            this.level = level;
            this.goldHarryLoss = goldHarryLoss;
        }
        public void getTradeTypeTitle() {
            this.tradeType = OptionTradeType.getTitleByKey(tradeType);
        }

        public void getLevelTypeLevel() {
            this.level = OptionTradeType.getTitleByKey(level);
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
        private FactoryInfo factoryInfo;
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
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
        private FactoryInfo factoryInfo;
        private String commonOptionId;
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        private String addressId;
        private AddressDto.AddressInfo addressInfo;
        private String goldHarryId;
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
}
