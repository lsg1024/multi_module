package com.msa.account.local.store.domain.dto;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.dto.AdditionalOptionDto;
import com.msa.account.global.domain.dto.AddressDto;
import com.msa.account.global.domain.dto.CommonOptionDto;
import com.msa.account.global.domain.entity.Address;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.local.store.domain.entity.AdditionalOption;
import com.msa.account.local.store.domain.entity.Store;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.account.global.domain.dto.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.account.global.domain.dto.util.ExchangeEnumUtil.getTradeTypeTitle;

public class StoreDto {
    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    //목록 조회
    @Getter
    @NoArgsConstructor
    public static class StoreResponse {
        private Long accountId;
        private String accountName;
        private String businessOwnerName;
        private String businessOwnerNumber;
        private String businessNumber1;
        private String businessNumber2;
        private String faxNumber;
        private String note;
        private String address;
        private String tradeType;
        private String level;
        private String goldHarryLoss;

        @QueryProjection
        public StoreResponse(Long accountId, String accountName, String businessOwnerName, String businessOwnerNumber, String businessNumber1, String businessNumber2, String faxNumber, String note, String address, String tradeType, String level, String goldHarryLoss) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.businessOwnerName = businessOwnerName;
            this.businessOwnerNumber = businessOwnerNumber;
            this.businessNumber1 = businessNumber1;
            this.businessNumber2 = businessNumber2;
            this.faxNumber = faxNumber;
            this.note = note;
            this.address = address;
            this.tradeType = getTradeTypeTitle(tradeType);
            this.level = getLevelTypeTitle(level);
            this.goldHarryLoss = goldHarryLoss;
        }
    }


    //상점 생성
    @Getter
    @NoArgsConstructor
    public static class StoreRequest {

        @Valid
        @NotNull(message = "판매처 필수 입력값을 입력해주세요.")
        private AccountDto.AccountInfo accountInfo;

        @NotNull(message = "거래 유형, 거래 등급은 필수 선택 사항입니다.")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        private AdditionalOptionDto.AdditionalOptionInfo additionalOptionInfo;
        @Valid
        private AddressDto.AddressInfo addressInfo;

        public Store toEntity(GoldHarry goldHarry) {

            Address addressEntity = (this.addressInfo != null) ? this.addressInfo.toEntity() : null;
            AdditionalOption additionalOptionEntity = (this.additionalOptionInfo != null) ? this.additionalOptionInfo.toEntity() : null;

            return Store.builder()
                    .storeName(accountInfo.getAccountName())
                    .storeOwnerName(accountInfo.getAccountOwnerName())
                    .storePhoneNumber(accountInfo.getAccountPhoneNumber())
                    .storeContactNumber1(accountInfo.getAccountContactNumber1())
                    .storeContactNumber2(accountInfo.getAccountContactNumber2())
                    .storeFaxNumber(accountInfo.getAccountFaxNumber())
                    .storeNote(accountInfo.getAccountNote())
                    .address(addressEntity)
                    .commonOption(commonOptionInfo.toEntity(goldHarry))
                    .additionalOption(additionalOptionEntity)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StoreInfo {

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String storeName;

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String storeOwnerName;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storePhoneNumber;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storeContactNumber1;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storeContactNumber2;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String storeFaxNumber;

        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String storeNote;

        @Builder
        @QueryProjection
        public StoreInfo(String storeName, String storeOwnerName, String storePhoneNumber, String storeContactNumber1, String storeContactNumber2, String storeFaxNumber, String storeNote) {
            this.storeName = storeName;
            this.storeOwnerName = storeOwnerName;
            this.storePhoneNumber = storePhoneNumber;
            this.storeContactNumber1 = storeContactNumber1;
            this.storeContactNumber2 = storeContactNumber2;
            this.storeFaxNumber = storeFaxNumber;
            this.storeNote = storeNote;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StoreCondition {
        private String storeName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiStoreInfo {
        private Long storeId;
        private String storeName;
        private String grade;
        private String storeHarry;
    }

    @Getter
    @NoArgsConstructor
    public static class StoreAttemptResponse {
        private Long storeId;
        private String storeName;
        private String tradeType;
        private String level;
        private String goldHarryLoss;
        private String goldWeight;
        private String moneyAmount;

        @QueryProjection
        public StoreAttemptResponse(Long storeId, String storeName, String tradeType, String level, String goldHarryLoss, String goldWeight, String moneyAmount) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.tradeType = getTradeTypeTitle(tradeType);
            this.level = getLevelTypeTitle(level);;
            this.goldHarryLoss = goldHarryLoss;
            this.goldWeight = goldWeight;
            this.moneyAmount = moneyAmount;
        }
    }
}
