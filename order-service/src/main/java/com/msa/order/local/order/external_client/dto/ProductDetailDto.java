package com.msa.order.local.order.external_client.dto;

import com.msa.order.global.dto.StoneDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
public class ProductDetailDto {
    private Long productId;
    private String productName;
    private String classificationName;
    private String setType;
    private Integer purchaseCost;
    private Integer laborCost;
    private Integer addLaborCost;
    private List<StoneDto.StoneInfo> stoneInfos;
}
