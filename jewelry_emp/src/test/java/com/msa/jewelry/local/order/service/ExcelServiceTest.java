package com.msa.jewelry.local.order.service;

import com.msa.jewelry.global.excel.dto.OrderExcelQueryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("order/ExcelService 단위 테스트")
class ExcelServiceTest {

    private final ExcelService excelService = new ExcelService();

    @Nested
    @DisplayName("getFormatDtoToExcel")
    class GetFormatDtoToExcel {

        @Test
        @DisplayName("빈 리스트 — 헤더만 있는 워크북 byte[] 반환")
        void 빈리스트_헤더만() throws Exception {
            byte[] result = excelService.getFormatDtoToExcel(Collections.emptyList());

            assertThat(result).isNotNull();
            assertThat(result.length).isPositive();
        }

        @Test
        @DisplayName("연속 호출 시 매번 새 byte[] 반환 (stateless 검증)")
        void 연속호출_stateless() throws Exception {
            byte[] first = excelService.getFormatDtoToExcel(Collections.emptyList());
            byte[] second = excelService.getFormatDtoToExcel(Collections.emptyList());

            assertThat(first).isNotSameAs(second);
            assertThat(first.length).isPositive();
            assertThat(second.length).isPositive();
        }

        @Test
        @DisplayName("XLSX 매직넘버 검증 — 'PK' (ZIP)")
        void XLSX_매직넘버() throws Exception {
            byte[] bytes = excelService.getFormatDtoToExcel(Collections.emptyList());

            assertThat(bytes.length).isGreaterThan(2);
            assertThat(bytes[0]).isEqualTo((byte) 'P');
            assertThat(bytes[1]).isEqualTo((byte) 'K');
        }

        @Test
        @DisplayName("null 리스트 입력 시 — NullPointerException 으로 보호 (가드 부재 명문화)")
        void null_리스트_NPE() {
            // ExcelUtil.createOrderWorkSheet 가 null 가드 없는 현재 구현 명문화.
            // 향후 ExcelUtil 에 null 체크 가드 추가 시 이 테스트가 깨지면 expected 만 바꿔주면 됨.
            assertThatCode(() -> excelService.getFormatDtoToExcel(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
