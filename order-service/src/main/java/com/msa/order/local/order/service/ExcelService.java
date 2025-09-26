package com.msa.order.local.order.service;

import com.msa.order.global.excel.dto.OrderExcelQueryDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static com.msa.order.global.excel.util.ExcelUtil.createOrderWorkSheet;

@Service
public class ExcelService {

    public byte[] getFormatDtoToExcel(List<OrderExcelQueryDto> queryDtos) throws IOException {
        return createOrderWorkSheet(queryDtos, LocalDate.now());
    }

}
