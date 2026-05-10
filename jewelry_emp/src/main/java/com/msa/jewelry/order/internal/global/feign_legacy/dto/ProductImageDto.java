package com.msa.jewelry.order.internal.global.feign_legacy.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductImageDto {

    private Long productId;
    private String imagePath;
}
