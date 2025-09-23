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
import java.time.OffsetDateTime;
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
        private String colorId;
        private String classificationName;
        private String setTypeName;
        private String priorityName;

        private BigDecimal stoneWeight;
        private String mainStoneNote;
        private String assistanceStoneNote;

        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneCreateAt;

        private String createAt;
        private String shippingAt;
        private String productStatus; // 주문 상태 설정 값 기본은 RECEIPT

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;

        @Override
        public String toString() {
            return "Request{" +
                    "storeId='" + storeId + '\'' +
                    ", orderNote='" + orderNote + '\'' +
                    ", factoryId='" + factoryId + '\'' +
                    ", productId='" + productId + '\'' +
                    ", productSize='" + productSize + '\'' +
                    ", isProductWeightSale=" + isProductWeightSale +
                    ", productAddLaborCost=" + productAddLaborCost +
                    ", materialId='" + materialId + '\'' +
                    ", colorId='" + colorId + '\'' +
                    ", classificationName='" + classificationName + '\'' +
                    ", setTypeName='" + setTypeName + '\'' +
                    ", priorityName='" + priorityName + '\'' +
                    ", stoneWeight=" + stoneWeight +
                    ", mainStoneNote='" + mainStoneNote + '\'' +
                    ", assistanceStoneNote='" + assistanceStoneNote + '\'' +
                    ", assistantStone=" + assistantStone +
                    ", assistantStoneId='" + assistantStoneId + '\'' +
                    ", assistantStoneCreateAt='" + assistantStoneCreateAt + '\'' +
                    ", createAt='" + createAt + '\'' +
                    ", shippingAt='" + shippingAt + '\'' +
                    ", productStatus='" + productStatus + '\'' +
                    ", stoneInfos=" + stoneInfos +
                    '}';
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String createAt;
        private String shippingAt;
        private String flowCode;
        private String storeName;
        private String productName;
        private String materialName;
        private String colorName;
        private String setType;
        private String productSize;
        private Integer stockQuantity;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private List<String> stockFlowCodes;
        private String orderNote;
        private String factoryName;
        private String priority;
        private ProductStatus productStatus;
        private OrderStatus orderStatus;
        private String imagePath;

        public static Response from(OrderQueryDto queryDto, String imagePath) {
            Response response = new Response();
            response.createAt = queryDto.getCreateAt();
            response.shippingAt = queryDto.getShippingAt();
            response.flowCode = queryDto.getFlowCode();
            response.storeName = queryDto.getStoreName();
            response.productName = queryDto.getProductName();
            response.materialName = queryDto.getMaterialName();
            response.colorName = queryDto.getColorName();
            response.setType = queryDto.getSetType();
            response.productSize = queryDto.getProductSize();
            response.stockQuantity = queryDto.getStockQuantity();
            response.mainStoneNote = queryDto.getMainStoneNote();
            response.assistanceStoneNote = queryDto.getAssistanceStoneNote();
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
        private String shippingAt;
        private String flowCode;
        private String storeId;
        private String storeName;
        private String factoryId;
        private String factoryName;
        private String productId;
        private String productName;
        private String classification;
        private String materialName;
        private String colorName;
        private String setType;
        private String productSize;
        private String orderNote;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String priority;
        private String productStatus;
        private String orderStatus;
        private boolean assistantStone;
        private String assistantStoneName;
        private OffsetDateTime assistantStoneCreateAt;
        private List<StoneDto.StoneResponse> stoneInfos;

        @Builder
        public ResponseDetail(String createAt, String shippingAt, String flowCode, String storeId, String storeName, String factoryId, String productId, String productName, String classification, String materialName, String colorName, String setType, String productSize, String orderNote, String factoryName, String mainStoneNote, String assistanceStoneNote, String priority, String productStatus, String orderStatus, boolean assistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, List<StoneDto.StoneResponse> stoneInfos) {
            this.createAt = createAt;
            this.shippingAt = shippingAt;
            this.flowCode = flowCode;
            this.storeId = storeId;
            this.storeName = storeName;
            this.factoryId = factoryId;
            this.productId = productId;
            this.productName = productName;
            this.classification = classification;
            this.materialName = materialName;
            this.colorName = colorName;
            this.setType = setType;
            this.productSize = productSize;
            this.orderNote = orderNote;
            this.factoryName = factoryName;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.priority = priority;
            this.productStatus = productStatus;
            this.orderStatus = orderStatus;
            this.assistantStone = assistantStone;
            this.assistantStoneName = assistantStoneName;
            this.assistantStoneCreateAt = assistantStoneCreateAt;
            this.stoneInfos = stoneInfos;
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
        private OptionCondition optionCondition;
        private String orderStatus;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpectCondition {
        private String endAt;
        private OptionCondition optionCondition;
        private String orderStatus;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionCondition {
        private String factoryName;
        private String storeName;
        private String setTypeName;
    }
}
