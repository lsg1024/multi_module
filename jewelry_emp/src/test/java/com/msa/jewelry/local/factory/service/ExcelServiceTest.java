package com.msa.jewelry.local.factory.service;

import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("factory/ExcelService 단위 테스트")
class ExcelServiceTest {

    private final ExcelService excelService = new ExcelService();

    @Nested
    @DisplayName("getFormatDtoToExcel")
    class GetFormatDtoToExcel {

        @Test
        @DisplayName("빈 리스트 — 헤더만 있는 워크북 byte[] 반환 (길이 > 0)")
        void 빈리스트_헤더만() throws Exception {
            byte[] result = excelService.getFormatDtoToExcel(Collections.emptyList(), "STORE");

            assertThat(result).isNotNull();
            assertThat(result.length).isPositive();
        }

        @Test
        @DisplayName("type 파라미터가 다양해도 예외 없이 처리 (STORE/FACTORY 등)")
        void 다양한_type() {
            assertThatCode(() -> excelService.getFormatDtoToExcel(Collections.emptyList(), "STORE"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> excelService.getFormatDtoToExcel(Collections.emptyList(), "FACTORY"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("XLSX 시그니처 — 처음 두 바이트가 'PK' (ZIP 매직넘버, XLSX 는 ZIP 컨테이너)")
        void XLSX_시그니처() throws Exception {
            byte[] bytes = excelService.getFormatDtoToExcel(Collections.emptyList(), "STORE");

            assertThat(bytes.length).isGreaterThan(2);
            assertThat(bytes[0]).isEqualTo((byte) 'P');
            assertThat(bytes[1]).isEqualTo((byte) 'K');
        }
    }
}
