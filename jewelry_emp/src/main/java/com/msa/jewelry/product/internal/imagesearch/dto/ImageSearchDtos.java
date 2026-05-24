package com.msa.jewelry.product.internal.imagesearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "임베딩 요청 단위 아이템 — 상품 이미지 1건")
    public record EmbedItem(
            @Schema(description = "상품 ID", example = "1001") long productId,
            @Schema(description = "상품 이미지 ID", example = "5001") long productImageId,
            @Schema(description = "이미지 경로(URL)", example = "https://cdn.example.com/products/abc.jpg") String imagePath
    ) {}

    @Schema(description = "image-search-service 로 보내는 임베딩 요청")
    public record EmbedRequest(
            @Schema(description = "테넌트 ID (멀티 테넌시 키)", example = "jewelry-main") String tenantId,
            @Schema(description = "임베딩할 이미지 아이템 목록") List<EmbedItem> items
    ) {}

    @Schema(description = "임베딩 성공 결과 — 이미지 ID")
    public record EmbedSuccess(
            @Schema(description = "임베딩에 성공한 상품 이미지 ID", example = "5001") long productImageId
    ) {}

    @Schema(description = "임베딩 실패 결과 — 이미지 ID와 사유")
    public record EmbedFailure(
            @Schema(description = "임베딩에 실패한 상품 이미지 ID", example = "5001") long productImageId,
            @Schema(description = "실패 사유", example = "DOWNLOAD_FAILED") String reason
    ) {}

    @Schema(description = "임베딩 응답 — 성공·실패 분리")
    public record EmbedResponse(
            @Schema(description = "임베딩 모델 버전", example = "clip-vit-b32-2024") String modelVersion,
            @Schema(description = "성공 목록") List<EmbedSuccess> succeeded,
            @Schema(description = "실패 목록") List<EmbedFailure> failed
    ) {}

    // ============================================================
    // image-search-service /embed/delete
    // ============================================================
    @Schema(description = "임베딩 삭제 요청 — 이미지 ID 목록")
    public record EmbedDeleteRequest(
            @Schema(description = "테넌트 ID", example = "jewelry-main") String tenantId,
            @Schema(description = "삭제할 상품 이미지 ID 목록") List<Long> productImageIds
    ) {}

    @Schema(description = "임베딩 삭제 응답 — 삭제된 개수")
    public record EmbedDeleteResponse(
            @Schema(description = "실제 삭제된 임베딩 개수", example = "3") int deleted
    ) {}

    // ============================================================
    // image-search-service /search/text
    // ============================================================
    @Schema(description = "텍스트 기반 이미지 검색 요청")
    public record SearchTextRequest(
            @Schema(description = "테넌트 ID", example = "jewelry-main") String tenantId,
            @Schema(description = "검색 텍스트(자연어 쿼리)", example = "심플한 솔리테어 반지") String text,
            @Schema(description = "분류 필터", example = "반지") String classification,
            @Schema(description = "최대 결과 수 (Top-K)", example = "20") int topK
    ) {}

    @Schema(description = "이미지 검색 결과 한 건 — 유사도 점수")
    public record SearchHit(
            @Schema(description = "상품 ID", example = "1001") long productId,
            @Schema(description = "매칭된 상품 이미지 ID", example = "5001") long productImageId,
            @Schema(description = "유사도 점수 (0.0 ~ 1.0)", example = "0.87") double similarity
    ) {}

    @Schema(description = "image-search-service 내부 응답 — 모델 버전과 매칭 목록")
    public record SearchInternalResponse(
            @Schema(description = "검색 모델 버전", example = "clip-vit-b32-2024") String modelVersion,
            @Schema(description = "매칭된 이미지 목록") List<SearchHit> results
    ) {}

    // ============================================================
    // 사용자 노출 응답 (controller)
    // ============================================================
    @Schema(description = "메타데이터 매칭 정보 — 분류/재질/색상 (boost score 산정용)")
    public record MetaMatches(
            @Schema(description = "매칭된 분류명", example = "반지") String classification,
            @Schema(description = "매칭된 재질명", example = "18K") String material,
            @Schema(description = "매칭된 색상명", example = "옐로골드") String color
    ) {}

    @Schema(description = "검색 결과 한 건 — 사용자 노출용")
    public record SearchResultItem(
            @Schema(description = "상품 ID", example = "1001") long productId,
            @Schema(description = "상품명", example = "프로포즈 솔리테어 반지") String productName,
            @Schema(description = "상품 대표 이미지 경로", example = "https://cdn.example.com/products/abc.jpg") String mainImagePath,
            @Schema(description = "검색에 매칭된 이미지 경로", example = "https://cdn.example.com/products/abc-2.jpg") String matchedImagePath,
            @Schema(description = "원본 유사도 (0.0 ~ 1.0)", example = "0.87") double similarity,
            @Schema(description = "메타 매칭 가중치 적용 후 점수", example = "0.91") double boostedScore,
            @Schema(description = "메타데이터 매칭 정보") MetaMatches metaMatches
    ) {}

    @Schema(description = "사용자 노출용 이미지 검색 응답 — 전체 결과 + 메타")
    public record SearchResponse(
            @Schema(description = "검색 모델 버전", example = "clip-vit-b32-2024") String modelVersion,
            @Schema(description = "전체 후보 수", example = "120") int totalCandidates,
            @Schema(description = "최종 반환된 항목 수", example = "20") int returned,
            @Schema(description = "검색 결과 항목 목록") List<SearchResultItem> items
    ) {}
}
