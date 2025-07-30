package com.msa.product.local.product.dto;

import com.msa.product.local.product.entity.ProductImage;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductImageDto {
    private String imageOriginName;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String imagePath;

        @Builder
        @QueryProjection
        public Response(String imagePath) {
            this.imagePath = imagePath;
        }

        public static Response fromEntity(ProductImage image) {
            return Response
                    .builder()
                    .imagePath(image.getImagePath())
                    .build();
        }
    }
}
