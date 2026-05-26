package com.msa.jewelry.local.imagesearch.service;

import com.msa.common.global.tenant.TenantContext;
import com.msa.jewelry.local.imagesearch.client.ImageSearchClient;
import com.msa.jewelry.local.imagesearch.client.ImageSearchProperties;
import com.msa.jewelry.local.imagesearch.dto.ImageSearchDtos.*;
import com.msa.jewelry.local.imagesearch.service.ProductImageSearchService.ProductMetadataLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ProductImageSearchService 단위 테스트.
 *
 * <p>이미지 검색(searchByImage)과 텍스트 검색(searchByText) 두 진입점이 공유하는
 * {@code rerankAndEnrich} 로직(메타 조회, 부스팅, 정렬, topK 컷)을 집중적으로 검증한다.
 *
 * <p>외부 의존성:
 * <ul>
 *   <li>{@link ImageSearchClient} — 임베딩 서비스 호출. mock</li>
 *   <li>{@link ImageSearchProperties} — 점수 가중치. 실제 객체 사용</li>
 *   <li>{@link ProductMetadataLookup} — 상품 메타/이미지 경로 일괄 조회. mock</li>
 *   <li>{@link TenantContext} — ThreadLocal. set/clear 로 직접 제어</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductImageSearchService 단위 테스트")
class ProductImageSearchServiceTest {

    private static final String TENANT = "jewelry-main";

    @Mock ImageSearchClient client;
    @Mock ProductMetadataLookup productLookup;

    // properties 는 실제 객체로 — defaultTopK/candidateTopK/Scoring 기본값 활용
    ImageSearchProperties properties;

    ProductImageSearchService service;

    @BeforeEach
    void setUp() {
        properties = new ImageSearchProperties();
        properties.setDefaultTopK(20);
        properties.setCandidateTopK(200);
        // scoring 기본값: 0.7 / 0.15 / 0.15
        service = new ProductImageSearchService(client, properties, productLookup);

        TenantContext.setTenant(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ============================================================
    // searchByImage
    // ============================================================
    @Nested
    @DisplayName("searchByImage")
    class SearchByImage {

        private MultipartFile sampleFile() {
            return new MockMultipartFile(
                    "file", "query.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
            );
        }

        @Test
        @DisplayName("내부 결과 비어있음 — totalCandidates/returned 모두 0, items 빈 리스트")
        void 빈결과() throws IOException {
            given(client.searchByImage(any(), eq("query.jpg"), eq(TENANT), eq("반지"), anyInt()))
                    .willReturn(new SearchInternalResponse("v1", List.of()));

            SearchResponse resp = service.searchByImage(sampleFile(), "반지", null, null, null);

            assertThat(resp.modelVersion()).isEqualTo("v1");
            assertThat(resp.totalCandidates()).isZero();
            assertThat(resp.returned()).isZero();
            assertThat(resp.items()).isEmpty();
            verify(productLookup, never()).findByIds(anyCollection());
        }

        @Test
        @DisplayName("정상 — 메타 조회 후 매칭/정렬/topK 컷")
        void 정상() throws IOException {
            SearchHit h1 = new SearchHit(1001L, 5001L, 0.50);
            SearchHit h2 = new SearchHit(1002L, 5002L, 0.80);
            given(client.searchByImage(any(), eq("query.jpg"), eq(TENANT), eq("반지"), anyInt()))
                    .willReturn(new SearchInternalResponse("v1", List.of(h1, h2)));

            given(productLookup.findByIds(anyCollection())).willReturn(Map.of(
                    1001L, new ProductMetadataLookup.ProductMeta(
                            1001L, "P1", "main1.jpg", 10L, "14K", null, null, "반지"),
                    1002L, new ProductMetadataLookup.ProductMeta(
                            1002L, "P2", "main2.jpg", 10L, "14K", null, null, "반지")
            ));
            given(productLookup.findImagePaths(anyCollection())).willReturn(Map.of(
                    5001L, "img1.jpg",
                    5002L, "img2.jpg"
            ));

            SearchResponse resp = service.searchByImage(sampleFile(), "반지", List.of(10L), null, 10);

            assertThat(resp.totalCandidates()).isEqualTo(2);
            assertThat(resp.returned()).isEqualTo(2);
            // 정렬: similarity*0.7 + materialMatch*0.15
            // h2: 0.8*0.7 + 0.15 = 0.71  /  h1: 0.5*0.7 + 0.15 = 0.50
            assertThat(resp.items().get(0).productId()).isEqualTo(1002L);
            assertThat(resp.items().get(0).boostedScore())
                    .isCloseTo(0.71, org.assertj.core.data.Offset.offset(0.0001));
            assertThat(resp.items().get(0).metaMatches().material()).isEqualTo("14K");
            assertThat(resp.items().get(0).metaMatches().classification()).isEqualTo("반지");
        }

        @Test
        @DisplayName("메타 조회 결과에 없는 productId 는 필터링됨 (임베딩 정합성 이슈)")
        void 메타_없는_상품() throws IOException {
            SearchHit h1 = new SearchHit(1001L, 5001L, 0.9);
            SearchHit h2 = new SearchHit(9999L, 5002L, 0.85); // meta 에 없음
            given(client.searchByImage(any(), anyString(), anyString(), any(), anyInt()))
                    .willReturn(new SearchInternalResponse("v1", List.of(h1, h2)));
            given(productLookup.findByIds(anyCollection())).willReturn(Map.of(
                    1001L, new ProductMetadataLookup.ProductMeta(
                            1001L, "P1", "m1.jpg", null, null, null, null, null)
            ));
            given(productLookup.findImagePaths(anyCollection()))
                    .willReturn(Map.of(5001L, "i1.jpg"));

            SearchResponse resp = service.searchByImage(sampleFile(), null, null, null, null);

            assertThat(resp.totalCandidates()).isEqualTo(2);
            assertThat(resp.returned()).isEqualTo(1);
            assertThat(resp.items().get(0).productId()).isEqualTo(1001L);
        }

        @Test
        @DisplayName("외부 클라이언트 실패 → RestClientException 전파")
        void 외부_API_실패() {
            willThrow(new RestClientException("connection refused"))
                    .given(client).searchByImage(any(), anyString(), anyString(), any(), anyInt());

            assertThatThrownBy(() ->
                    service.searchByImage(sampleFile(), "반지", null, null, null))
                    .isInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("파일 IO 실패 — MultipartFile.getBytes 가 IOException 던지면 그대로 전파")
        void 파일_IO_실패() throws IOException {
            MultipartFile bad = mock(MultipartFile.class);
            given(bad.getBytes()).willThrow(new IOException("read fail"));
            given(bad.getOriginalFilename()).willReturn("x.jpg");

            assertThatThrownBy(() -> service.searchByImage(bad, null, null, null, null))
                    .isInstanceOf(IOException.class);
            verify(client, never()).searchByImage(any(), anyString(), anyString(), any(), anyInt());
        }

        @Test
        @DisplayName("originalFilename null — query.jpg 로 대체")
        void 파일명_null() throws IOException {
            MultipartFile bad = mock(MultipartFile.class);
            given(bad.getBytes()).willReturn(new byte[]{0});
            given(bad.getOriginalFilename()).willReturn(null);
            given(client.searchByImage(any(), eq("query.jpg"), anyString(), any(), anyInt()))
                    .willReturn(new SearchInternalResponse("v1", List.of()));

            service.searchByImage(bad, null, null, null, null);

            verify(client).searchByImage(any(), eq("query.jpg"), eq(TENANT), org.mockito.ArgumentMatchers.isNull(), anyInt());
        }
    }

    // ============================================================
    // searchByText
    // ============================================================
    @Nested
    @DisplayName("searchByText")
    class SearchByText {

        @Test
        @DisplayName("정상 — color 매치 부스팅")
        void color_매칭() {
            SearchHit h1 = new SearchHit(2001L, 6001L, 0.6);
            given(client.searchByText(any(SearchTextRequest.class)))
                    .willReturn(new SearchInternalResponse("v2", List.of(h1)));
            given(productLookup.findByIds(anyCollection())).willReturn(Map.of(
                    2001L, new ProductMetadataLookup.ProductMeta(
                            2001L, "PC", "main.jpg", null, null, 7L, "옐로골드", "목걸이")
            ));
            given(productLookup.findImagePaths(anyCollection()))
                    .willReturn(Map.of(6001L, "img.jpg"));

            SearchResponse resp = service.searchByText("심플", "목걸이", null, List.of(7L), 5);

            assertThat(resp.items()).hasSize(1);
            // 0.6*0.7 + 0.15(color) = 0.57
            assertThat(resp.items().get(0).boostedScore())
                    .isCloseTo(0.57, org.assertj.core.data.Offset.offset(0.0001));
            assertThat(resp.items().get(0).metaMatches().color()).isEqualTo("옐로골드");
        }

        @Test
        @DisplayName("빈 검색 결과 — items 빈 리스트")
        void 빈결과() {
            given(client.searchByText(any(SearchTextRequest.class)))
                    .willReturn(new SearchInternalResponse("v2", List.of()));

            SearchResponse resp = service.searchByText("x", null, null, null, null);

            assertThat(resp.items()).isEmpty();
            assertThat(resp.returned()).isZero();
        }

        @Test
        @DisplayName("topK 가 candidateTopK 보다 크면 candidateTopK 로 제한 — 결과 크기 검증")
        void topK_상한() {
            // candidateTopK 기본 200. topK=500 으로 호출해도 컷 동작 (내부 결과는 1건이므로 1건 반환)
            given(client.searchByText(any(SearchTextRequest.class)))
                    .willReturn(new SearchInternalResponse("v2", List.of(
                            new SearchHit(3001L, 7001L, 0.5)
                    )));
            given(productLookup.findByIds(anyCollection())).willReturn(Map.of(
                    3001L, new ProductMetadataLookup.ProductMeta(
                            3001L, "P", "m.jpg", null, null, null, null, null)
            ));
            given(productLookup.findImagePaths(anyCollection()))
                    .willReturn(Map.of(7001L, "i.jpg"));

            SearchResponse resp = service.searchByText("x", null, null, null, 500);

            assertThat(resp.items()).hasSize(1);
        }

        @Test
        @DisplayName("topK null/0 — defaultTopK 사용 (실제 hit 1개 → 1건 반환)")
        void topK_null이면_default() {
            given(client.searchByText(any(SearchTextRequest.class)))
                    .willReturn(new SearchInternalResponse("v2", List.of(
                            new SearchHit(3001L, 7001L, 0.5)
                    )));
            given(productLookup.findByIds(anyCollection())).willReturn(Map.of(
                    3001L, new ProductMetadataLookup.ProductMeta(
                            3001L, "P", "m.jpg", null, null, null, null, null)
            ));
            given(productLookup.findImagePaths(anyCollection()))
                    .willReturn(Map.of(7001L, "i.jpg"));

            SearchResponse resp = service.searchByText("x", null, null, null, 0);

            assertThat(resp.items()).hasSize(1);
        }

        @Test
        @DisplayName("필터 모두 null — 부스팅 가산점 없이 similarity 가중치만 적용")
        void 필터_없음() {
            given(client.searchByText(any(SearchTextRequest.class)))
                    .willReturn(new SearchInternalResponse("v2", List.of(
                            new SearchHit(4001L, 8001L, 0.5)
                    )));
            given(productLookup.findByIds(anyCollection())).willReturn(Map.of(
                    4001L, new ProductMetadataLookup.ProductMeta(
                            4001L, "P", "m.jpg", 10L, "14K", 7L, "옐로", "반지")
            ));
            given(productLookup.findImagePaths(anyCollection()))
                    .willReturn(Map.of(8001L, "i.jpg"));

            SearchResponse resp = service.searchByText("x", "반지", null, null, 5);

            // material/color 필터 모두 비어있으므로 매치 없음 → 0.5 * 0.7 = 0.35
            assertThat(resp.items().get(0).boostedScore())
                    .isCloseTo(0.35, org.assertj.core.data.Offset.offset(0.0001));
            assertThat(resp.items().get(0).metaMatches().material()).isNull();
            assertThat(resp.items().get(0).metaMatches().color()).isNull();
        }
    }
}
