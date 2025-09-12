package com.msa.order.local.order.external_client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductImageDto {

    private Long productId;
    private String imagePath;
}
