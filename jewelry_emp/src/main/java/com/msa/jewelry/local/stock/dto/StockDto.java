package com.msa.jewelry.local.stock.dto;

import com.msa.jewelry.local.order.dto.StatusHistoryDto;
import com.msa.jewelry.local.order.dto.StoneDto;
import com.msa.jewelry.local.order.dto.OrderDto;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.querydsl.core.annotations.QueryProjection;
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
import java.util.List;

import static com.msa.jewelry.global.util.DateConversionUtil.LocalDateTimeToLocalDate;

@Schema(description = "재고(Stock) DTO 컨테이너 — 재고 등록/대여/업데이트/조회 등의 inner DTO 모음.")
public class StockDto {

    // 직접 재고 등록 시
    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 직접 등록 요청 — 주문 없이 매장이 직접 재고를 등록할 때.")
    public static class Request {
        @NotBlank(message = "판매처 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "판매처를 선택해주세요.")
        @Schema(description = "거래처(매장) ID", example = "10")
        private String storeId;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "거래처 등급 (스냅샷)", example = "A")
        private String storeGrade;
        @Schema(description = "거래처 수수료(허리, 문자열)", example = "1.50")
        private String storeHarry;

        @NotBlank(message = "공장 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "공장을 선택해주세요.")
        @Schema(description = "제조사(공장) ID", example = "5")
        private String factoryId;
        @Schema(description = "제조사(공장) 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "제조사 수수료(허리, 문자열)", example = "1.20")
        private String factoryHarry;

        @NotBlank(message = "상품 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상품을 선택해주세요.")
        @Schema(description = "상품 ID", example = "501")
        private String productId;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "상품 제조사 표기명", example = "삼성공방")
        private String productFactoryName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "재고 비고", example = "샘플로 들어온 제품")
        private String stockNote;
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
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @NotBlank(message = "색상 값은 필수입니다.")
        @Pattern(regexp = "\\d+", message = "색상을 선택해주세요.")
        @Schema(description = "색상 ID", example = "3")
        private String colorId;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "분류 ID", example = "2")
        private String classificationId;
        @Schema(description = "분류 이름", example = "반지")
        private String classificationName;
        @Schema(description = "세트 타입 ID", example = "4")
        private String setTypeId;
        @Schema(description = "세트 타입 이름", example = "단품")
        private String setTypeName;
        @Schema(description = "우선순위 이름", example = "일반")
        private String priorityName;

        @Schema(description = "금 무게 (g)", example = "3.250")
        private BigDecimal goldWeight;
        @Schema(description = "스톤 무게 (g)", example = "0.500")
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

        @Valid
        @Schema(description = "스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 부분 업데이트 요청 — 사이즈/공임/스톤 정보 등 갱신.")
    public static class stockRequest {
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "중량 판매 여부", example = "false")
        private Boolean isProductWeightSale;
        @Schema(description = "추가 상품 공임", example = "20000")
        private Integer addProductLaborCost;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "재고 비고", example = "샘플로 들어온 제품")
        private String stockNote;
        // 보조석
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantStoneCreateAt;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;

        @Valid
        @Schema(description = "스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 대여(rental) 요청 — 매장에 빌려줄 때 사용. 거래처 변경도 같이 가능.")
    public static class StockRentalRequest {
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "재고 비고", example = "대여 - 5월 말 반납")
        private String stockNote;
        @Schema(description = "중량 판매 여부", example = "false")
        private Boolean isProductWeightSale;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
        // 대여 시 거래처 변경 가능
        @Schema(description = "대여처 거래처 ID (변경 시)", example = "11")
        private String storeId;
        @Schema(description = "대여처 거래처 이름", example = "XYZ 보석상")
        private String storeName;
        @Schema(description = "대여처 거래처 등급", example = "B")
        private String storeGrade;
        @Schema(description = "대여처 수수료(허리, 문자열)", example = "1.60")
        private String storeHarry;
        @Valid
        @Schema(description = "스톤 정보 목록 — 개당 알수는 직접 수정 불가")
        private List<StoneDto.StoneInfo> stoneInfos; // 개당 알수는 직접 수정 불가
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "주문 완료 시 재고 등록 요청 — 공장 출고가 끝나서 재고로 잡을 때.")
    public static class StockRegisterRequest {
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "재질 ID", example = "1")
        private String materialId;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "색상 ID", example = "3")
        private String colorId;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "중량 판매 여부", example = "false")
        private boolean isProductWeightSale;
        @Schema(description = "상품 매입 비용", example = "500000")
        private Integer productPurchaseCost;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "거래처 수수료(허리, 문자열)", example = "1.50")
        private String storeHarry;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;
        @Schema(description = "주문 비고", example = "포장 정중하게")
        private String orderNote;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantStoneCreateAt;
        @Schema(description = "스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
    }

    // 재고값 상세 조회 데이터
    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 상세 응답 — 한 건의 재고 + 스톤 목록.")
    public static class ResponseDetail {
        @Schema(description = "등록 일시 (문자열)", example = "2026-05-01 10:00")
        private String createAt;
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "원본 상품 상태 (예: 일반/주문)", example = "일반")
        private String originalProductStatus;
        @Schema(description = "거래처(매장) ID", example = "10")
        private String storeId;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "거래처 등급", example = "A")
        private String storeGrade;
        @Schema(description = "거래처 수수료(허리, 문자열)", example = "1.50")
        private String storeHarry;
        @Schema(description = "제조사 ID", example = "5")
        private String factoryId;
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "상품 ID", example = "501")
        private String productId;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "색상 ID", example = "3")
        private String colorId;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "재질 ID", example = "1")
        private String materialId;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "비고", example = "샘플로 들어온 제품")
        private String note;
        @Schema(description = "중량 판매 여부", example = "false")
        private boolean isProductWeightSale;
        @Schema(description = "상품 매입 비용", example = "500000")
        private Integer productPurchaseCost;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일자", example = "2026-05-16")
        private LocalDate assistantStoneCreateAt;
        @Schema(description = "스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;

        @Builder
        public ResponseDetail(String flowCode, String createAt, String originalProductStatus, String storeId, String productName, String storeName, String storeGrade, String storeHarry, String factoryId, String factoryName, String colorId, String materialId, String materialName, String colorName, String productId, String mainStoneNote, String assistanceStoneNote, String productSize, String note, boolean isProductWeightSale, Integer productLaborCost, Integer productAddLaborCost, String assistantStoneId, boolean assistantStone, LocalDateTime assistantStoneCreateAt, Integer stoneAddLaborCost, String goldWeight, String stoneWeight, Integer productPurchaseCost, String assistantStoneName, List<StoneDto.StoneInfo> stoneInfos) {
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
            this.assistantStoneCreateAt = LocalDateTimeToLocalDate(assistantStoneCreateAt);
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
    @Schema(description = "재고 업데이트 요청 — 사이즈/공임/비고/거래처 등 부분 변경.")
    public static class updateStockRequest {
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "중량 판매 여부", example = "false")
        private boolean isProductWeightSale;
        @Schema(description = "상품 매입 비용", example = "500000")
        private Integer productPurchaseCost;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "재고 비고")
        private String stockNote;
        @Schema(description = "거래처 수수료(허리, 문자열)", example = "1.50")
        private String storeHarry;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;
        @Schema(description = "메인 스톤 비고")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고")
        private String assistanceStoneNote;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantStoneCreateAt;
        @Schema(description = "스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
        @Schema(description = "총 스톤 매입 비용", example = "150000")
        private Integer totalStonePurchaseCost;
        @Schema(description = "거래처(매장) ID", example = "10")
        private String storeId;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "거래처 등급", example = "A")
        private String storeGrade;
        @Schema(description = "제조사 ID", example = "5")
        private String factoryId;
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "제조사 수수료(허리, 문자열)", example = "1.20")
        private String factoryHarry;
    }

    // 재고값 조회 데이터
    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 목록 응답 — 한 건의 재고 요약 (Querydsl projection).")
    public static class Response {
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "등록 일시 (문자열)", example = "2026-05-01 10:00")
        private String createAt;
        @Schema(description = "출고 예정 일시 (문자열)", example = "2026-05-20 10:00")
        private String shippingAt;
        @Schema(description = "원본 상태 (예: 일반/주문)", example = "일반")
        private String originStatus;
        @Schema(description = "현재 재고 비즈니스 상태", example = "STOCK")
        private String currentStatus;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "상품 ID", example = "501")
        private String productId;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "상품 제조사 표기명", example = "삼성공방")
        private String productFactoryName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "재고 비고")
        private String stockNote;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "분류 이름", example = "반지")
        private String classificationName;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "상품 매입 비용", example = "500000")
        private Integer productPurchaseCost;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "메인 스톤 공임", example = "200000")
        private Integer mainStoneLaborCost;
        @Schema(description = "보조 스톤 공임", example = "50000")
        private Integer assistanceStoneLaborCost;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "메인 스톤 개수", example = "1")
        private Integer mainStoneQuantity;
        @Schema(description = "보조 스톤 개수", example = "12")
        private Integer assistanceStoneQuantity;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;
        @Schema(description = "총 스톤 매입 비용", example = "150000")
        private Integer stonePurchaseCost;
        @Schema(description = "상태 이력 목록 (시계열)")
        private List<StatusHistoryDto> statusHistory;

        public void updateHistory(List<StatusHistoryDto> statusHistoryDtos) {
            this.statusHistory = statusHistoryDtos;
        }
        public void updateStatus(String originStatus, String currentStatus) {
            this.originStatus = originStatus;
            this.currentStatus = currentStatus;
        }

        @QueryProjection
        public Response(String flowCode, String createAt, String shippingAt, String originStatus, String currentStatus, String storeName, String factoryName, String productId, String productName, String productFactoryName, String productSize, String stockNote, String materialName, String classificationName, String colorName, Integer productLaborCost, Integer productAddLaborCost, String assistantStoneName, boolean assistantStone, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer stoneAddLaborCost, String mainStoneNote, String assistanceStoneNote, Integer mainStoneQuantity, Integer assistanceStoneQuantity, String stoneWeight, String goldWeight, Integer productPurchaseCost, Integer stonePurchaseCost) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.shippingAt = shippingAt;
            this.originStatus = originStatus;
            this.currentStatus = currentStatus;
            this.storeName = storeName;
            this.factoryName = factoryName;
            this.productId = productId;
            this.productName = productName;
            this.productFactoryName = productFactoryName;
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
    @Schema(description = "재고 목록 검색 조건 — 기간/옵션/정렬/상태.")
    public static class StockCondition {
        @Schema(description = "시작 일자 (yyyy-MM-dd)", example = "2026-05-01")
        private String startAt;
        @Schema(description = "종료 일자 (yyyy-MM-dd)", example = "2026-05-31")
        private String endAt;
        @Schema(description = "필터 옵션")
        private OrderDto.OptionCondition optionCondition;
        @Schema(description = "정렬 조건")
        private OrderDto.SortCondition sortCondition;
        @Schema(description = "재고 비즈니스 상태", example = "STOCK")
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
    @Schema(description = "재고 이력 검색 조건 — 기간/단계(phase)/필터/정렬.")
    public static class HistoryCondition {
        @Schema(description = "시작 일자 (yyyy-MM-dd)", example = "2026-05-01")
        private String startAt;
        @Schema(description = "종료 일자 (yyyy-MM-dd)", example = "2026-05-31")
        private String endAt;
        @Schema(description = "비즈니스 단계 필터")
        private BusinessPhase phase;
        @Schema(description = "필터 옵션")
        private OrderDto.OptionCondition optionCondition;
        @Schema(description = "정렬 조건")
        private OrderDto.SortCondition sortCondition;
    }

}
