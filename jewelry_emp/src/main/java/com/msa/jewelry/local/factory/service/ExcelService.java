package com.msa.jewelry.local.factory.service;

import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.msa.jewelry.global.excel.util.AccountExcelUtil.createAccountWorkSheet;

@Service("factoryExcelService")
public class ExcelService {

    public byte[] getFormatDtoToExcel(List<AccountExcelDto> excelDtos, String type) throws IOException {
        return createAccountWorkSheet(excelDtos, type);
    }
}
