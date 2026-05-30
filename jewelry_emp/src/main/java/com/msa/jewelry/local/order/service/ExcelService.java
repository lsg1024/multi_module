package com.msa.jewelry.local.order.service;

import com.msa.jewelry.global.excel.dto.OrderExcelQueryDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static com.msa.jewelry.global.excel.util.ExcelUtil.createOrderWorkSheet;

@Component("orderExcelService")
public class ExcelService {

    public byte[] getFormatDtoToExcel(List<OrderExcelQueryDto> queryDtos) throws IOException {
        return createOrderWorkSheet(queryDtos, LocalDate.now());
    }

}
