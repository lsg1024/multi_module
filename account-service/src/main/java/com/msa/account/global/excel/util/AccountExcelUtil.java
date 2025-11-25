package com.msa.account.global.excel.util;

import com.msa.account.global.excel.dto.AccountExcelDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class AccountExcelUtil {

    public static byte[] createAccountWorkSheet(List<AccountExcelDto> accountQueryDtos, String type) throws IOException {

        Workbook newWorkbook = new XSSFWorkbook(XSSFWorkbookType.XLSX);

        Sheet sheet = newWorkbook.createSheet(type);

        // 헤더 목록 정의
        List<String> outputHeaders = Arrays.asList(
                "No", "유형", "생성일", "생성자", "거래처명",
                "이름", "번호", "담당 번호1", "담당 번호2", "팩스 번호",
                "비고", "주소", "거래유형", "등급", "해리"
        );

        // --- 상단 타이틀/메타정보 생성 (Row 0) ---
        Row firstRow = sheet.createRow(0);

        // 엑셀 생성 날짜 표시 (현재 날짜 기준)
        Cell dateCell = firstRow.createCell(4);
        dateCell.setCellValue(LocalDate.now().toString());
        CellStyle dateStyle = newWorkbook.createCellStyle();
        headerSheetStyle(newWorkbook, 14, dateStyle, dateCell);

        // --- 데이터 테이블 헤더 생성 (Row 1) ---
        Row headerRow = sheet.createRow(1);
        CellStyle headerStyle = newWorkbook.createCellStyle();
        headerSheetStyle(newWorkbook, headerStyle);
        setWorkSheetHeader(outputHeaders, sheet, headerRow, headerStyle);

        // --- 데이터 본문 생성 (Row 2 ~ ...) ---
        CellStyle defaultDataStyle = newWorkbook.createCellStyle();
        dataSheetStyle(newWorkbook, defaultDataStyle);

        // [수정됨] 전체 리스트를 한 번에 넘김
        setWorkSheetBodyFromDto(accountQueryDtos, sheet, defaultDataStyle, 2);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        newWorkbook.write(outputStream);
        newWorkbook.close();

        return outputStream.toByteArray();
    }

    private static void setWorkSheetBodyFromDto(List<AccountExcelDto> dtoList, Sheet sheet, CellStyle defaultStyle, int startRowIdx) {
        int index = 1;

        for (AccountExcelDto dto : dtoList) {
            Row excelRow = sheet.createRow(startRowIdx++);
            excelRow.setHeightInPoints(30); // 행 높이

            createCell(excelRow, 0, index++, defaultStyle);
            createCell(excelRow, 1, dto.getAccountType(), defaultStyle);
            createCell(excelRow, 2, dto.getCreateAt(), defaultStyle);
            createCell(excelRow, 3, dto.getCreateBy(), defaultStyle);
            createCell(excelRow, 4, dto.getAccountName(), defaultStyle);
            createCell(excelRow, 5, dto.getAccountOwnerName(), defaultStyle);
            createCell(excelRow, 6, dto.getAccountPhoneNumber(), defaultStyle);
            createCell(excelRow, 7, dto.getAccountContactNumber1(), defaultStyle);
            createCell(excelRow, 8, dto.getAccountContactNumber2(), defaultStyle);
            createCell(excelRow, 9, dto.getAccountFaxNumber(), defaultStyle);
            createCell(excelRow, 10, dto.getAccountNote(), defaultStyle);
            createCell(excelRow, 11, dto.getAccountAddress(), defaultStyle);
            createCell(excelRow, 12, dto.getAccountTradeType(), defaultStyle);
            createCell(excelRow, 13, dto.getAccountGrade(), defaultStyle);
            createCell(excelRow, 14, dto.getAccountGoldHarry(), defaultStyle);
        }
    }

    private static void createCell(Row row, int cellIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(cellIndex);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value != null) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private static void setWorkSheetHeader(List<String> outputHeaders, Sheet sheet, Row header, CellStyle headerStyle) {
        int[] widths = {
                5 * 256, 10 * 256, 15 * 256, 10 * 256, 20 * 256,
                10 * 256, 15 * 256, 15 * 256, 15 * 256, 15 * 256,
                25 * 256, 30 * 256, 10 * 256, 8 * 256, 10 * 256
        };

        for (int i = 0; i < outputHeaders.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(outputHeaders.get(i));
            cell.setCellStyle(headerStyle);
            if (i < widths.length) sheet.setColumnWidth(i, widths[i]);
            else sheet.setColumnWidth(i, 10 * 256);
        }
        header.setHeightInPoints(24);
    }

    // 스타일 관련 메서드들 (기존 유지)
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

    private static void dataSheetStyle(Workbook newWorkbook, CellStyle dataStyle) {
        Font dataFont = newWorkbook.createFont();
        dataFont.setFontHeightInPoints((short) 10);
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