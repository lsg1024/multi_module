package com.msa.jewelry.order.internal.order.dto;

import com.msa.jewelry.order.internal.global.dto.StatusHistoryDto;
import com.msa.jewelry.order.internal.global.dto.StoneDto;
import com.msa.jewelry.order.internal.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.order.internal.order.entity.order_enum.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.msa.jewelry.order.internal.global.util.DateConversionUtil.LocalDateTimeToLocalDate;

@Schema(description = "주문 DTO 컨테이너 — 주문 생성/조회/검색에 사용하는 Request/Response inner DTO 모음.")
@Getter
@NoArgsConstructor
public class OrderDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "주문 생성/수정 요청 — 매장이 공장에 발주할 때 보내는 페이로드.")
    public static class Request {
        @NotBlank(message = "판매처 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "판매처를 선택해주세요.")
        @Schema(description = "거래처(매장) ID (숫자 문자열)", example = "10")
        private String storeId;
        @Schema(description = "거래처(매장) 이름 (참고용)", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "거래처(매장) 등급 (스냅샷)", example = "A")
        private String storeGrade;
        @Schema(description = "거래처(매장) 수수료(허리, 문자열)", example = "1.50")
        private String storeHarry;

        @NotBlank(message = "공장 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "공장을 선택해주세요.")
        @Schema(description = "제조사(공장) ID", example = "5")
        private String factoryId;
        @Schema(description = "제조사(공장) 이름 (참고용)", example = "삼성공방")
        private String factoryName;
        @Schema(description = "제조사 수수료(허리, 문자열)", example = "1.20")
        private String factoryHarry;

        @NotBlank(message = "상품 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상품을 선택해주세요.")
        @Schema(description = "상품 ID", example = "501")
        private String productId;
        @Schema(description = "상품 이름 (스냅샷)", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "상품 제조사 표기명 (스냅샷)", example = "삼성공방")
        private String productFactoryName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "주문 비고", example = "포장 정중하게")
        private String orderNote;
        @Schema(description = "중량 판매 여부", example = "false")
        private Boolean isProductWeightSale;
        @Schema(description = "상품 매입 비용 (원가)", example = "500000")
        private Integer productPurchaseCost;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;

        @NotBlank(message = "재질 값은 필수입니다.")
        @Pattern(regexp = "\\d+", message = "재질을 선택해주세요")
        @Schema(description = "재질 ID", example = "1")
        private String materialId;
        @Schema(description = "재질 이름 (스냅샷)", example = "18K")
        private String materialName;
        @NotBlank(message = "색상 값은 필수입니다.")
        @Pattern(regexp = "\\d+", message = "색상을 선택해주세요.")
        @Schema(description = "색상 ID", example = "3")
        private String colorId;
        @Schema(description = "색상 이름 (스냅샷)", example = "옐로우골드")
        private String colorName;
        @Schema(description = "분류 ID", example = "2")
        private String classificationId;
        @Schema(description = "분류 이름 (스냅샷)", example = "반지")
        private String classificationName;
        @Schema(description = "세트 타입 ID", example = "4")
        private String setTypeId;
        @Schema(description = "세트 타입 이름 (스냅샷)", example = "단품")
        private String setTypeName;
        @Schema(description = "우선순위 이름 (일반/긴급 등)", example = "일반")
        private String priorityName;

        @Schema(description = "스톤 총 무게 (g)", example = "0.500")
        private BigDecimal stoneWeight;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;

        // 보조석
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantStoneCreateAt;

        @Schema(description = "주문 접수 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String createAt;
        @Schema(description = "출고 예정 일시 (문자열)", example = "2026-05-20T10:00:00")
        private String shippingAt;

        @Valid
        @Schema(description = "주문에 포함된 스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "주문 목록 조회 응답 — 주문 한 건의 요약 정보. 이력/이미지 포함.")
    public static class Response {
        @Schema(description = "주문 접수 일시 (문자열)", example = "2026-05-16 14:30")
        private String createAt;
        @Schema(description = "출고 예정 일시 (문자열)", example = "2026-05-20 10:00")
        private String shippingAt;
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "상품 ID", example = "501")
        private String productId;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "상품 제조사 표기명", example = "삼성공방")
        private String productFactoryName;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "세트 타입 이름", example = "단품")
        private String setType;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "주문에 연결된 재고 수량", example = "1")
        private Integer stockQuantity;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantStoneCreateAt;
        @Schema(description = "연결된 재고 flowCode 목록")
        private List<String> stockFlowCodes;
        @Schema(description = "주문 비고", example = "포장 정중하게")
        private String orderNote;
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "우선순위 이름", example = "일반")
        private String priority;
        @Schema(description = "상품 진행 상태")
        private ProductStatus productStatus;
        @Schema(description = "주문 비즈니스 상태")
        private OrderStatus orderStatus;
        @Schema(description = "상품 이미지 경로", example = "/images/products/501.jpg")
        private String imagePath;
        @Schema(description = "상태 이력 목록 (시계열)")
        private List<StatusHistoryDto> statusHistory;

        public static Response from(OrderQueryDto queryDto, String imagePath, List<StatusHistoryDto> statusHistoryDtos) {
            Response response = new Response();
            response.productId = queryDto.getProductId() != null ? queryDto.getProductId().toString() : null;
            response.createAt = queryDto.getCreateAt();
            response.shippingAt = queryDto.getShippingAt();
            response.flowCode = queryDto.getFlowCode();
            response.storeName = queryDto.getStoreName();
            response.productName = queryDto.getProductName();
            response.productFactoryName = queryDto.getProductFactoryName();
            response.materialName = queryDto.getMaterialName();
            response.colorName = queryDto.getColorName();
            response.setType = queryDto.getSetType();
            response.productSize = queryDto.getProductSize();
            response.stockQuantity = queryDto.getStockQuantity();
            response.mainStoneNote = queryDto.getMainStoneNote();
            response.assistanceStoneNote = queryDto.getAssistanceStoneNote();
            response.assistantStone = queryDto.isAssistantStone();
            response.assistantStoneName = queryDto.getAssistantStoneName();
            response.assistantStoneCreateAt = queryDto.getAssistantStoneCreateAt();
            response.orderNote = queryDto.getOrderNote();
            response.factoryName = queryDto.getFactoryName();
            response.priority = queryDto.getPriority();
            response.productStatus = queryDto.getProductStatus();
            response.orderStatus = queryDto.getOrderStatus();
            response.imagePath = (imagePath != null) ? imagePath : "";
            response.stockFlowCodes = (queryDto.getStockFlowCodes() != null)
                    ? queryDto.getStockFlowCodes()
                    : Collections.emptyList();
            response.statusHistory = statusHistoryDtos;
            return response;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "주문 상세 응답 — 한 건의 주문에 대한 모든 필드와 스톤 목록.")
    public static class ResponseDetail {
        @Schema(description = "주문 접수 일시 (문자열)", example = "2026-05-16 14:30")
        private String createAt;
        @Schema(description = "출고 예정 일시 (문자열)", example = "2026-05-20 10:00")
        private String shippingAt;
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "거래처(매장) ID", example = "10")
        private String storeId;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "거래처 수수료(허리, 문자열)", example = "1.50")
        private String storeHarry;
        @Schema(description = "거래처 등급 (스냅샷)", example = "A")
        private String storeGrade;
        @Schema(description = "제조사 ID", example = "5")
        private String factoryId;
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "상품 ID", example = "501")
        private String productId;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "상품 제조사 표기명 (스냅샷)", example = "삼성공방")
        private String productFactoryName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;
        @Schema(description = "분류 ID", example = "2")
        private String classificationId;
        @Schema(description = "분류 이름", example = "반지")
        private String classificationName;
        @Schema(description = "재질 ID", example = "1")
        private String materialId;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "색상 ID", example = "3")
        private String colorId;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "세트 타입 ID", example = "4")
        private String setTypeId;
        @Schema(description = "세트 타입 이름", example = "단품")
        private String setTypeName;
        @Schema(description = "주문 비고", example = "포장 정중하게")
        private String orderNote;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "우선순위 이름", example = "일반")
        private String priority;
        @Schema(description = "상품 진행 상태 (문자열)", example = "WAITING")
        private String productStatus;
        @Schema(description = "주문 비즈니스 상태 (문자열)", example = "WAIT")
        private String orderStatus;
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일자", example = "2026-05-16")
        private LocalDate assistantStoneCreateAt;
        @Schema(description = "주문 스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
        @Schema(description = "추가 스톤 공임 (문자열)", example = "30000")
        private String stoneAddLaborCost;

        @Builder
        public ResponseDetail(String createAt, String shippingAt, String flowCode, String storeId, String storeName, String storeHarry, String storeGrade, String factoryId, String productId, String productName, String productFactoryName, Integer productLaborCost, Integer productAddLaborCost, String stoneWeight, String classificationId, String classificationName, String materialName, String colorName, String setTypeName, String productSize, String orderNote, String factoryName, String goldWeight, String materialId, String colorId, String setTypeId, String mainStoneNote, String assistanceStoneNote, String priority, String productStatus, String orderStatus, boolean assistantStone, String assistantStoneId, String assistantStoneName, LocalDateTime assistantStoneCreateAt, List<StoneDto.StoneInfo> stoneInfos, String stoneAddLaborCost) {
            this.createAt = createAt;
            this.shippingAt = shippingAt;
            this.flowCode = flowCode;
            this.storeId = storeId;
            this.storeName = storeName;
            this.storeHarry = storeHarry;
            this.storeGrade = storeGrade;
            this.factoryId = factoryId;
            this.productId = productId;
            this.productName = productName;
            this.productFactoryName = productFactoryName;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.stoneWeight = stoneWeight;
            this.classificationId = classificationId;
            this.classificationName = classificationName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.setTypeName = setTypeName;
            this.productSize = productSize;
            this.orderNote = orderNote;
            this.factoryName = factoryName;
            this.goldWeight = goldWeight;
            this.materialId = materialId;
            this.colorId = colorId;
            this.setTypeId = setTypeId;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.priority = priority;
            this.productStatus = productStatus;
            this.orderStatus = orderStatus;
            this.assistantStone = assistantStone;
            this.assistantStoneId = assistantStoneId;
            this.assistantStoneName = assistantStoneName;
            this.assistantStoneCreateAt = LocalDateTimeToLocalDate(assistantStoneCreateAt);
            this.stoneInfos = stoneInfos;
            this.stoneAddLaborCost = stoneAddLaborCost;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 응답에 포함되는 주문 정보 — 재고 화면에서 주문 정보를 함께 보여줄 때 사용.")
    public static class StockResponse {
        @Schema(description = "주문 상세")
        private ResponseDetail orderResponse;
        @Schema(description = "거래 당시 거래처 수수료(허리) 스냅샷", example = "1.50")
        private String storeHarry;

        @Builder
        public StockResponse(ResponseDetail orderResponse, String storeHarry) {
            this.orderResponse = orderResponse;
            this.storeHarry = storeHarry;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "검색어 입력 조건 — 단일 검색어와 검색 대상 필드.")
    public static class InputCondition {
        @Schema(description = "검색어", example = "다이아")
        private String searchInput;
        @Schema(description = "검색 대상 필드 (productName/storeName 등)", example = "productName")
        private String searchField;

        public InputCondition(String searchInput) {
            this.searchInput = searchInput;
        }
    }
    @Getter
    @NoArgsConstructor
    @Schema(description = "주문 목록 검색 조건 — 기간/필터/정렬/상태.")
    public static class OrderCondition {
        @Schema(description = "시작 일자 (yyyy-MM-dd)", example = "2026-05-01")
        private String startAt;
        @Schema(description = "종료 일자 (yyyy-MM-dd)", example = "2026-05-31")
        private String endAt;
        @Schema(description = "필터 옵션")
        private OptionCondition optionCondition;
        @Schema(description = "정렬 조건")
        private SortCondition sortCondition;
        @Schema(description = "주문 비즈니스 상태", example = "WAIT")
        private String orderStatus;

        public OrderCondition(String startAt, String endAt, OptionCondition optionCondition, SortCondition sortCondition, String orderStatus) {
            this.startAt = startAt;
            this.endAt = endAt;
            this.optionCondition = optionCondition;
            this.sortCondition = sortCondition;
            this.orderStatus = orderStatus;
        }

        public OrderCondition(String startAt, String endAt, OptionCondition optionCondition, String orderStatus) {
            this.startAt = startAt;
            this.endAt = endAt;
            this.optionCondition = optionCondition;
            this.orderStatus = orderStatus;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "출고 예정일 기반 검색 조건.")
    public static class ExpectCondition {
        @Schema(description = "종료 일자 (yyyy-MM-dd)", example = "2026-05-31")
        private String endAt;
        @Schema(description = "필터 옵션")
        private OptionCondition optionCondition;
        @Schema(description = "정렬 조건")
        private SortCondition sortCondition;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "주문 검색 필터 옵션 — 제조사/거래처/재질/색상 등 필터 값.")
    public static class OptionCondition {
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "세트 타입 이름", example = "단품")
        private String setTypeName;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "분류 이름", example = "반지")
        private String classificationName;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "정렬 조건 — 정렬 필드 + 방향.")
    public static class SortCondition {
        @Schema(description = "정렬 필드명", example = "createAt")
        private String sortField;
        @Schema(description = "정렬 방향 (asc/desc)", example = "desc")
        private String sort;
    }

}
