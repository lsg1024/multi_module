package com.msa.order.local.stock.dto;

import com.msa.order.global.dto.StoneDto;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.msa.order.global.util.DateConversionUtil.OffsetDateTimeToLocalDate;

public class StockDto {

    // 직접 재고 등록 시
    @Getter
    @NoArgsConstructor
    public static class Request {
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
        private Integer productLaborCost;
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
        private String priorityName;

        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;
        private String mainStoneNote;
        private String assistanceStoneNote;

        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneName;
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
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneName;
        private String assistantStoneCreateAt;
        private String goldWeight;
        private String stoneWeight;

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    public static class StockRentalRequest {
        private String productSize;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        private Boolean isProductWeightSale;
        private String goldWeight;
        private String stoneWeight;
        private Integer productAddLaborCost;
        private Integer stoneAddLaborCost;
        @Valid
        private List<StoneDto.StoneInfo> stoneInfos; // 개당 알수는 직접 수정 불가
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
        private String createAt;
        private String flowCode;
        private String originalProductStatus;
        private String storeId;
        private String storeName;
        private String storeGrade;
        private String storeHarry;
        private String factoryId;
        private String factoryName;
        private String productId;
        private String productName;
        private String productSize;
        private String colorId;
        private String colorName;
        private String materialId;
        private String materialName;
        private String note;
        private boolean isProductWeightSale;
        private Integer productPurchaseCost;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private String goldWeight;
        private String stoneWeight;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneName;
        private LocalDate assistantStoneCreateAt;
        private List<StoneDto.StoneInfo> stoneInfos;
        private Integer stoneAddLaborCost;

        @Builder
        public ResponseDetail(String flowCode, String createAt, String originalProductStatus, String storeId, String productName, String storeName, String storeGrade, String storeHarry, String factoryId, String factoryName, String colorId, String materialId, String materialName, String colorName, String productId, String mainStoneNote, String assistanceStoneNote, String productSize, String note, boolean isProductWeightSale, Integer productLaborCost, Integer productAddLaborCost, String assistantStoneId, boolean assistantStone, OffsetDateTime assistantStoneCreateAt, Integer stoneAddLaborCost, String goldWeight, String stoneWeight, Integer productPurchaseCost, String assistantStoneName, List<StoneDto.StoneInfo> stoneInfos) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.originalProductStatus = originalProductStatus;
            this.storeId = storeId;
            this.productName = productName;
            this.storeName = storeName;
            this.storeGrade = storeGrade;
            this.storeHarry = storeHarry;
            this.factoryId = factoryId;
            this.factoryName = factoryName;
            this.colorId = colorId;
            this.materialId = materialId;
            this.materialName = materialName;
            this.colorName = colorName;
            this.productId = productId;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.productSize = productSize;
            this.note = note;
            this.isProductWeightSale = isProductWeightSale;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.assistantStoneId = assistantStoneId;
            this.assistantStone = assistantStone;
            this.assistantStoneCreateAt = OffsetDateTimeToLocalDate(assistantStoneCreateAt);
            this.stoneAddLaborCost = stoneAddLaborCost;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.productPurchaseCost = productPurchaseCost;
            this.assistantStoneName = assistantStoneName;
            this.stoneInfos = stoneInfos;
        }
    }

    // 재고 업데이트
    @Getter
    @NoArgsConstructor
    public static class updateStockRequest {
        private String productSize;
        private boolean isProductWeightSale;
        private Integer productPurchaseCost;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private String stockNote;
        private String storeHarry;
        private String goldWeight;
        private String stoneWeight;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String assistantStoneId;
        private boolean assistantStone;
        private String assistantStoneName;
        private String assistantStoneCreateAt;
        private List<StoneDto.StoneInfo> stoneInfos;
        private Integer stoneAddLaborCost;
        private Integer totalStonePurchaseCost;
    }

    // 재고값 조회 데이터
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String flowCode;
        private String createAt;
        private String shippingAt;
        private String originStatus;
        private String currentStatus;
        private String storeName;
        private String productId;
        private String productName;
        private String productSize;
        private String stockNote;
        private String materialName;
        private String classificationName;
        private String colorName;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private Integer productPurchaseCost;
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
        private Integer stonePurchaseCost;

        public void updateStatus(String originStatus, String currentStatus) {
            this.originStatus = originStatus;
            this.currentStatus = currentStatus;
        }

        @QueryProjection
        public Response(String flowCode, String createAt, String shippingAt, String originStatus, String currentStatus, String storeName, String productId, String productName, String productSize, String stockNote, String materialName, String classificationName, String colorName, Integer productLaborCost, Integer productAddLaborCost, String assistantStoneName, boolean assistantStone, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer stoneAddLaborCost, String mainStoneNote, String assistanceStoneNote, Integer mainStoneQuantity, Integer assistanceStoneQuantity, String stoneWeight, String goldWeight, Integer productPurchaseCost, Integer stonePurchaseCost) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.shippingAt = shippingAt;
            this.originStatus = originStatus;
            this.currentStatus = currentStatus;
            this.storeName = storeName;
            this.productId = productId;
            this.productName = productName;
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

    @Getter
    @AllArgsConstructor
    public static class HistoryCondition {
        private String startAt;
        private String endAt;
        private BusinessPhase phase;
        private OrderDto.OptionCondition optionCondition;
        private OrderDto.SortCondition sortCondition;
    }

}
