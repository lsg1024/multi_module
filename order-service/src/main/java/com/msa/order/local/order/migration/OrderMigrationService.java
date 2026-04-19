package com.msa.order.local.order.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 마이그레이션 유틸리티 서비스.
 * 실패 목록 CSV 생성을 담당한다.
 */
@Slf4j
@Service
public class OrderMigrationService {

    private static final Charset CP949 = Charset.forName("CP949");

    /**
     * 실패 목록을 CSV 바이트 배열로 변환한다.
     */
    public byte[] generateFailureCsv(List<FailedOrderRow> failures) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter osw = new OutputStreamWriter(baos, CP949);
             BufferedWriter writer = new BufferedWriter(osw)) {

            writer.write("No,접수번호,매장,구분,단계,제조사,제조번호,접수일,출고일,거 래 처,모델번호,분류,수량,재질,색상,중심스톤,보조스톤,사이즈,비고,실패사유");
            writer.newLine();

            for (FailedOrderRow f : failures) {
                OrderCsvRow r = f.getRow();
                writer.write(String.join(",",
                        safe(r.getNo()), safe(r.getReceiptNumber()), safe(r.getShopName()),
                        safe(r.getCategory()), safe(r.getPhase()), safe(r.getManufacturer()),
                        safe(r.getManufacturingNo()), safe(r.getReceiptDate()), safe(r.getShippingDate()),
                        safe(r.getTradingPartner()), safe(r.getModelNumber()), safe(r.getClassification()),
                        safe(r.getQuantity()), safe(r.getMaterial()), safe(r.getColor()),
                        safe(r.getMainStone()), safe(r.getSubStone()), safe(r.getSize()),
                        safe(r.getNote()), csvEscape(f.getReason())
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
