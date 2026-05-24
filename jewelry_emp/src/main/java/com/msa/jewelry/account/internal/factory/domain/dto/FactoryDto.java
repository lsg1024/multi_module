package com.msa.jewelry.account.internal.factory.domain.dto;

import com.msa.jewelry.account.internal.global.domain.dto.AccountDto;
import com.msa.jewelry.account.internal.factory.domain.entity.Factory;
import com.msa.jewelry.account.internal.global.domain.dto.AddressDto;
import com.msa.jewelry.account.internal.global.domain.dto.CommonOptionDto;
import com.msa.jewelry.account.internal.global.domain.entity.GoldHarry;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.jewelry.account.internal.global.util.ExchangeEnumUtil.getLevelTypeTitle;
import static com.msa.jewelry.account.internal.global.util.ExchangeEnumUtil.getTradeTypeTitle;

@Schema(description = "제조사(공장) DTO 묶음 — 제조사 목록/등록/수정 요청·응답 형태")
public class FactoryDto {

    private static final String ERR_KO_EN_NUM_ONLY = "영어, 한글, 숫자만 허용됩니다.";
    private static final String ERR_NUM_ONLY = "숫자만 허용됩니다.";


    @Getter
    @NoArgsConstructor
    @Schema(description = "제조사 단건/목록 응답")
    public static class FactoryResponse {
        @Schema(description = "제조사 PK", example = "5")
        private Long factoryId;
        @Schema(description = "제조사명", example = "한빛제조사")
        private String factoryName;
        @Schema(description = "제조사 대표자명", example = "홍길동")
        private String factoryOwnerName;
        @Schema(description = "제조사 대표 전화번호", example = "01012345678")
        private String factoryPhoneNumber;
        @Schema(description = "제조사 연락처 1", example = "021234567")
        private String factoryContactNumber1;
        @Schema(description = "제조사 연락처 2", example = "029876543")
        private String factoryContactNumber2;
        @Schema(description = "제조사 팩스 번호", example = "021234568")
        private String factoryFaxNumber;
        @Schema(description = "제조사 비고", example = "주요 협력사")
        private String factoryNote;
        @Schema(description = "주소 (표시용 합본 문자열)", example = "서울 강남구 테헤란로 123")
        private String address;
        @Schema(description = "거래 유형 (BUY/SELL, 표시명 변환)", example = "매입")
        private String tradeType;
        @Schema(description = "거래처 등급 (A/B/C, 표시명 변환)", example = "A등급")
        private String grade;
        @Schema(description = "금 손모율 (문자열)", example = "1.5")
        private String goldHarryLoss;
        /**
         * 최근 거래일 — 해당 제조사(Factory)와의 가장 최신 SALE 트랜잭션 일자.
         * (findAllFactory 에서 JPQLQuery 서브쿼리로 계산. Task 4-2.)
         */
        @Schema(description = "최근 거래일 (가장 최신 SALE 트랜잭션 일자)", example = "2026-05-10")
        private String lastSaleDate;
        /**
         * 최근 결제일 — 해당 제조사(Factory)와의 가장 최신 PAYMENT 트랜잭션 일자.
         * (findAllFactory 에서 JPQLQuery 서브쿼리로 계산. Task 4-2.)
         */
        @Schema(description = "최근 결제일 (가장 최신 PAYMENT 트랜잭션 일자)", example = "2026-05-12")
        private String lastPaymentDate;

        @QueryProjection
        public FactoryResponse(Long factoryId, String factoryName, String factoryOwnerName, String factoryPhoneNumber, String factoryContactNumber1, String factoryContactNumber2, String factoryFaxNumber, String factoryNote, String address, String tradeType, String grade, String goldHarryLoss, String lastSaleDate, String lastPaymentDate) {
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
            this.grade = getLevelTypeTitle(grade);
            this.goldHarryLoss = goldHarryLoss;
            this.lastSaleDate = lastSaleDate;
            this.lastPaymentDate = lastPaymentDate;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "제조사 등록 요청 — 매입처 정보 + 거래 옵션 + 주소")
    public static class FactoryRequest {
        @Valid
        @NotNull(message = "매입처 필수 입력값을 입력해주세요.")
        @Schema(description = "매입처(제조사) 기본 정보")
        private AccountDto.AccountInfo accountInfo;

        @Valid
        @NotNull(message = "거래 유형, 거래 등급은 필수 선택 사항입니다.")
        @Schema(description = "거래 유형/등급/금시세 정책")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;

        @Valid
        @Schema(description = "주소 정보")
        private AddressDto.AddressInfo addressInfo;

        public Factory toEntity(GoldHarry goldHarry) {
            return Factory.builder()
                    .factoryName(accountInfo.getAccountName())
                    .factoryOwnerName(accountInfo.getAccountOwnerName())
                    .factoryPhoneNumber(accountInfo.getAccountPhoneNumber())
                    .factoryContactNumber1(accountInfo.getAccountContactNumber1())
                    .factoryContactNumber2(accountInfo.getAccountContactNumber2())
                    .factoryFaxNumber(accountInfo.getAccountFaxNumber())
                    .factoryNote(accountInfo.getAccountNote())
                    .address(addressInfo.toEntity())
                    .commonOption(commonOptionInfo.toEntity(goldHarry))
                    .build();
        }

    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "제조사 수정 요청 — 제조사 정보 + 거래 옵션 + 주소")
    public static class FactoryUpdate {
        @Valid
        @NotNull(message = "매입처 필수 입력값을 입력해주세요.")
        @Schema(description = "제조사 기본 정보")
        private FactoryInfo factoryInfo;
        @NotNull(message = "거래 유형, 거래 등급은 필수 선택 사항입니다.")
        @Schema(description = "거래 유형/등급/금시세 정책")
        private CommonOptionDto.CommonOptionInfo commonOptionInfo;
        @Valid
        @Schema(description = "주소 정보")
        private AddressDto.AddressInfo addressInfo;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "제조사 기본 정보 (수정 시 사용)")
    public static class FactoryInfo {
        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "제조사명", example = "한빛제조사")
        private String factoryName;

        @Pattern(regexp = "^[A-Za-z0-9가-힣\\s]+$", message = ERR_KO_EN_NUM_ONLY)
        @Schema(description = "제조사 대표자명", example = "홍길동")
        private String factoryOwnerName;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "제조사 대표 전화번호 (숫자만)", example = "01012345678")
        private String factoryPhoneNumber;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "제조사 연락처 1 (숫자만)", example = "021234567")
        private String factoryContactNumber1;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "제조사 연락처 2 (숫자만)", example = "029876543")
        private String factoryContactNumber2;

        @Pattern(regexp = "^[0-9]+$", message = ERR_NUM_ONLY)
        @Schema(description = "제조사 팩스 번호 (숫자만)", example = "021234568")
        private String factoryFaxNumber;

        @Schema(description = "제조사 비고", example = "주요 협력사")
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
    @Schema(description = "제조사 외부 API용 경량 정보 (id/name/harry)")
    public static class ApiFactoryInfo {
        @Schema(description = "제조사 PK", example = "5")
        private Long factoryId;
        @Schema(description = "제조사명", example = "한빛제조사")
        private String factoryName;
        @Schema(description = "제조사 수수료(허리) — 문자열로 보관된 손모율", example = "1.5")
        private String factoryHarry;

        @QueryProjection
        public ApiFactoryInfo(Long factoryId, String factoryName) {
            this.factoryId = factoryId;
            this.factoryName = factoryName;
        }
    }
}
