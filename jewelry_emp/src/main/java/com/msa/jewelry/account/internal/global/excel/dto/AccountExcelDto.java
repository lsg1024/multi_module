package com.msa.jewelry.account.internal.global.excel.dto;


import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.jewelry.account.internal.global.util.ExchangeEnumUtil.*;

@Getter
@NoArgsConstructor
@Schema(description = "거래처 엑셀 다운로드용 DTO — 거래처/제조사 공통 평탄화 응답")
public class AccountExcelDto {
    @Schema(description = "계정 유형 (STORE/FACTORY)", example = "STORE")
    private String accountType;
    @Schema(description = "생성 일자 (포맷 변환)", example = "2026-05-16")
    private String createAt;
    @Schema(description = "생성자", example = "admin")
    private String createBy;
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
    @Schema(description = "거래처 비고", example = "VIP")
    private String accountNote;
    @Schema(description = "주소 합본", example = "서울 강남구 테헤란로 123")
    private String accountAddress;
    @Schema(description = "거래 유형 (표시명)", example = "매출")
    private String accountTradeType;
    @Schema(description = "거래처 등급 (표시명)", example = "A등급")
    private String accountGrade;
    @Schema(description = "금 손모율 (문자열)", example = "1.5")
    private String accountGoldHarry;

    @QueryProjection
    public AccountExcelDto(String accountType, String createAt, String createBy, String accountName, String accountOwnerName, String accountPhoneNumber, String accountContactNumber1, String accountContactNumber2, String accountFaxNumber, String accountNote, String accountAddress, String accountTradeType, String accountGrade, String accountGoldHarry) {
        this.accountType = accountType;
        this.createAt = getFormattedDate(createAt);
        this.createBy = createBy;
        this.accountName = accountName;
        this.accountOwnerName = accountOwnerName;
        this.accountPhoneNumber = accountPhoneNumber;
        this.accountContactNumber1 = accountContactNumber1;
        this.accountContactNumber2 = accountContactNumber2;
        this.accountFaxNumber = accountFaxNumber;
        this.accountNote = accountNote;
        this.accountAddress = accountAddress;
        this.accountTradeType = getTradeTypeTitle(accountTradeType);
        this.accountGrade = getLevelTypeTitle(accountGrade);
        this.accountGoldHarry = accountGoldHarry;
    }
}
