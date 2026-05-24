package com.msa.jewelry.product.internal.product.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "상품 이미지 DTO 묶음")
public class ProductImageDto {
    @Schema(description = "사용자 업로드 시 원본 파일명", example = "내제품사진.jpg")
    private String imageOriginName;

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 이미지 응답 DTO")
    public static class Response {

        @Schema(description = "이미지 ID", example = "5001")
        private String imageId;
        @Schema(description = "이미지 저장 경로 (URL)", example = "https://cdn.example.com/products/abc.jpg")
        private String imagePath;
        @Schema(description = "저장된 파일명 (서버 측 고유)", example = "abc-1715842322.jpg")
        private String imageName;
        @Schema(description = "사용자 업로드 시 원본 파일명", example = "내제품사진.jpg")
        private String imageOriginName;
        @Schema(description = "메인 이미지 여부", example = "true")
        private Boolean imageMain;

        @Builder
        @QueryProjection
        public Response(String imageId, String imagePath, String imageName, String imageOriginName, Boolean imageMain) {
            this.imageId = imageId;
            this.imagePath = imagePath;
            this.imageName = imageName;
            this.imageOriginName = imageOriginName;
            this.imageMain = imageMain;
        }

        @Builder
        @QueryProjection
        public Response(String imageId, String imagePath) {
            this.imageId = imageId;
            this.imagePath = imagePath;
        }

    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 ID + 대표 이미지 경로 묶음 (다건 일괄 조회 결과 element)")
    public static class ProductImageResponse {
        @Schema(description = "상품 ID", example = "1001")
        private Long productId;
        @Schema(description = "대표 이미지 경로", example = "https://cdn.example.com/products/abc.jpg")
        private String imagePath;

        @Builder
        @QueryProjection
        public ProductImageResponse(Long productId, String imagePath) {
            this.productId = productId;
            this.imagePath = imagePath;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "상품 이미지 업로드/순서 변경 요청 DTO")
    public static class Request {
        @Schema(description = "메인 이미지로 지정할 이미지의 0-based 인덱스", example = "0")
        private int mainImageIndex;
        @Valid
        @Schema(description = "이미지 메타데이터 목록 (업로드 파일과 순서 매칭)")
        private List<ImageMeta> images;

        @Getter
        @NoArgsConstructor
        @Schema(description = "이미지 메타 — 기존 이미지의 식별자")
        public static class ImageMeta {
            @Schema(description = "기존 이미지 ID (신규 업로드면 null)", example = "5001")
            private Long id;
        }
    }
}
