package com.msa.account.local.factory.service;

import com.msa.account.global.excel.dto.AccountExcelDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.msa.account.global.excel.util.AccountExcelUtil.createAccountWorkSheet;

@Service
public class ExcelService {

    public byte[] getFormatDtoToExcel(List<AccountExcelDto> excelDtos, String type) throws IOException {
        return createAccountWorkSheet(excelDtos, type);
    }
}
