package com.msa.order.local.domain.order.dto;

import com.msa.order.global.exception.EnumValue;
import com.msa.order.local.domain.order.entity.OrderStatus;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        private Integer productAddLaborCost;

        @NotBlank(message = "재질 값은 필수입니다.")
        @Pattern(regexp = "\\d+", message = "재질 ID는 숫자여야 합니다.")
        private String materialId;
        private String classificationId;
        private String colorId;
        private String priorityName;
        private String createAt;

        @EnumValue(enumClass = OrderStatus.class)
        private String orderStatus;

        @Valid
        private List<ProductDetailDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String createAt;
        private String orderId;
        private String orderCode;
        private String storeName;
        private String productName;
        private String materialName;
        private String colorName;
        private String productSize;
        // 재고 기능 구현 후 재고에 동일 제품 여부 체크
        private String orderNote;
        private String factoryName;
        private String priority;
        private String orderStatus;

        @QueryProjection
        public Response(String orderId, String orderCode, String storeName, String productName, String productSize, String orderNote, String factoryName, String materialName, String colorName, String priority, String createAt, String orderStatus) {
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.storeName = storeName;
            this.productName = productName;
            this.productSize = productSize;
            this.orderNote = orderNote;
            this.factoryName = factoryName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.priority = priority;
            this.createAt = createAt;
            this.orderStatus = orderStatus;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ResponseDetail {
        private String createAt;
        private String deliveryAt;
        private String orderId;
        private String orderCode;
        private String storeName;
        private String productLaborCost;
        private String productAddLaborCost;
        private String productStoneMainLaborCost;
        private String productStoneAssistanceLaborCost;
        private String productStoneMainQuantity;
        private String productStoneAssistanceQuantity;
        private String productName;
        private String classification;
        private String materialName;
        private String colorName;
        private String productSize;
        private String orderNote;
        private String factoryName;
        private String priority;
        private String orderStatus;

        @QueryProjection
        public ResponseDetail(String createAt, String deliveryAt, String orderId, String orderCode, String storeName, String productLaborCost, String productAddLaborCost, String productStoneMainLaborCost, String productStoneAssistanceLaborCost, String productStoneMainQuantity, String productStoneAssistanceQuantity, String productName, String classification, String materialName, String colorName, String productSize, String orderNote, String factoryName, String priority, String orderStatus) {
            this.createAt = createAt;
            this.deliveryAt = deliveryAt;
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.storeName = storeName;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.productStoneMainLaborCost = productStoneMainLaborCost;
            this.productStoneAssistanceLaborCost = productStoneAssistanceLaborCost;
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
            this.orderStatus = orderStatus;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String searchInput;
        private String startAt;
        private String endAt;
    }
}
