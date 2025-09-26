package com.msa.order.global.excel.util;

import com.msa.order.global.excel.dto.OrderExcelQueryDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
public class ExcelUtil {

    public static byte[] createOrderWorkSheet(List<OrderExcelQueryDto> excelQueryDtos, LocalDate today) throws IOException {
        Map<String, List<OrderExcelQueryDto>> factoryRows = new LinkedHashMap<>();

        for (OrderExcelQueryDto dto : excelQueryDtos) {
            factoryRows.computeIfAbsent(dto.getFactory().toUpperCase(), k -> new ArrayList<>()).add(dto);
        }

        Workbook newWorkbook = new XSSFWorkbook();
        List<String> outputHeaders = Arrays.asList("No", "제조번호", "재질", "색상", "메인/보조", "사이즈", "비고");

        for (String factory : factoryRows.keySet()) {
            Sheet factorySheet = newWorkbook.createSheet(factory);

            // --- 상단 헤더 생성 ---
            Row firstRow = factorySheet.createRow(0);
            // 제조사명
            Cell factoryCell = firstRow.createCell(1);
            factoryCell.setCellValue(factory);
            CellStyle factoryStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, 16, factoryStyle, factoryCell);
            // 매장명 -> 추후 할당 받아 사용
            Cell storeCell = firstRow.createCell(4);
            storeCell.setCellValue("칸"); // 필요시 파라미터로 받아서 처리
            CellStyle storeStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, 24, storeStyle, storeCell);
            // 날짜
            Cell dateCell = firstRow.createCell(6);
            dateCell.setCellValue(today.toString());
            CellStyle dateStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, 20, dateStyle, dateCell);

            // --- 데이터 헤더 생성 (No, 제조번호 ...) ---
            Row header = factorySheet.createRow(1);
            CellStyle headerStyle = newWorkbook.createCellStyle();
            headerSheetStyle(newWorkbook, headerStyle);
            setWorkSheetHeader(outputHeaders, factorySheet, header, headerStyle);

            // --- 데이터 본문 생성 ---
            // 두 종류의 데이터 스타일 생성 (기본 12pt, 메인/보조용 10pt)
            CellStyle defaultDataStyle = newWorkbook.createCellStyle();
            dataSheetStyle(newWorkbook, defaultDataStyle); // 기본 스타일 (12pt)

            CellStyle multiLineDataStyle = newWorkbook.createCellStyle();
            createMultiLineDataStyle(newWorkbook, multiLineDataStyle); // 메인/보조용 스타일 (10pt)

            setWorkSheetBodyFromDto(factoryRows.get(factory), factorySheet, defaultDataStyle, multiLineDataStyle, 2);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        newWorkbook.write(outputStream);
        newWorkbook.close();

        return outputStream.toByteArray();
    }

    private static void setWorkSheetBodyFromDto(List<OrderExcelQueryDto> dtoList, Sheet factorySheet, CellStyle defaultStyle, CellStyle multiLineStyle, int startRowIdx) {
        int index = 1;
        int multiLineColumnIndex = 4;

        for (OrderExcelQueryDto dto : dtoList) {
            Row excelRow = factorySheet.createRow(startRowIdx++);
            excelRow.setHeightInPoints(50);

            // DTO 필드 순서에 맞게 셀 생성
            createCell(excelRow, 0, index++, defaultStyle);
            createCell(excelRow, 1, dto.getProductFactoryName(), defaultStyle);
            createCell(excelRow, 2, dto.getMaterial(), defaultStyle);
            createCell(excelRow, 3, dto.getColor(), defaultStyle);

            // 메인스톤/보조스톤 조합 및 10pt 스타일 적용
            String combinedStoneNote = dto.getOrderMainStoneNote() + "\n" + dto.getOrderAssistanceStoneNote();
            createCell(excelRow, multiLineColumnIndex, combinedStoneNote, multiLineStyle);

            createCell(excelRow, 5, dto.getProductSize(), defaultStyle);
            createCell(excelRow, 6, dto.getOrderNote(), defaultStyle);
        }
    }

    private static void createCell(Row row, int cellIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(cellIndex);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        }
        cell.setCellStyle(style);
    }

    private static void setWorkSheetHeader(List<String> outputHeaders, Sheet factorySheet, Row header, CellStyle headerStyle) {
        // CHANGED: "메인/보조" 컬럼이 추가되었으므로 widths 배열 크기 및 값 조정
        int[] widths = {5 * 256, 15 * 256, 5 * 256, 5 * 256, 20 * 256, 8 * 256, 30 * 256};
        for (int i = 0; i < outputHeaders.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(outputHeaders.get(i));
            cell.setCellStyle(headerStyle);
            factorySheet.setColumnWidth(i, widths[i]);
        }
        header.setHeightInPoints(24);
    }

    private static void headerSheetStyle(Workbook newWorkbook, int x, CellStyle cellStyle, Cell cell) {
        Font font = newWorkbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) x);
        cellStyle.setWrapText(true);
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(cellStyle);
    }

    private static void headerSheetStyle(Workbook newWorkbook, CellStyle headerStyle) {
        Font headerFont = newWorkbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    // 기본 데이터 스타일 (12pt)
    private static void dataSheetStyle(Workbook newWorkbook, CellStyle dataStyle) {
        Font dataFont = newWorkbook.createFont();
        dataFont.setFontHeightInPoints((short) 10); // 10pt
        dataStyle.setFont(dataFont);
        dataStyle.setWrapText(true);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }

    // ADDED: "메인/보조" 열 전용 데이터 스타일 (10pt)
    private static void createMultiLineDataStyle(Workbook newWorkbook, CellStyle dataStyle) {
        Font dataFont = newWorkbook.createFont();
        dataFont.setFontHeightInPoints((short) 8); // 8pt로 폰트 크기 줄임
        dataStyle.setFont(dataFont);
        dataStyle.setWrapText(true);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        dataStyle.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }
}