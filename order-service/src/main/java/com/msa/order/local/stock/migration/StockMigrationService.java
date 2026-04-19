package com.msa.order.local.stock.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 재고 마이그레이션 유틸리티 서비스.
 * 실패 목록 CSV 생성을 담당한다.
 */
@Slf4j
@Service
public class StockMigrationService {

    private static final Charset CP949 = Charset.forName("CP949");

    /**
     * 실패 목록을 CSV 바이트 배열로 변환한다.
     * (34 컬럼 모두 + 실패사유)
     */
    public byte[] generateFailureCsv(List<FailedStockRow> failures) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter osw = new OutputStreamWriter(baos, CP949);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // 헤더 (원본 34 컬럼 + 실패사유)
            writer.write("No,매장,매대구분,원재고구분,현재고구분,등록일,변경일,시리얼,접수번호,모델,모델분류,재질,색상,중심스톤,보조스톤,사이즈,기타설명,단가상품,총중량,금중량,알중량,수량,메인알수/EA,보조알수/EA,공임고정,기본공임/EA,추가공임/EA,중심공임/EA,보조공임/EA,공임합계,매입처,매입해리,기본공임원가/EA,스톤원가/EA,원가합계,실패사유");
            writer.newLine();

            for (FailedStockRow f : failures) {
                StockCsvRow r = f.getRow();
                writer.write(String.join(",",
                        safe(r.getNo()),
                        safe(r.getStoreName()),
                        safe(r.getStoreGrade()),
                        safe(r.getSourceType()),
                        safe(r.getCurrentStockType()),
                        safe(r.getCreatedDate()),
                        safe(r.getChangedDate()),
                        safe(r.getSerialNumber()),
                        safe(r.getReceiptNumber()),
                        safe(r.getModelName()),
                        safe(r.getClassification()),
                        safe(r.getMaterial()),
                        safe(r.getColor()),
                        safe(r.getMainStone()),
                        safe(r.getSubStone()),
                        safe(r.getSize()),
                        safe(r.getStockNote()),
                        safe(r.getUnitProduct()),
                        safe(r.getTotalWeight()),
                        safe(r.getGoldWeight()),
                        safe(r.getStoneWeight()),
                        safe(r.getQuantity()),
                        safe(r.getMainStoneQuantity()),
                        safe(r.getSubStoneQuantity()),
                        safe(r.getLaborCostFixed()),
                        safe(r.getProductLaborCost()),
                        safe(r.getProductAddLaborCost()),
                        safe(r.getStoneMainLaborCost()),
                        safe(r.getStoneSubLaborCost()),
                        safe(r.getTotalLaborCost()),
                        safe(r.getFactoryName()),
                        safe(r.getFactoryHarry()),
                        safe(r.getProductPurchaseCost()),
                        safe(r.getTotalStonePurchaseCost()),
                        "", // 원가합계 (파싱 불필요)
                        csvEscape(f.getReason())
                ));
                writer.newLine();
            }
        }
        return baos.toByteArray();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
