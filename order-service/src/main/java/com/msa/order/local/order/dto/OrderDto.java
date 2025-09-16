package com.msa.order.local.order.dto;

import com.msa.order.global.dto.StoneDto;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
public class OrderDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        @NotBlank(message = "상점 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 ID는 숫자여야 합니다.")
        private String storeId;

        private String orderNote;

        @NotBlank(message = "공장 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "공장 ID는 숫자여야 합니다.")
        private String factoryId;

        @NotBlank(message = "상품 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상품 ID는 숫자여야 합니다.")
        private String productId;
        private String productSize;
        private boolean isProductWeightSale;
        private Integer productAddLaborCost;

        @NotBlank(message = "재질 값은 필수입니다.")
        @Pattern(regexp = "\\d+", message = "재질 ID는 숫자여야 합니다.")
        private String materialId;
        private String classificationId;
        private String colorId;
        private String setType;
        private String priorityName;

        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;
        private Integer stoneTotalLaborCost;
        private String mainStoneNote;
        private String assistanceStoneNote;

        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneCreateAt;

        private LocalDateTime createAt;
        private String productStatus; // 주문 상태 설정 값 기본은 RECEIPT

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;

    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String orderDate;
        private String orderExpectDate;
        private String flowCode;
        private String storeName;
        private String productName;
        private String materialName;
        private String colorName;
        private String setType;
        private String productSize;
        private Integer stockQuantity;
        private String orderMainStoneNote;
        private String orderAssistanceStoneNote;
        private List<String> stockFlowCodes;
        private String orderNote;
        private String factoryName;
        private String priority;
        private ProductStatus productStatus;
        private OrderStatus orderStatus;
        private String imagePath;

        public static Response from(OrderQueryDto queryDto, String imagePath) {
            Response response = new Response();
            response.orderDate = queryDto.getOrderDate();
            response.orderExpectDate = queryDto.getOrderExpectDate();
            response.flowCode = queryDto.getFlowCode();
            response.storeName = queryDto.getStoreName();
            response.productName = queryDto.getProductName();
            response.materialName = queryDto.getMaterialName();
            response.colorName = queryDto.getColorName();
            response.setType = queryDto.getSetType();
            response.productSize = queryDto.getProductSize();
            response.stockQuantity = queryDto.getStockQuantity();
            response.orderMainStoneNote = queryDto.getOrderMainStoneNote();
            response.orderAssistanceStoneNote = queryDto.getOrderAssistanceStoneNote();
            response.orderNote = queryDto.getOrderNote();
            response.factoryName = queryDto.getFactoryName();
            response.priority = queryDto.getPriority();
            response.productStatus = queryDto.getProductStatus();
            response.orderStatus = queryDto.getOrderStatus();
            response.imagePath = (imagePath != null) ? imagePath : "";
            response.stockFlowCodes = (queryDto.getStockFlowCodes() != null)
                    ? queryDto.getStockFlowCodes()
                    : Collections.emptyList();

            return response;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ResponseDetail {
        private String createAt;
        private String deliveryAt;
        private String flowCode;
        private String storeName;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private Integer productStoneMainLaborCost;
        private Integer productStoneAssistanceLaborCost;
        private Integer productStoneAddLaborCost;
        private Integer productStoneMainQuantity;
        private Integer productStoneAssistanceQuantity;
        private String productName;
        private String classification;
        private String materialName;
        private String colorName;
        private String productSize;
        private String orderNote;
        private String factoryName;
        private String priority;
        private String productStatus;
        private String orderStatus;

        @Builder
        public ResponseDetail(String createAt, String deliveryAt, String flowCode, String storeName, Integer productLaborCost, Integer productAddLaborCost, Integer productStoneMainLaborCost, Integer productStoneAssistanceLaborCost, Integer productStoneAddLaborCost, Integer productStoneMainQuantity, Integer productStoneAssistanceQuantity, String productName, String classification, String materialName, String colorName, String productSize, String orderNote, String factoryName, String priority, String productStatus, String orderStatus) {
            this.createAt = createAt;
            this.deliveryAt = deliveryAt;
            this.flowCode = flowCode;
            this.storeName = storeName;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.productStoneMainLaborCost = productStoneMainLaborCost;
            this.productStoneAssistanceLaborCost = productStoneAssistanceLaborCost;
            this.productStoneAddLaborCost = productStoneAddLaborCost;
            this.productStoneMainQuantity = productStoneMainQuantity;
            this.productStoneAssistanceQuantity = productStoneAssistanceQuantity;
            this.productName = productName;
            this.classification = classification;
            this.materialName = materialName;
            this.colorName = colorName;
            this.productSize = productSize;
            this.orderNote = orderNote;
            this.factoryName = factoryName;
            this.priority = priority;
            this.productStatus = productStatus;
            this.orderStatus = orderStatus;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputCondition {
        private String searchInput;
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCondition {
        private String startAt;
        private String endAt;
        private String factoryName;
        private String storeName;
        private String setTypeName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpectCondition {
        private String endAt;
    }
}
