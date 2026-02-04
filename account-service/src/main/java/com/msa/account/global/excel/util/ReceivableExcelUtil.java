package com.msa.account.global.excel.util;

import com.msa.account.global.excel.dto.ReceivableExcelDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ReceivableExcelUtil {

    public static byte[] createReceivableWorkSheet(List<ReceivableExcelDto> dtos, String type) throws IOException {

        Workbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSX);
        Sheet sheet = workbook.createSheet(type + " 목록");

        List<String> headers = Arrays.asList(
                "No", "거래처ID", "거래처명", "등급", "미수금(중량)",
                "미수금(금액)", "최근판매일", "최근결제일", "비고"
        );

        // 타이틀 행 (Row 0)
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(type + " 목록");
        CellStyle titleStyle = workbook.createCellStyle();
        applyTitleStyle(workbook, titleStyle, titleCell);

        Cell dateCell = titleRow.createCell(5);
        dateCell.setCellValue(LocalDate.now().toString());
        CellStyle dateStyle = workbook.createCellStyle();
        applyDateStyle(workbook, dateStyle, dateCell);

        // 헤더 행 (Row 1)
        Row headerRow = sheet.createRow(1);
        CellStyle headerStyle = workbook.createCellStyle();
        applyHeaderStyle(workbook, headerStyle);
        setHeaders(headers, sheet, headerRow, headerStyle);

        // 데이터 행 (Row 2 ~)
        CellStyle dataStyle = workbook.createCellStyle();
        applyDataStyle(workbook, dataStyle);
        setDataRows(dtos, sheet, dataStyle, 2);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private static void setDataRows(List<ReceivableExcelDto> dtoList, Sheet sheet, CellStyle style, int startRowIdx) {
        int index = 1;
        for (ReceivableExcelDto dto : dtoList) {
            Row row = sheet.createRow(startRowIdx++);
            row.setHeightInPoints(25);

            createCell(row, 0, index++, style);
            createCell(row, 1, dto.getAccountId() != null ? dto.getAccountId().toString() : "", style);
            createCell(row, 2, dto.getAccountName(), style);
            createCell(row, 3, dto.getGrade(), style);
            createCell(row, 4, dto.getGoldWeight(), style);
            createCell(row, 5, dto.getMoneyAmount(), style);
            createCell(row, 6, dto.getLastSaleDate(), style);
            createCell(row, 7, dto.getLastPaymentDate(), style);
            createCell(row, 8, dto.getNote(), style);
        }
    }

    private static void createCell(Row row, int index, Object value, CellStyle style) {
        Cell cell = row.createCell(index);
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

    private static void setHeaders(List<String> headers, Sheet sheet, Row headerRow, CellStyle style) {
        int[] widths = {
                5 * 256, 12 * 256, 20 * 256, 10 * 256, 15 * 256,
                15 * 256, 15 * 256, 15 * 256, 30 * 256
        };
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, widths[i]);
        }
        headerRow.setHeightInPoints(24);
    }

    private static void applyTitleStyle(Workbook workbook, CellStyle style, Cell cell) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(style);
    }

    private static void applyDateStyle(Workbook workbook, CellStyle style, Cell cell) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(style);
    }

    private static void applyHeaderStyle(Workbook workbook, CellStyle style) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    private static void applyDataStyle(Workbook workbook, CellStyle style) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }
}
