package com.msa.account.global.domain.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.msa.account.global.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.account.global.util.ExchangeEnumUtil.getTradeTypeTitle;

public class AccountDto {
    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    @Getter
    @NoArgsConstructor
    public static class AccountResponse {
        private Long accountId;
        private String accountName;
        private String tradeType;
        private String grade;
        private String goldHarryLoss;
        private String goldWeight;
        private String moneyAmount;
        private String lastSaleDate;
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
        public AccountResponse(Long accountId, String accountName, String goldWeight, String moneyAmount, String lastSaleDate, String businessOwnerName, String businessOwnerNumber, String businessNumber1, String businessNumber2, String faxNumber, String note, String grade, String tradeType, String goldHarryLoss, String lastPaymentDate, String address) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.goldWeight = goldWeight;
            this.moneyAmount = moneyAmount;
            this.lastSaleDate = lastSaleDate;
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

        @QueryProjection
        public AccountResponse(Long accountId, String accountName, String goldWeight, String moneyAmount) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.goldWeight = goldWeight;
            this.moneyAmount = moneyAmount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class AccountSaleLogResponse {
        private Long accountId;
        private String accountName;
        private String tradeType;
        private String grade;
        private String goldHarryLoss;
        private String previousGoldBalance;
        private String previousMoneyBalance;
        private String afterGoldBalance;
        private String afterMoneyBalance;
        private String lastSaleDate;
        private String lastPaymentDate;
        private String businessOwnerName;
        private String businessOwnerNumber;
        private String businessNumber1;
        private String businessNumber2;
        private String faxNumber;
        private String address;
        private String note;

        @QueryProjection
        public AccountSaleLogResponse(Long accountId, String accountName, String tradeType, String grade, String goldHarryLoss, String lastSaleDate, String lastPaymentDate, String businessOwnerName, String businessOwnerNumber, String businessNumber1, String businessNumber2, String faxNumber, String address, String note) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.tradeType = tradeType;
            this.grade = grade;
            this.goldHarryLoss = goldHarryLoss;
            this.lastSaleDate = lastSaleDate;
            this.lastPaymentDate = lastPaymentDate;
            this.businessOwnerName = businessOwnerName;
            this.businessOwnerNumber = businessOwnerNumber;
            this.businessNumber1 = businessNumber1;
            this.businessNumber2 = businessNumber2;
            this.faxNumber = faxNumber;
            this.address = address;
            this.note = note;
        }

        public void updateBalance(BigDecimal previousGoldBalance, Long previousMoneyBalance, BigDecimal afterGoldBalance, Long afterMoneyBalance) {
            this.previousGoldBalance = previousGoldBalance.toPlainString();
            this.previousMoneyBalance = previousMoneyBalance.toString();
            this.afterGoldBalance = afterGoldBalance.toPlainString();
            this.afterMoneyBalance = afterMoneyBalance.toString();
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

    /**
     * Task 4-3 / 4-4 — 최근거래일/최근결제일 클릭 시 상세 팝업 응답.
     *
     * - recentTransactions : 최근 SALE 트랜잭션 내역(최대 N건, 기본 20). 거래일 desc.
     * - paymentSummary     : PAYMENT 트랜잭션 집계(총 순금 중량, 총 결제 금액, 건수, 최근 결제일).
     *
     * 한 API 로 두 탭(거래/결제)의 데이터를 동시에 내려줘 프론트에서 한 번의 요청으로
     * 팝업을 채울 수 있게 한다. 거래 상품의 상세(모델번호·사이즈 등)는 order-service 영역이라
     * 이 단계에서는 transaction_history 가 보유한 필드(material, goldAmount, moneyAmount,
     * accountSaleCode, transactionDate, transactionHistoryNote)만 노출한다.
     */
    @Getter
    @NoArgsConstructor
    public static class RecentActivityResponse {
        private java.util.List<TransactionItem> recentTransactions;
        private PaymentSummary paymentSummary;

        public RecentActivityResponse(java.util.List<TransactionItem> recentTransactions, PaymentSummary paymentSummary) {
            this.recentTransactions = recentTransactions;
            this.paymentSummary = paymentSummary;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class TransactionItem {
        private String transactionDate;
        private String transactionType; // SALE / PAYMENT / DISCOUNT / WG / 등
        private String material;
        private String goldAmount;
        private String moneyAmount;
        private String saleCode;
        private String note;

        @QueryProjection
        public TransactionItem(String transactionDate, String transactionType, String material, String goldAmount, String moneyAmount, String saleCode, String note) {
            this.transactionDate = transactionDate;
            this.transactionType = transactionType;
            this.material = material;
            this.goldAmount = goldAmount;
            this.moneyAmount = moneyAmount;
            this.saleCode = saleCode;
            this.note = note;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PaymentSummary {
        private String totalGoldWeight;   // PAYMENT 누적 순금 중량
        private String totalMoneyAmount;  // PAYMENT 누적 결제 금액 (공임 포함)
        private Long paymentCount;        // PAYMENT 건수
        private String lastPaymentDate;   // 최근 결제일

        @QueryProjection
        public PaymentSummary(String totalGoldWeight, String totalMoneyAmount, Long paymentCount, String lastPaymentDate) {
            this.totalGoldWeight = totalGoldWeight;
            this.totalMoneyAmount = totalMoneyAmount;
            this.paymentCount = paymentCount;
            this.lastPaymentDate = lastPaymentDate;
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

        private String accountNote;
    }

}
