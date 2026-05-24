package com.msa.jewelry.product.internal.product.entity;

import com.msa.jewelry.product.internal.product.dto.ProductStoneDto;
import com.msa.jewelry.product.internal.stone.stone.entity.Stone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "PRODUCT_STONE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "상품-스톤 매핑 — Product 에 어떤 Stone 이 몇 개 들어가는지, 가격 포함 여부 등을 기록")
public class ProductStone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_STONE_ID")
    @Schema(description = "상품-스톤 매핑 PK", example = "7001")
    private Long productStoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    @Schema(description = "소속 상품")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STONE_ID", nullable = false)
    @Schema(description = "매핑된 스톤 마스터 (판매 단가 산출 근거)")
    private Stone stone; // 판매 단가 호출 필요

    @Column(name = "MAIN_STONE")
    @Schema(description = "메인 스톤 여부 — TRUE 면 상품 대표 스톤", example = "true")
    private Boolean mainStone; // 메인 여부

    @Column(name = "INCLUDE_STONE")
    @Schema(description = "스톤 자체 포함 여부 (가격 산정 대상 여부)", example = "true")
    private Boolean includeStone; // 포함 여부

    @Column(name = "INCLUDE_QUANTITY")
    @Schema(description = "스톤 개수 가격 산정 포함 여부", example = "true")
    private Boolean includeQuantity; // 알 개수 포함 여부

    @Column(name = "INCLUDE_PRICE")
    @Schema(description = "스톤 가격(단가) 산정 포함 여부", example = "true")
    private Boolean includePrice; // 가격 포함 여부

    @Column(name = "STONE_QUANTITY")
    @Schema(description = "스톤 개수 (알 수)", example = "4")
    private Integer stoneQuantity;

    @Column(name = "PRODUCT_STONE_NOTE")
    @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알")
    private String productStoneNote;

    @Builder
    public ProductStone(Product product, Stone stone, Boolean mainStone, Boolean includeStone, Boolean includeQuantity, Boolean includePrice, Integer stoneQuantity, String productStoneNote) {
        this.product = product;
        this.stone = stone;
        this.mainStone = mainStone;
        this.includeStone = includeStone;
        this.includeQuantity = includeQuantity != null ? includeQuantity : true;
        this.includePrice = includePrice != null ? includePrice : true;
        this.stoneQuantity = stoneQuantity;
        this.productStoneNote = productStoneNote;
    }

    public void updateStone(ProductStoneDto.Request dto) {
        this.mainStone = dto.isMainStone();
        this.includeStone = dto.isIncludeStone();
        this.includeQuantity = dto.isIncludeQuantity();
        this.includePrice = dto.isIncludePrice();
        this.stoneQuantity = dto.getStoneQuantity();
    }

    public void updateIncludeQuantity(boolean includeQuantity) {
        this.includeQuantity = includeQuantity;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
    public void setStone(Stone stone) {
        this.stone = stone;
    }
}
