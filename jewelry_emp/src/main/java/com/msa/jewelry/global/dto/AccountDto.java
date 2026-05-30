package com.msa.jewelry.global.dto;

import com.msa.jewelry.local.address.dto.AddressDto;
import com.msa.jewelry.local.common_option.dto.AdditionalOptionDto;
import com.msa.jewelry.local.common_option.dto.CommonOptionDto;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.msa.jewelry.global.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.jewelry.global.util.ExchangeEnumUtil.getTradeTypeTitle;

@Schema(description = "거래처(매장/제조사) 공통 DTO 묶음 — 단건/목록/팝업 응답 + 등록/수정 요청")
public class AccountDto {
    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 목록 응답 (잔액/거래일/주소 합본 포함)")
    public static class AccountResponse {
        @Schema(description = "거래처 PK", example = "10")
        private Long accountId;
        @Schema(description = "거래처명", example = "강남금은방")
        private String accountName;
        @Schema(description = "거래 유형 (표시명)", example = "매출")
        private String tradeType;
        @Schema(description = "거래처 등급 (표시명)", example = "A등급")
        private String grade;
        @Schema(description = "금 손모율 (문자열)", example = "1.5")
        private String goldHarryLoss;
        @Schema(description = "현재 금 미수 잔액 (문자열)", example = "12.345")
        private String goldWeight;
        @Schema(description = "현재 현금 미수 잔액 (문자열)", example = "1500000")
        private String moneyAmount;
        @Schema(description = "최근 거래일", example = "2026-05-10")
        private String lastSaleDate;
        @Schema(description = "최근 결제일", example = "2026-05-12")
        private String lastPaymentDate;
        @Schema(description = "대표자명", example = "홍길동")
        private String businessOwnerName;
        @Schema(description = "대표 전화번호", example = "01012345678")
        private String businessOwnerNumber;
        @Schema(description = "연락처 1", example = "021234567")
        private String businessNumber1;
        @Schema(description = "연락처 2", example = "029876543")
        private String businessNumber2;
        @Schema(description = "팩스 번호", example = "021234568")
        private String faxNumber;
        @Schema(description = "주소 (표시용 합본 문자열)", example = "서울 강남구 테헤란로 123")
        private String address;
        @Schema(description = "비고", example = "VIP 거래처")
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
    @Schema(description = "거래처 판매 시점 잔액 변화 응답 — 영수증 출력용")
    public static class AccountSaleLogResponse {
        @Schema(description = "거래처 PK", example = "10")
        private Long accountId;
        @Schema(description = "거래처명", example = "강남금은방")
        private String accountName;
        @Schema(description = "거래 유형", example = "SELL")
        private String tradeType;
        @Schema(description = "거래처 등급", example = "A")
        private String grade;
        @Schema(description = "금 손모율 (문자열)", example = "1.5")
        private String goldHarryLoss;
        @Schema(description = "판매 직전 금 잔액 (문자열)", example = "12.345")
        private String previousGoldBalance;
        @Schema(description = "판매 직전 현금 잔액 (문자열)", example = "1500000")
        private String previousMoneyBalance;
        @Schema(description = "판매 직후 금 잔액 (문자열)", example = "15.678")
        private String afterGoldBalance;
        @Schema(description = "판매 직후 현금 잔액 (문자열)", example = "2000000")
        private String afterMoneyBalance;
        @Schema(description = "최근 거래일", example = "2026-05-10")
        private String lastSaleDate;
        @Schema(description = "최근 결제일", example = "2026-05-12")
        private String lastPaymentDate;
        @Schema(description = "대표자명", example = "홍길동")
        private String businessOwnerName;
        @Schema(description = "대표 전화번호", example = "01012345678")
        private String businessOwnerNumber;
        @Schema(description = "연락처 1", example = "021234567")
        private String businessNumber1;
        @Schema(description = "연락처 2", example = "029876543")
        private String businessNumber2;
        @Schema(description = "팩스 번호", example = "021234568")
        private String faxNumber;
        @Schema(description = "주소 합본", example = "서울 강남구 테헤란로 123")
        private String address;
        @Schema(description = "비고", example = "VIP 거래처")
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
    @Schema(description = "거래처 단건 상세 응답 — 거래처/주소/공통옵션/부가옵션 평면화")
    public static class AccountSingleResponse {
        @Schema(description = "거래처 PK", example = "10")
        private String accountId;
        @Schema(description = "거래처명", example = "강남금은방")
        private String accountName;
        @Schema(description = "거래처 대표자명", example = "홍길동")
        private String accountOwnerName;
        @Schema(description = "거래처 대표 전화번호", example = "01012345678")
        private String accountPhoneNumber;
        @Schema(description = "거래처 연락처 1", example = "021234567")
        private String accountContactNumber1;
        @Schema(description = "거래처 연락처 2", example = "029876543")
        private String accountContactNumber2;
        @Schema(description = "거래처 팩스 번호", example = "021234568")
        private String accountFaxNumber;
        @Schema(description = "거래처 비고", example = "VIP 거래처")
        private String accountNote;

        @Schema(description = "주소 PK", example = "100")
        private String addressId;
        @Schema(description = "우편번호", example = "06236")
        private String addressZipCode;
        @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 123")
        private String addressBasic;
        @Schema(description = "상세 주소", example = "4층 401호")
        private String addressAdd;

        @Schema(description = "공통 옵션 PK", example = "1")
        private String commonOptionId;
        @Schema(description = "거래 유형 (표시명)", example = "매출")
        private String tradeType;
        @Schema(description = "거래처 등급 (표시명)", example = "A등급")
        private String grade;
        @Schema(description = "금시세 정책 PK", example = "1")
        private String goldHarryId;
        @Schema(description = "금 손모율 (문자열)", example = "1.5")
        private String goldHarryLoss;

        @Schema(description = "부가 옵션 PK", example = "1")
        private String additionalOptionId;
        @Schema(description = "과거 판매분 적용 여부", example = "false")
        private Boolean additionalApplyPastSales;
        @Schema(description = "부가 옵션 재질 ID", example = "10")
        private String additionalMaterialId;
        @Schema(description = "부가 옵션 재질명", example = "18K")
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
    @Schema(description = "최근거래일/최근결제일 클릭 시 상세 팝업 응답 (거래/결제 탭 데이터 합본)")
    public static class RecentActivityResponse {
        @Schema(description = "최근 SALE 트랜잭션 목록 (최대 N건, 기본 20)")
        private java.util.List<TransactionItem> recentTransactions;
        @Schema(description = "PAYMENT 트랜잭션 집계")
        private PaymentSummary paymentSummary;

        public RecentActivityResponse(java.util.List<TransactionItem> recentTransactions, PaymentSummary paymentSummary) {
            this.recentTransactions = recentTransactions;
            this.paymentSummary = paymentSummary;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래 이력 단건 항목 — 거래일/유형/재질/금/금액/세션코드/비고")
    public static class TransactionItem {
        @Schema(description = "거래 일시 (문자열)", example = "2026-05-16 14:30:00")
        private String transactionDate;
        @Schema(description = "거래 유형 (SALE/PAYMENT/DISCOUNT/WG 등)", example = "SALE")
        private String transactionType; // SALE / PAYMENT / DISCOUNT / WG / 등
        @Schema(description = "재질", example = "18K")
        private String material;
        @Schema(description = "금 수량(돈)", example = "3.333")
        private String goldAmount;
        @Schema(description = "현금 금액(원)", example = "500000")
        private String moneyAmount;
        @Schema(description = "판매 세션 코드 (TSID)", example = "445823472384938240")
        private String saleCode;
        @Schema(description = "거래 비고", example = "신규 매입")
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
    @Schema(description = "결제 집계 요약 — PAYMENT 누적 금/금액/건수 + 최근 결제일")
    public static class PaymentSummary {
        @Schema(description = "PAYMENT 누적 순금 중량", example = "30.500")
        private String totalGoldWeight;   // PAYMENT 누적 순금 중량
        @Schema(description = "PAYMENT 누적 결제 금액 (공임 포함, 원)", example = "5000000")
        private String totalMoneyAmount;  // PAYMENT 누적 결제 금액 (공임 포함)
        @Schema(description = "PAYMENT 건수", example = "12")
        private Long paymentCount;        // PAYMENT 건수
        @Schema(description = "최근 결제일", example = "2026-05-12")
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
    @Schema(description = "거래처 수정 요청 — 상점 정보 + 거래 옵션 + 부가 옵션 + 주소")
    public static class AccountUpdate {
        @Valid
        @NotNull(message = "상점 정보는 필수입니다.")
        @Schema(description = "거래처 기본 정보")
        private AccountInfo accountInfo;
        @NotNull(message = "기본 옵션 정보는 필수입니다.")
        @Schema(description = "거래 유형/등급/금시세 정책")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        @Schema(description = "부가 옵션 정보")
        private AdditionalOptionDto.AdditionalOptionInfo additionalOptionInfo;
        @Valid
        @Schema(description = "주소 정보")
        private AddressDto.AddressInfo addressInfo;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 기본 정보 — Store/Factory 공통 입력 필드")
    public static class AccountInfo {

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "거래처명", example = "강남금은방")
        private String accountName;

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "거래처 대표자명", example = "홍길동")
        private String accountOwnerName;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 대표 전화번호 (숫자만)", example = "01012345678")
        private String accountPhoneNumber;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 연락처 1 (숫자만)", example = "021234567")
        private String accountContactNumber1;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 연락처 2 (숫자만)", example = "029876543")
        private String accountContactNumber2;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 팩스 번호 (숫자만)", example = "021234568")
        private String accountFaxNumber;

        @Schema(description = "거래처 비고", example = "VIP 거래처")
        private String accountNote;
    }

}
