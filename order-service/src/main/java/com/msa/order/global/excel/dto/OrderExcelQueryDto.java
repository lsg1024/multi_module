package com.msa.order.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderExcelQueryDto {
    private String factory;
    private String productFactoryName;
    private String material;
    private String color;
    private String orderMainStoneNote;
    private String orderAssistanceStoneNote;
    private String productSize;
    private String orderNote;

    @QueryProjection
    public OrderExcelQueryDto(String factory, String productFactoryName, String material, String color, String orderMainStoneNote, String orderAssistanceStoneNote, String productSize, String orderNote) {
        this.factory = factory;
        this.productFactoryName = productFactoryName;
        this.material = material;
        this.color = color;
        this.orderMainStoneNote = orderMainStoneNote;
        this.orderAssistanceStoneNote = orderAssistanceStoneNote;
        this.productSize = productSize;
        this.orderNote = orderNote;
    }
}
