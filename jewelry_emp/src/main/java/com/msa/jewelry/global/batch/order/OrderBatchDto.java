package com.msa.jewelry.global.batch.order;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class OrderBatchDto {

    private String legacyOrderNo;     // 원본 주문번호 (멱등키)
    private String orderStatus;       // ORDER / STOCK
    private String storeName;         // 거래처 전체명 (미존재 시 자동 생성)
    private String factoryName;       // 제조사 전체명 (null = 미지정)
    private String createAt;          // 접수일 yyyy-MM-dd
    private String shippingAt;        // 출고(예정)일 yyyy-MM-dd
    private String priorityName;      // 일반 / 급
    private String note;

    // 원본 작성/수정 메타 (추적 보존용 — order_note 병기)
    private String createdBy;
    private String createdDate;
    private String updatedBy;
    private String updatedDate;

    private Product product;
    private List<Stone> stones = new ArrayList<>();

    @Getter
    @NoArgsConstructor
    public static class Product {
        private String modelName;          // product.product_name 조인 키
        private String factoryModelName;   // 제조사 모델번호
        private String materialName;       // 14K/18K/...
        private String colorName;          // 자유값 (미존재 시 자동 생성)
        private String size;
        private Integer qty;
        private Integer laborBase;         // 기본공임 → product_labor_cost
        private Integer laborAdd;          // 추가공임 → product_add_labor_cost
        private String mainStoneNote;
        private String assistanceStoneNote;
        private Double goldWeight;         // 대개 null (주문 화면에 실측 없음)
        private Double stoneWeight;
    }

    @Getter
    @NoArgsConstructor
    public static class Stone {
        private Boolean isMain;
        private Integer laborCost;
        private Integer quantity;
        private String stoneName;
    }
}
