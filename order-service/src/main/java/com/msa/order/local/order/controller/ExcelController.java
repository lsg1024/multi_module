package com.msa.order.local.order.controller;

import com.msa.order.global.excel.dto.OrderExcelQueryDto;
import com.msa.order.local.order.service.ExcelService;
import com.msa.order.local.order.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
public class ExcelController {

    private final OrdersService ordersService;
    private final ExcelService excelService;

    public ExcelController(OrdersService ordersService, ExcelService excelService) {
        this.ordersService = ordersService;
        this.excelService = excelService;
    }

    // 주문 전체 조회

    // 주문장 조회
    @GetMapping("/orders/excel/order-book")
    public ResponseEntity<byte[]> getOrderExcel(
            @RequestParam(name = "start") String startAt,
            @RequestParam(name = "end") String endAt,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "store", required = false) String storeName,
            @RequestParam(name = "setType", required = false) String setTypeName,
            @RequestParam(name = "color", required = false) String colorName,
            @RequestParam(name = "order_status") String orderStatus) throws IOException {

        List<OrderExcelQueryDto> excelData = ordersService.getExcel(startAt, endAt, factoryName, storeName, setTypeName, colorName, orderStatus);

        byte[] formatDtoToExcel = excelService.getFormatDtoToExcel(excelData);

        HttpHeaders headers = new HttpHeaders();

        // 파일 이름 설정 (한글 인코딩)
        String fileName = "주문장_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return new ResponseEntity<>(formatDtoToExcel, headers, HttpStatus.OK);
    }

}
