package com.msa.account.global.domain.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.account.global.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.account.global.util.ExchangeEnumUtil.getTradeTypeTitle;

public class AccountDto {
    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    @Getter
    @NoArgsConstructor
    public static class accountResponse {
        private Long accountId;
        private String accountName;
        private String tradeType;
        private String grade;
        private String goldHarryLoss;
        private String goldWeight;
        private String moneyAmount;
        private String lastPaymentDate;
        private String businessOwnerName;
        private String businessOwnerNumber;
        private String businessNumber1;
        private String businessNumber2;
        private String faxNumber;
        private String address;
        private String note;

        @Builder
        @QueryProjection
        public accountResponse(Long accountId, String accountName, String goldWeight, String moneyAmount, String businessOwnerName, String businessOwnerNumber, String businessNumber1, String businessNumber2, String faxNumber, String note, String grade, String tradeType, String goldHarryLoss, String lastPaymentDate, String address) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.goldWeight = goldWeight;
            this.moneyAmount = moneyAmount;
            this.businessOwnerName = businessOwnerName;
            this.businessOwnerNumber = businessOwnerNumber;
            this.businessNumber1 = businessNumber1;
            this.businessNumber2 = businessNumber2;
            this.faxNumber = faxNumber;
            this.note = note;
            this.grade = getLevelTypeTitle(grade);
            this.tradeType = getTradeTypeTitle(tradeType);
            this.goldHarryLoss = goldHarryLoss;
            this.lastPaymentDate = lastPaymentDate;
            this.address = address;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class AccountSingleResponse {
        private String accountId;
        private String accountName;
        private String accountOwnerName;
        private String accountPhoneNumber;
        private String accountContactNumber1;
        private String accountContactNumber2;
        private String accountFaxNumber;
        private String accountNote;

        private String addressId;
        private String addressZipCode;
        private String addressBasic;
        private String addressAdd;

        private String commonOptionId;
        private String tradeType;
        private String grade;
        private String goldHarryId;
        private String goldHarryLoss;

        private String additionalOptionId;
        private Boolean additionalApplyPastSales;
        private String additionalMaterialId;
        private String additionalMaterialName;

        @QueryProjection
        public AccountSingleResponse(String accountId, String accountName, String accountOwnerName, String accountPhoneNumber, String accountContactNumber1, String accountContactNumber2, String accountFaxNumber, String accountNote, String addressId, String addressZipCode, String addressBasic, String addressAdd, String commonOptionId, String tradeType, String grade, String goldHarryId, String goldHarryLoss) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.accountOwnerName = accountOwnerName;
            this.accountPhoneNumber = accountPhoneNumber;
            this.accountContactNumber1 = accountContactNumber1;
            this.accountContactNumber2 = accountContactNumber2;
            this.accountFaxNumber = accountFaxNumber;
            this.accountNote = accountNote;
            this.addressId = addressId;
            this.addressZipCode = addressZipCode;
            this.addressBasic = addressBasic;
            this.addressAdd = addressAdd;
            this.commonOptionId = commonOptionId;
            this.tradeType = getTradeTypeTitle(tradeType);
            this.grade = getLevelTypeTitle(grade);
            this.goldHarryId = goldHarryId;
            this.goldHarryLoss = goldHarryLoss;
        }

        @QueryProjection
        public AccountSingleResponse(String accountId, String accountName, String accountOwnerName, String accountPhoneNumber, String accountContactNumber1, String accountContactNumber2, String accountFaxNumber, String accountNote, String addressId, String addressZipCode, String addressBasic, String addressAdd, String commonOptionId, String tradeType, String grade, String goldHarryId, String goldHarryLoss, String additionalOptionId, Boolean additionalApplyPastSales, String additionalMaterialId, String additionalMaterialName) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.accountOwnerName = accountOwnerName;
            this.accountPhoneNumber = accountPhoneNumber;
            this.accountContactNumber1 = accountContactNumber1;
            this.accountContactNumber2 = accountContactNumber2;
            this.accountFaxNumber = accountFaxNumber;
            this.accountNote = accountNote;
            this.addressId = addressId;
            this.addressZipCode = addressZipCode;
            this.addressBasic = addressBasic;
            this.addressAdd = addressAdd;
            this.commonOptionId = commonOptionId;
            this.tradeType = getTradeTypeTitle(tradeType);
            this.grade = getLevelTypeTitle(grade);
            this.goldHarryId = goldHarryId;
            this.goldHarryLoss = goldHarryLoss;
            this.additionalOptionId = additionalOptionId;
            this.additionalApplyPastSales = additionalApplyPastSales;
            this.additionalMaterialId = additionalMaterialId;
            this.additionalMaterialName = additionalMaterialName;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class AccountUpdate {
        @Valid
        @NotNull(message = "상점 정보는 필수입니다.")
        private AccountInfo accountInfo;
        @NotNull(message = "기본 옵션 정보는 필수입니다.")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        private AdditionalOptionDto.AdditionalOptionInfo additionalOptionInfo;
        @Valid
        private AddressDto.AddressInfo addressInfo;
    }

    @Getter
    @NoArgsConstructor
    public static class AccountInfo {

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String accountName;

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String accountOwnerName;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String accountPhoneNumber;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String accountContactNumber1;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String accountContactNumber2;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        private String accountFaxNumber;

        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        private String accountNote;
    }

}
