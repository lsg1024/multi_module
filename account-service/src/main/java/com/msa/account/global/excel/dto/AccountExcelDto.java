package com.msa.account.global.excel.dto;


import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.msa.account.global.util.ExchangeEnumUtil.*;

@Getter
@NoArgsConstructor
public class AccountExcelDto {
    private String accountType;
    private String createAt;
    private String createBy;
    private String accountName;
    private String accountOwnerName;
    private String accountPhoneNumber;
    private String accountContactNumber1;
    private String accountContactNumber2;
    private String accountFaxNumber;
    private String accountNote;
    private String accountAddress;
    private String accountTradeType;
    private String accountGrade;
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
