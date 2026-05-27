package com.msa.jewelry.local.catalog.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.excel.dto.CatalogExcelDto;
import com.msa.jewelry.local.catalog.dto.CatalogProductDto;
import com.msa.jewelry.local.catalog.repository.CatalogRepository;
import com.msa.jewelry.local.stock.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CatalogService 단위 테스트")
class CatalogServiceTest {

    private static final String TOKEN = "Bearer store-token";
    private static final Long PRODUCT_ID = 1001L;

    @Mock CatalogRepository catalogRepository;
    @Mock AuthorityUserRoleUtil authorityUserRoleUtil;
    @Mock StockService stockService;

    @InjectMocks CatalogService catalogService;

    // -----------------------------------------------------------------------
    // getCatalogProducts
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getCatalogProducts")
    class GetCatalogProducts {

        @Test
        @DisplayName("판매처 권한 없음 → IllegalArgumentException 즉시 종료")
        void 권한_없음() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(false);
            Pageable pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() -> catalogService.getCatalogProducts(TOKEN, "반지", "1", "1", "name", "asc", pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("판매처 계정만 접근 가능합니다.");

            verifyNoInteractions(catalogRepository, stockService);
        }

        @Test
        @DisplayName("결과 비어있음 — stockService 호출 안 함")
        void 빈_결과_재고조회_생략() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            Pageable pageable = PageRequest.of(0, 20);

            CustomPage<CatalogProductDto.Page> empty = new CustomPage<>(
                    Collections.emptyList(), pageable, 0L);
            given(catalogRepository.findCatalogProducts(any(), any(), any(), any(), any(), eq(pageable)))
                    .willReturn(empty);

            CustomPage<CatalogProductDto.Page> result =
                    catalogService.getCatalogProducts(TOKEN, "반지", "1", "1", "name", "asc", pageable);

            assertThat(result.getContent()).isEmpty();
            verify(stockService, never()).getStockCountByProductNames(anyList());
        }

        @Test
        @DisplayName("정상 — productName 추출 후 stockCount 채워서 반환")
        void 정상_재고채움() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            Pageable pageable = PageRequest.of(0, 20);

            CatalogProductDto.Page page1 = CatalogProductDto.Page.builder()
                    .productId("1001")
                    .productName("프로포즈 솔리테어 반지")
                    .build();
            CatalogProductDto.Page page2 = CatalogProductDto.Page.builder()
                    .productId("1002")
                    .productName("심플 14K 목걸이")
                    .build();
            CustomPage<CatalogProductDto.Page> resultPage = new CustomPage<>(
                    List.of(page1, page2), pageable, 2L);

            given(catalogRepository.findCatalogProducts(any(), any(), any(), any(), any(), eq(pageable)))
                    .willReturn(resultPage);
            given(stockService.getStockCountByProductNames(anyList()))
                    .willReturn(Map.of("프로포즈 솔리테어 반지", 7, "심플 14K 목걸이", 0));

            CustomPage<CatalogProductDto.Page> result =
                    catalogService.getCatalogProducts(TOKEN, null, null, null, "name", "asc", pageable);

            assertThat(result.getContent()).extracting(CatalogProductDto.Page::getStockCount)
                    .containsExactly(7, 0);
        }

        @Test
        @DisplayName("재고 맵에 없는 상품명 → getOrDefault(0) 로 0 채워짐")
        void 누락_재고_0() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            Pageable pageable = PageRequest.of(0, 20);

            CatalogProductDto.Page page1 = CatalogProductDto.Page.builder()
                    .productId("1001")
                    .productName("존재하지않는상품")
                    .build();
            CustomPage<CatalogProductDto.Page> resultPage = new CustomPage<>(
                    List.of(page1), pageable, 1L);

            given(catalogRepository.findCatalogProducts(any(), any(), any(), any(), any(), eq(pageable)))
                    .willReturn(resultPage);
            given(stockService.getStockCountByProductNames(anyList()))
                    .willReturn(Collections.emptyMap());

            CustomPage<CatalogProductDto.Page> result =
                    catalogService.getCatalogProducts(TOKEN, null, null, null, null, null, pageable);

            assertThat(result.getContent().get(0).getStockCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // getCatalogProductDetail
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getCatalogProductDetail")
    class GetCatalogProductDetail {

        @Test
        @DisplayName("판매처 권한 없음 → IllegalArgumentException")
        void 권한_없음() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> catalogService.getCatalogProductDetail(TOKEN, PRODUCT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("판매처 계정만 접근 가능합니다.");

            verifyNoInteractions(catalogRepository);
        }

        @Test
        @DisplayName("정상 — 상세 반환")
        void 정상() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            CatalogProductDto.Detail detail = mock(CatalogProductDto.Detail.class);
            given(catalogRepository.findCatalogProductDetail(PRODUCT_ID)).willReturn(detail);

            assertThat(catalogService.getCatalogProductDetail(TOKEN, PRODUCT_ID)).isSameAs(detail);
        }

        @Test
        @DisplayName("상품 미존재 → IllegalArgumentException(상품을 찾을 수 없습니다.)")
        void 상품_없음() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            given(catalogRepository.findCatalogProductDetail(PRODUCT_ID)).willReturn(null);

            assertThatThrownBy(() -> catalogService.getCatalogProductDetail(TOKEN, PRODUCT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품을 찾을 수 없습니다.");
        }
    }

    // -----------------------------------------------------------------------
    // getRelatedProducts
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getRelatedProducts")
    class GetRelatedProducts {

        @Test
        @DisplayName("판매처 권한 없음 → 예외")
        void 권한_없음() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> catalogService.getRelatedProducts(TOKEN, PRODUCT_ID, "R-2024-SERIES"))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(catalogRepository);
        }

        @Test
        @DisplayName("정상 — repository 결과 그대로 반환")
        void 정상() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            CatalogProductDto.RelatedProduct r = CatalogProductDto.RelatedProduct.builder()
                    .productId(2001L).productName("관련상품A").imagePath("/img/a.jpg").build();
            given(catalogRepository.findRelatedProducts(PRODUCT_ID, "R-2024-SERIES"))
                    .willReturn(List.of(r));

            assertThat(catalogService.getRelatedProducts(TOKEN, PRODUCT_ID, "R-2024-SERIES"))
                    .containsExactly(r);
        }

        @Test
        @DisplayName("빈 결과")
        void 빈결과() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            given(catalogRepository.findRelatedProducts(any(), anyString()))
                    .willReturn(Collections.emptyList());

            assertThat(catalogService.getRelatedProducts(TOKEN, PRODUCT_ID, "X")).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getCatalogProductsExcel
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getCatalogProductsExcel")
    class GetCatalogProductsExcel {

        @Test
        @DisplayName("판매처 권한 없음 → 예외, repository 호출 안 함")
        void 권한_없음() {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> catalogService.getCatalogProductsExcel(TOKEN, "반지", "1", "1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("판매처 계정만 접근 가능합니다.");

            verifyNoInteractions(catalogRepository);
        }

        @Test
        @DisplayName("빈 결과 — 헤더만 들어있는 워크북 byte[] (길이 > 0)")
        void 빈결과_헤더만() throws Exception {
            given(authorityUserRoleUtil.isStore(TOKEN)).willReturn(true);
            given(catalogRepository.findCatalogProductsForExcel(any(), any(), any()))
                    .willReturn(Collections.<CatalogExcelDto>emptyList());

            byte[] bytes = catalogService.getCatalogProductsExcel(TOKEN, "반지", "1", "1");

            assertThat(bytes).isNotEmpty();
        }
    }
}
