package com.msa.jewelry.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "주문 엑셀 출력용 쿼리 프로젝션 DTO — 주문 한 건의 엑셀 한 행을 표현.")
public class OrderExcelQueryDto {
    @Schema(description = "제조사 이름", example = "삼성공방")
    private String factory;
    @Schema(description = "상품 제조사 표기명 (스냅샷)", example = "삼성공방")
    private String productFactoryName;
    @Schema(description = "재질", example = "18K")
    private String material;
    @Schema(description = "색상", example = "옐로우골드")
    private String color;
    @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
    private String orderMainStoneNote;
    @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
    private String orderAssistanceStoneNote;
    @Schema(description = "상품 사이즈", example = "15호")
    private String productSize;
    @Schema(description = "주문 비고", example = "포장 정중하게")
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
