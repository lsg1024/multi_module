package com.msa.order.local.stock.dto;

import com.msa.order.global.dto.StoneDto;
import com.msa.order.local.order.dto.OrderDto;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class StockDto {

    // 직접 재고 등록 시
    @Getter
    @NoArgsConstructor
    public static class createStockRequest {
        @NotBlank(message = "판매처 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "판매처를 선택해주세요.")
        private String storeId;
        private String storeName;
        private String storeGrade;
        private String storeHarry;

        @NotBlank(message = "공장 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "공장을 선택해주세요.")
        private String factoryId;
        private String factoryName;

        @NotBlank(message = "상품 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상품을 선택해주세요.")
        private String productId;
        private String productName;
        private String productFactoryName;
        private String productSize;
        private String stockNote;
        private Boolean isProductWeightSale;
        private Integer productPurchaseCost;
        private Integer productAddLaborCost;

        @NotBlank(message = "재질 값은 필수입니다.")
        @Pattern(regexp = "\\d+", message = "재질을 선택해주세요")
        private String materialId;
        private String materialName;
        @NotBlank(message = "색상 값은 필수입니다.")
        @Pattern(regexp = "\\d+", message = "색상을 선택해주세요.")
        private String colorId;
        private String colorName;
        private String classificationId;
        private String classificationName;
        private String setTypeId;
        private String setTypeName;

        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;
        private String mainStoneNote;
        private String assistanceStoneNote;

        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneCreateAt;

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;
        private Integer stoneAddLaborCost;
    }

    @Getter
    @NoArgsConstructor
    public static class stockRequest {
        private String productSize;
        private Boolean isProductWeightSale;
        private Integer addProductLaborCost;
        private Integer stoneAddLaborCost;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneCreateAt;
        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    public static class StockRentalRequest {
        @NotBlank(message = "상점 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 입력 값 오류.")
        private String storeId;
        private String productSize;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        private Boolean isProductWeightSale;
        private String goldWeight;
        private String stoneWeight;
        private Integer addProductLaborCost;
        private Integer stoneAddLaborCost;
        @Valid
        private List<StoneDto.StoneInfo> stoneInfos; // 개당 알수는 직접 수정 불가
    }


    @Getter
    @NoArgsConstructor
    public static class StockRegisterResponse {
        private String createAt;
        private String flowCode;
        private String storeId;
        private String storeName;
        private String storeHarry;
        private String factoryId;
        private String factoryName;
        private String productId;
        private String productName;
        private String productSize;
        private boolean isProductWeightSale;
        private Integer productPurchaseCost;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private String goldWeight;
        private String stoneWeight;
        private String materialName;
        private String colorName;
        private String orderNote;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneName;
        private OffsetDateTime assistantStoneCreateAt;
        private List<StoneDto.StoneInfo> stoneInfos;
        private Integer stoneAddLaborCost;

        @Builder
        public StockRegisterResponse(String createAt, String flowCode, String storeId, String storeName, String factoryId, String factoryName, String productId, String productName, String productSize, boolean isProductWeightSale, String storeHarry, Integer productPurchaseCost, Integer productLaborCost, Integer productAddLaborCost, String goldWeight, String stoneWeight, String materialName, String colorName, String orderNote, String mainStoneNote, String assistanceStoneNote, boolean assistantStone, String assistantStoneId, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, List<StoneDto.StoneInfo> stoneInfos, Integer stoneAddLaborCost) {
            this.createAt = createAt;
            this.flowCode = flowCode;
            this.storeId = storeId;
            this.storeName = storeName;
            this.factoryId = factoryId;
            this.factoryName = factoryName;
            this.productId = productId;
            this.productName = productName;
            this.productSize = productSize;
            this.isProductWeightSale = isProductWeightSale;
            this.stoneAddLaborCost = stoneAddLaborCost;
            this.storeHarry = storeHarry;
            this.productPurchaseCost = productPurchaseCost;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.materialName = materialName;
            this.colorName = colorName;
            this.orderNote = orderNote;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.assistantStone = assistantStone;
            this.assistantStoneId = assistantStoneId;
            this.assistantStoneName = assistantStoneName;
            this.assistantStoneCreateAt = assistantStoneCreateAt;
            this.stoneInfos = stoneInfos;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StockRegisterRequest {
        private String createAt;
        private String flowCode;
        private String materialId;
        private String materialName;
        private String colorId;
        private String colorName;
        private String productSize;
        private boolean isProductWeightSale;
        private Integer productPurchaseCost;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private String storeHarry;
        private String goldWeight;
        private String stoneWeight;
        private String orderNote;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String assistantStoneId;
        private boolean assistantStone;
        private String assistantStoneName;
        private String assistantStoneCreateAt;
        private List<StoneDto.StoneInfo> stoneInfos;
        private Integer stoneAddLaborCost;
    }

    // 재고값 상세 조회 데이터
    @Getter
    @NoArgsConstructor
    public static class ResponseDetail {
        private String flowCode;
        private String createAt;
        private String originalProductStatus;
        private String classificationName;
        private String productName;
        private String storeName;
        private String materialName;
        private String colorName;
        private String setTypeName;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String productSize;
        private String stockNote;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private Integer mainStoneLaborCost;
        private Integer assistanceStoneLaborCost;
        private Integer mainStoneQuantity;
        private Integer assistanceStoneQuantity;
        private String goldWeight;
        private String stoneWeight;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;

        @Builder
        public ResponseDetail(String flowCode, String createAt, String originalProductStatus, String classificationName, String productName, String storeName, String materialName, String colorName, String setTypeName, String mainStoneNote, String assistanceStoneNote, String productSize, String stockNote, Integer productLaborCost, Integer productAddLaborCost, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer mainStoneQuantity, Integer assistanceStoneQuantity, String goldWeight, String stoneWeight, Integer productPurchaseCost, Integer stonePurchaseCost) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.originalProductStatus = originalProductStatus;
            this.classificationName = classificationName;
            this.productName = productName;
            this.storeName = storeName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.setTypeName = setTypeName;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.productSize = productSize;
            this.stockNote = stockNote;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.mainStoneLaborCost = mainStoneLaborCost;
            this.assistanceStoneLaborCost = assistanceStoneLaborCost;
            this.mainStoneQuantity = mainStoneQuantity;
            this.assistanceStoneQuantity = assistanceStoneQuantity;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.productPurchaseCost = productPurchaseCost;
            this.stonePurchaseCost = stonePurchaseCost;
        }
    }

    // 재고값 조회 데이터
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String flowCode;
        private String createAt;
        private String originStatus;
        private String currentStatus;
        private String storeName;
        private String productSize;
        private String stockNote;
        private String materialName;
        private String classificationName;
        private String colorName;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private String assistantStoneName;
        private boolean assistantStone;
        private Integer mainStoneLaborCost;
        private Integer assistanceStoneLaborCost;
        private Integer stoneAddLaborCost;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private Integer mainStoneQuantity;
        private Integer assistanceStoneQuantity;
        private String goldWeight;
        private String stoneWeight;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;

        public void updateStatus(String originStatus, String currentStatus) {
            this.originStatus = originStatus;
            this.currentStatus = currentStatus;
        }

        @QueryProjection
        public Response(String flowCode, String createAt, String originStatus, String currentStatus, String storeName, String productSize, String stockNote, String materialName, String classificationName, String colorName, Integer productLaborCost, Integer productAddLaborCost, String assistantStoneName, boolean assistantStone, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer stoneAddLaborCost, String mainStoneNote, String assistanceStoneNote, Integer mainStoneQuantity, Integer assistanceStoneQuantity, String stoneWeight, String goldWeight, Integer productPurchaseCost, Integer stonePurchaseCost) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.originStatus = originStatus;
            this.currentStatus = currentStatus;
            this.storeName = storeName;
            this.productSize = productSize;
            this.stockNote = stockNote;
            this.materialName = materialName;
            this.classificationName = classificationName;
            this.colorName = colorName;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.assistantStoneName = assistantStoneName;
            this.assistantStone = assistantStone;
            this.mainStoneLaborCost = mainStoneLaborCost;
            this.assistanceStoneLaborCost = assistanceStoneLaborCost;
            this.stoneAddLaborCost = stoneAddLaborCost;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.mainStoneQuantity = mainStoneQuantity;
            this.assistanceStoneQuantity = assistanceStoneQuantity;
            this.stoneWeight = stoneWeight;
            this.goldWeight = goldWeight;
            this.productPurchaseCost = productPurchaseCost;
            this.stonePurchaseCost = stonePurchaseCost;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StockCondition {
        private String startAt;
        private String endAt;
        private OrderDto.OptionCondition optionCondition;
        private OrderDto.SortCondition sortCondition;
        private String orderStatus;

        public StockCondition(String startAt, String endAt, OrderDto.OptionCondition optionCondition, OrderDto.SortCondition sortCondition, String orderStatus) {
            this.startAt = startAt;
            this.endAt = endAt;
            this.optionCondition = optionCondition;
            this.sortCondition = sortCondition;
            this.orderStatus = orderStatus;
        }

        public StockCondition(String startAt, String endAt, String orderStatus) {
            this.startAt = startAt;
            this.endAt = endAt;
            this.orderStatus = orderStatus;
        }
    }

    public static class StockStone {
        private Integer stonePurchaseCost;
        private Integer stoneLaborCost;
        private Integer stoneQuantity;
    }

}
