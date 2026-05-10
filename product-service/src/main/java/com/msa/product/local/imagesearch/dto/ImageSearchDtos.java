package com.msa.product.local.imagesearch.dto;

import java.util.List;

/**
 * image-search-service ↔ product-service 간 DTO + 사용자 응답 DTO 모음.
 * 단일 파일에 묶어 가독성 ↑ (record).
 */
public final class ImageSearchDtos {

    private ImageSearchDtos() {}

    // ============================================================
    // image-search-service /embed 요청
    // ============================================================
    public record EmbedItem(long productId, long productImageId, String imagePath) {}

    public record EmbedRequest(String tenantId, List<EmbedItem> items) {}

    public record EmbedSuccess(long productImageId) {}

    public record EmbedFailure(long productImageId, String reason) {}

    public record EmbedResponse(
            String modelVersion,
            List<EmbedSuccess> succeeded,
            List<EmbedFailure> failed
    ) {}

    // ============================================================
    // image-search-service /embed/delete
    // ============================================================
    public record EmbedDeleteRequest(String tenantId, List<Long> productImageIds) {}

    public record EmbedDeleteResponse(int deleted) {}

    // ============================================================
    // image-search-service /search/text
    // ============================================================
    public record SearchTextRequest(
            String tenantId,
            String text,
            String classification,
            int topK
    ) {}

    public record SearchHit(long productId, long productImageId, double similarity) {}

    public record SearchInternalResponse(String modelVersion, List<SearchHit> results) {}

    // ============================================================
    // 사용자 노출 응답 (controller)
    // ============================================================
    public record MetaMatches(String classification, String material, String color) {}

    public record SearchResultItem(
            long productId,
            String productName,
            String mainImagePath,
            String matchedImagePath,
            double similarity,
            double boostedScore,
            MetaMatches metaMatches
    ) {}

    public record SearchResponse(
            String modelVersion,
            int totalCandidates,
            int returned,
            List<SearchResultItem> items
    ) {}
}
