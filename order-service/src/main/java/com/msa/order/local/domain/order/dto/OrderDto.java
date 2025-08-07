package com.msa.order.local.domain.order.dto;

import com.msa.order.global.exception.EnumValue;
import com.msa.order.local.domain.order.entity.OrderStatus;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
        private String orderId;
        private String orderCode;
        private String storeName;
        private String productName;
        private String productSize;
        private String productLaborCost;
        private String orderNote;
        private String factoryName;
        private String materialName;
        private String colorName;
        private Integer quantity;
        private Integer orderMainStoneQuantity;
        private Integer orderAuxiliaryStoneQuantity;
        private String priority;
        private String createAt;

        @QueryProjection
        public Response(String orderId, String orderCode, String storeName, String productName, String productSize, String productLaborCost, String orderNote, String factoryName, String materialName, String colorName, Integer quantity, Integer orderMainStoneQuantity, Integer orderAuxiliaryStoneQuantity, String priority, String createAt) {
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.storeName = storeName;
            this.productName = productName;
            this.productSize = productSize;
            this.productLaborCost = productLaborCost;
            this.orderNote = orderNote;
            this.factoryName = factoryName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.quantity = quantity;
            this.orderMainStoneQuantity = orderMainStoneQuantity;
            this.orderAuxiliaryStoneQuantity = orderAuxiliaryStoneQuantity;
            this.priority = priority;
            this.createAt = createAt;
        }
    }
}
