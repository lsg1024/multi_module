package com.msa.jewelry.local.store.dto;

import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.local.common_option.dto.AdditionalOptionDto;
import com.msa.jewelry.local.address.dto.AddressDto;
import com.msa.jewelry.local.common_option.dto.CommonOptionDto;
import com.msa.jewelry.local.address.entity.Address;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.store.entity.AdditionalOption;
import com.msa.jewelry.local.store.entity.Store;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.jewelry.global.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.jewelry.global.util.ExchangeEnumUtil.getTradeTypeTitle;

@Schema(description = "거래처(매장) DTO 묶음 — 목록/생성/수정 요청·응답, API용 경량 응답 포함")
public class StoreDto {
    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";

    //목록 조회
    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 목록 응답 — 잔액 외 거래처 메타 + 최근 거래/결제일")
    public static class StoreResponse {
        @Schema(description = "거래처 PK", example = "10")
        private Long accountId;
        @Schema(description = "거래처명", example = "강남금은방")
        private String accountName;
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
        @Schema(description = "비고", example = "VIP 거래처")
        private String note;
        @Schema(description = "주소 합본", example = "서울 강남구 테헤란로 123")
        private String address;
        @Schema(description = "거래 유형 (표시명)", example = "매출")
        private String tradeType;
        @Schema(description = "거래처 등급 (표시명)", example = "A등급")
        private String grade;
        @Schema(description = "금 손모율 (문자열)", example = "1.5")
        private String goldHarryLoss;
        /**
         * 최근 거래일 — 해당 거래처(Store)의 가장 최신 SALE 트랜잭션 일자.
         * (findAllStore 에서 JPQLQuery 서브쿼리로 계산. Task 4-2.)
         */
        @Schema(description = "최근 거래일 (최신 SALE 트랜잭션 일자)", example = "2026-05-10")
        private String lastSaleDate;
        /**
         * 최근 결제일 — 해당 거래처(Store)의 가장 최신 PAYMENT 트랜잭션 일자.
         * (findAllStore 에서 JPQLQuery 서브쿼리로 계산. Task 4-2.)
         */
        @Schema(description = "최근 결제일 (최신 PAYMENT 트랜잭션 일자)", example = "2026-05-12")
        private String lastPaymentDate;

        @QueryProjection
        public StoreResponse(Long accountId, String accountName, String businessOwnerName, String businessOwnerNumber, String businessNumber1, String businessNumber2, String faxNumber, String note, String address, String tradeType, String grade, String goldHarryLoss, String lastSaleDate, String lastPaymentDate) {
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
            this.grade = getLevelTypeTitle(grade);
            this.goldHarryLoss = goldHarryLoss;
            this.lastSaleDate = lastSaleDate;
            this.lastPaymentDate = lastPaymentDate;
        }
    }

    //상점 생성
    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처(매장) 등록 요청 — 거래처 정보 + 거래 옵션 + 부가 옵션 + 주소")
    public static class StoreRequest {

        @Valid
        @NotNull(message = "판매처 필수 입력값을 입력해주세요.")
        @Schema(description = "거래처(판매처) 기본 정보")
        private AccountDto.AccountInfo accountInfo;

        @NotNull(message = "거래 유형, 거래 등급은 필수 선택 사항입니다.")
        @Schema(description = "거래 유형/등급/금시세 정책")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        @Schema(description = "부가 옵션 정보")
        private AdditionalOptionDto.AdditionalOptionInfo additionalOptionInfo;
        @Valid
        @Schema(description = "주소 정보")
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
    @Schema(description = "거래처(매장) 기본 정보 — 수정 시 사용")
    public static class StoreInfo {

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "거래처명", example = "강남금은방")
        private String storeName;

        @NotBlank(message = "필수 입력입니다.")
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "거래처 대표자명", example = "홍길동")
        private String storeOwnerName;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 대표 전화번호 (숫자만)", example = "01012345678")
        private String storePhoneNumber;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 연락처 1 (숫자만)", example = "021234567")
        private String storeContactNumber1;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 연락처 2 (숫자만)", example = "029876543")
        private String storeContactNumber2;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "거래처 팩스 번호 (숫자만)", example = "021234568")
        private String storeFaxNumber;

        @Schema(description = "거래처 비고", example = "VIP 거래처")
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
    @Schema(description = "거래처 검색 조건")
    public static class StoreCondition {
        @Schema(description = "거래처명 검색어", example = "강남")
        private String storeName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "거래처 외부 API용 경량 정보")
    public static class ApiStoreInfo {
        @Schema(description = "거래처 PK", example = "10")
        private Long storeId;
        @Schema(description = "거래처명", example = "강남금은방")
        private String storeName;
        @Schema(description = "거래처 등급", example = "A")
        private String grade;
        @Schema(description = "거래처 수수료(허리) — 문자열로 보관된 손모율", example = "1.5")
        private String storeHarry;
        @Schema(description = "과거 판매분 적용 여부", example = "false")
        private boolean optionApplyPastSales;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "거래처 전화번호 경량 정보 — SMS 발송용")
    public static class StorePhoneInfo {
        @Schema(description = "거래처 PK", example = "10")
        private Long storeId;
        @Schema(description = "거래처명", example = "강남금은방")
        private String storeName;
        @Schema(description = "거래처 전화번호", example = "01012345678")
        private String storePhoneNumber;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 시도(Attempt) 응답 — 판매 진행 시 거래처 후보 정보")
    public static class StoreAttemptResponse {
        @Schema(description = "거래처 PK", example = "10")
        private Long storeId;
        @Schema(description = "거래처명", example = "강남금은방")
        private String storeName;
        @Schema(description = "거래 유형 (표시명)", example = "매출")
        private String tradeType;
        @Schema(description = "거래처 등급 (표시명)", example = "A등급")
        private String level;
        @Schema(description = "금 손모율 (문자열)", example = "1.5")
        private String goldHarryLoss;
        @Schema(description = "현재 금 미수 잔액 (문자열)", example = "12.345")
        private String goldWeight;
        @Schema(description = "현재 현금 미수 잔액 (문자열)", example = "1500000")
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
