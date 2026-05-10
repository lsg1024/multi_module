package com.msa.jewelry.product.internal.imagesearch.service;

import com.msa.common.global.tenant.TenantContext;
import com.msa.jewelry.product.internal.imagesearch.client.ImageSearchClient;
import com.msa.jewelry.product.internal.imagesearch.client.ImageSearchProperties;
import com.msa.jewelry.product.internal.imagesearch.dto.ImageSearchDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 노출 검색 비즈니스 로직.
 *
 * 흐름 (DESIGN.md §3.2):
 *   1) image-search-service에 후보 top-N(기본 200) 조회
 *   2) product_id 일괄 enrich (name, mainImagePath, material, color)
 *   3) soft 필터(material/color)로 점수 부스팅
 *   4) 최종 top-K 정렬 후 반환
 *
 * NOTE: ProductRepository / ProductImageRepository / 메타데이터 조회는
 *       기존 product-service 구조에 맞춰 주입해야 한다. 본 클래스는 인터페이스
 *       의존을 명시적으로 두고 구현은 별도 어댑터로 분리하길 권장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageSearchService {

    private final ImageSearchClient client;
    private final ImageSearchProperties properties;
    private final ProductMetadataLookup productLookup; // 아래 인터페이스 정의

    // ============================================================
    // Public API
    // ============================================================
    public SearchResponse searchByImage(
            MultipartFile file,
            String classification,
            List<Long> materialIds,
            List<Long> colorIds,
            Integer topK
    ) throws IOException {
        String tenantId = TenantContext.getTenant();

        SearchInternalResponse internal = client.searchByImage(
                file.getBytes(),
                Optional.ofNullable(file.getOriginalFilename()).orElse("query.jpg"),
                tenantId,
                classification,
                properties.getCandidateTopK()
        );

        return rerankAndEnrich(internal, materialIds, colorIds, classification, resolveTopK(topK));
    }

    public SearchResponse searchByText(
            String text,
            String classification,
            List<Long> materialIds,
            List<Long> colorIds,
            Integer topK
    ) {
        String tenantId = TenantContext.getTenant();

        SearchInternalResponse internal = client.searchByText(new SearchTextRequest(
                tenantId, text, classification, properties.getCandidateTopK()
        ));

        return rerankAndEnrich(internal, materialIds, colorIds, classification, resolveTopK(topK));
    }

    // ============================================================
    // Internal — enrich + soft 부스팅
    // ============================================================
    private SearchResponse rerankAndEnrich(
            SearchInternalResponse internal,
            List<Long> materialIds,
            List<Long> colorIds,
            String classification,
            int topK
    ) {
        if (internal.results() == null || internal.results().isEmpty()) {
            return new SearchResponse(internal.modelVersion(), 0, 0, List.of());
        }

        // 1) productId, productImageId 일괄 추출 후 메타 일괄 조회 (N+1 방지)
        Set<Long> productIds = internal.results().stream()
                .map(SearchHit::productId)
                .collect(Collectors.toSet());
        Set<Long> imageIds = internal.results().stream()
                .map(SearchHit::productImageId)
                .collect(Collectors.toSet());

        Map<Long, ProductMetadataLookup.ProductMeta> metaMap = productLookup.findByIds(productIds);
        Map<Long, String> imagePathMap = productLookup.findImagePaths(imageIds);

        // 2) 부스팅
        ImageSearchProperties.Scoring s = properties.getScoring();
        Set<Long> materialFilter = materialIds == null ? Set.of() : new HashSet<>(materialIds);
        Set<Long> colorFilter    = colorIds    == null ? Set.of() : new HashSet<>(colorIds);

        List<SearchResultItem> reranked = internal.results().stream()
                .map(hit -> {
                    ProductMetadataLookup.ProductMeta meta = metaMap.get(hit.productId());
                    if (meta == null) {
                        return null; // 기준 시점 product가 사라진 경우 — 임베딩 정합성 이슈
                    }
                    boolean materialMatch = !materialFilter.isEmpty()
                            && meta.materialId() != null
                            && materialFilter.contains(meta.materialId());
                    boolean colorMatch = !colorFilter.isEmpty()
                            && meta.colorId() != null
                            && colorFilter.contains(meta.colorId());

                    double score = s.getSimilarityWeight() * hit.similarity()
                            + (materialMatch ? s.getMaterialWeight() : 0)
                            + (colorMatch    ? s.getColorWeight()    : 0);

                    return new SearchResultItem(
                            meta.productId(),
                            meta.productName(),
                            meta.mainImagePath(),
                            imagePathMap.get(hit.productImageId()),
                            hit.similarity(),
                            score,
                            new MetaMatches(
                                    classification,
                                    materialMatch ? meta.materialName() : null,
                                    colorMatch    ? meta.colorName()    : null
                            )
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(SearchResultItem::boostedScore).reversed())
                .limit(topK)
                .toList();

        return new SearchResponse(
                internal.modelVersion(),
                internal.results().size(),
                reranked.size(),
                reranked
        );
    }

    private int resolveTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return properties.getDefaultTopK();
        }
        return Math.min(topK, properties.getCandidateTopK());
    }

    // ============================================================
    // 메타 조회 어댑터 인터페이스 — 실제 구현은 product 도메인에 둘 것
    // ============================================================
    public interface ProductMetadataLookup {
        Map<Long, ProductMeta> findByIds(Collection<Long> productIds);

        /** productImageId 집합 → imageId, 상대 imagePath 매핑 (일괄, N+1 방지) */
        Map<Long, String> findImagePaths(Collection<Long> imageIds);

        record ProductMeta(
                long productId,
                String productName,
                String mainImagePath,
                Long materialId,
                String materialName,
                Long colorId,
                String colorName,
                String classification
        ) {}
    }
}
