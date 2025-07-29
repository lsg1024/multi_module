package com.msa.product.local.product.dto;

import com.msa.product.local.product.entity.ProductImage;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductImageDto {
    private String imageOriginName;

    @Builder
    public static class Response {
        private String imageId;
        private String imagePath;
        private String imageName;
        private String imageOriginName;

        public static Response fromEntity(ProductImage image) {
            return Response.builder()
                    .imageId(image.getImageId().toString())
                    .imagePath(image.getImagePath())
                    .imageName(image.getImageName())
                    .imageOriginName(image.getImageOriginName())
                    .build();
        }
    }
}
