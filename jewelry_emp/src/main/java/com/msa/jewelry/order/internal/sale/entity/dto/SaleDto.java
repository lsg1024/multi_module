package com.msa.jewelry.order.internal.sale.entity.dto;

import com.msa.jewelry.order.internal.global.dto.StoneDto;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.msa.jewelry.order.internal.global.util.DateConversionUtil.LocalDateTimeToLocalDate;

@Schema(description = "판매(Sale) DTO 컨테이너 — 판매 라인/응답/검색/결제 등 inner DTO 모음.")
public class SaleDto {
    @Getter
    @NoArgsConstructor
    @Schema(description = "판매 라인 응답 — 한 라인(SaleItem)의 거래 정보와 스톤 정보.")
    public static class Response {
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private Long flowCode;
        @Schema(description = "거래 일시 (문자열)", example = "2026-05-16 14:30")
        private String createAt;
        @Schema(description = "거래 유형 표시명 (판매/반품/결제 등)", example = "판매")
        private String saleType;
        @Schema(description = "거래처 ID (문자열)", example = "10")
        private String id;
        @Schema(description = "거래처 이름", example = "ABC 보석상")
        private String name;
        @Schema(description = "거래처 등급", example = "A")
        private String grade;
        @Schema(description = "거래 당시 수수료(허리)", example = "1.50")
        private BigDecimal harry;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
        private String mainStoneNote;
        @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
        private String assistanceStoneNote;
        @Schema(description = "비고", example = "현금 영수증 발행")
        private String note;
        @Schema(description = "판매 세션 금 시세 (원/g)", example = "85000")
        private Integer accountGoldPrice;
        @Schema(description = "금 무게 (g)", example = "3.250")
        private BigDecimal goldWeight;
        @Schema(description = "스톤 무게 (g)", example = "0.500")
        private BigDecimal stoneWeight;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer addStoneLaborCost;
        @Schema(description = "보조석 포함 여부", example = "true")
        private Boolean assistantStone;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantStoneCreateAt;
        @Schema(description = "스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;

        @Builder
        public Response(Long flowCode, String createAt, String saleType, String id, String name, String grade, BigDecimal harry, String productName, String productSize, String materialName, String colorName, String mainStoneNote, String assistanceStoneNote, String note, Integer accountGoldPrice, BigDecimal goldWeight, BigDecimal stoneWeight, Integer productLaborCost, Integer productAddLaborCost, Integer addStoneLaborCost, Boolean assistantStone, String assistantStoneId, String assistantStoneName, String assistantStoneCreateAt, List<StoneDto.StoneInfo> stoneInfos) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.saleType = saleType;
            this.id = id;
            this.name = name;
            this.grade = grade;
            this.harry = harry;
            this.productName = productName;
            this.productSize = productSize;
            this.materialName = materialName;
            this.colorName = colorName;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.note = note;
            this.accountGoldPrice = accountGoldPrice;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.addStoneLaborCost = addStoneLaborCost;
            this.assistantStone = assistantStone;
            this.assistantStoneId = assistantStoneId;
            this.assistantStoneName = assistantStoneName;
            this.assistantStoneCreateAt = assistantStoneCreateAt;
            this.stoneInfos = stoneInfos;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "판매/결제 생성 요청 — 거래처 + 거래 유형 + 금액/순금.")
    public static class Request {
        @Schema(description = "판매 세션 금 시세 (원/g)", example = "85000")
        private Integer accountGoldPrice;
        @Schema(description = "거래처 ID", example = "10")
        private Long id;
        @Schema(description = "거래처 이름 (스냅샷)", example = "ABC 보석상")
        private String name;
        @Schema(description = "거래 당시 수수료(허리)", example = "1.50")
        private BigDecimal harry;
        @Schema(description = "거래처 등급", example = "A")
        private String grade;
        @NotBlank(message = "필수 입력값 입니다.")
        @Schema(description = "거래 유형 (SALE/PURCHASE/RETURN/PAYMENT 등)", example = "SALE")
        private String orderStatus;
        @Schema(description = "금 재질 (예: 18K)", example = "18K")
        private String material;
        @Schema(description = "비고", example = "현금 영수증 발행")
        private String note;
        @Schema(description = "납입/판매 금 총중량 (g 문자열) — 재질로 순금 환산", example = "3.250")
        private String goldWeight; // 총중량으로 재질로 순금 값 계산
        @Schema(description = "현금 결제 금액 (원)", example = "100000")
        private Integer payAmount;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "판매 라인의 재고 정보 업데이트 요청.")
    public static class updateRequest {
        @Schema(description = "변경할 거래 일시 (문자열)", example = "2026-05-16 14:30")
        private String createAt;
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
        @Schema(description = "보조석 포함 여부", example = "true")
        private boolean assistantStone;
        @Schema(description = "보조석 ID", example = "10")
        private String assistantStoneId;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantStoneCreateAt;
        @Schema(description = "스톤 정보 목록")
        private List<StoneDto.StoneInfo> stoneInfos;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;

    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "판매 검색 조건 — 검색어/기간/재질.")
    public static class Condition {
        @Schema(description = "검색어 (상품명/거래처명 등)", example = "다이아")
        private String input;
        @Schema(description = "시작 일자 (yyyy-MM-dd)", example = "2026-05-01")
        private String startAt;
        @Schema(description = "종료 일자 (yyyy-MM-dd)", example = "2026-05-31")
        private String endAt;
        @Schema(description = "재질 필터", example = "18K")
        private String material;
    }

    /**
     * 판매 거래처 정보 (메시지 전송용).
     * 날짜 범위 내 판매된 고유한 거래처 ID/이름만 반환.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "판매 거래처 요약 — 기간 내 판매가 발생한 고유 거래처(메시지 전송용).")
    public static class SaleStoreInfo {
        @Schema(description = "거래처(매장) ID", example = "10")
        private Long storeId;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @Schema(description = "판매 상세 (영수증/내역) — 한 라인의 전체 거래 정보.")
    public static class SaleDetailDto {
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private Long flowCode;
        @Schema(description = "판매 일시 (문자열)", example = "2026-05-16 14:30")
        private String saleCreateAt;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "재질 이름", example = "18K")
        private String productMaterial;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String productColor;
        @Schema(description = "재고 메인 스톤 비고", example = "1.0ct VS1")
        private String stockMainStoneNote;
        @Schema(description = "재고 보조 스톤 비고", example = "0.05ct x 12")
        private String stockAssistanceStoneNote;
        @Schema(description = "상품 사이즈", example = "15호")
        private String productSize;
        @Schema(description = "재고 비고")
        private String stockNote;
        @Schema(description = "금 무게 (g)", example = "3.250")
        private BigDecimal goldWeight;
        @Schema(description = "스톤 무게 (g)", example = "0.500")
        private BigDecimal stoneWeight;
        @Schema(description = "메인 스톤 개수", example = "1")
        private Integer mainStoneQuantity;
        @Schema(description = "보조 스톤 개수", example = "12")
        private Integer assistanceStoneQuantity;
        @Schema(description = "상품 공임", example = "120000")
        private Integer productLaborCost;
        @Schema(description = "상품 추가 공임", example = "20000")
        private Integer productAddLaborCost;
        @Schema(description = "메인 스톤 공임", example = "200000")
        private Integer mainStoneLaborCost;
        @Schema(description = "보조 스톤 공임", example = "50000")
        private Integer assistanceStoneLaborCost;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer addStoneLaborCost;
        @Schema(description = "보조석 포함 여부", example = "true")
        private Boolean assistantStone;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantStoneName;
        @Schema(description = "보조석 생성 일자", example = "2026-05-16")
        private LocalDate assistantStoneCreateAt;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;

        @QueryProjection
        public SaleDetailDto(Long flowCode, String saleCreateAt, String productName, String productMaterial, String productColor, String stockMainStoneNote, String stockAssistanceStoneNote, String productSize, String stockNote, BigDecimal goldWeight, BigDecimal stoneWeight, Integer mainStoneQuantity, Integer assistanceStoneQuantity, Integer productLaborCost, Integer productAddLaborCost, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer addStoneLaborCost, Boolean assistantStone, String assistantStoneName, LocalDateTime assistantStoneCreateAt, String storeName) {
            this.flowCode = flowCode;
            this.saleCreateAt = saleCreateAt;
            this.productName = productName;
            this.productMaterial = productMaterial;
            this.productColor = productColor;
            this.stockMainStoneNote = stockMainStoneNote;
            this.stockAssistanceStoneNote = stockAssistanceStoneNote;
            this.productSize = productSize;
            this.stockNote = stockNote;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.mainStoneQuantity = mainStoneQuantity;
            this.assistanceStoneQuantity = assistanceStoneQuantity;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.mainStoneLaborCost = mainStoneLaborCost;
            this.assistanceStoneLaborCost = assistanceStoneLaborCost;
            this.addStoneLaborCost = addStoneLaborCost;
            this.assistantStone = assistantStone;
            this.assistantStoneName = assistantStoneName;
            this.assistantStoneCreateAt = LocalDateTimeToLocalDate(assistantStoneCreateAt);
            this.storeName = storeName;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "라인(flowCode)별 스톤 개수 집계 — 메인/보조 별 총 개수.")
    public static class StoneCountDto {
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private Long flowCode;
        @Schema(description = "메인 스톤 여부", example = "true")
        private Boolean mainStone;
        @Schema(description = "총 스톤 개수", example = "12")
        private Integer totalQuantity;

        @QueryProjection
        public StoneCountDto(Long flowCode, Boolean mainStone, Integer totalQuantity) {
            this.flowCode = flowCode;
            this.mainStone = mainStone;
            this.totalQuantity = totalQuantity;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "판매 세션 금 시세 설정 요청.")
    public static class GoldPriceRequest {
        @Schema(description = "판매 세션 금 시세 (원/g)", example = "85000")
        private Integer accountGoldPrice;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "과거 판매 호출 요청 — saleCode/displayCode 로 기존 판매 세션을 재조회/연장.")
    public static class PastSaleRequest {
        @Schema(description = "판매 세션 코드", example = "445823472384938240")
        private String saleCode;
        @Schema(description = "사용자 표시용 주문장 코드 (YYMMDDNN)", example = "2605160001")
        private String displayCode;
        @Schema(description = "판매 세션 금 시세 (원/g)", example = "85000")
        private Integer accountGoldPrice;

        public PastSaleRequest(String saleCode, String displayCode, Integer accountGoldPrice) {
            this.saleCode = saleCode;
            this.displayCode = displayCode;
            this.accountGoldPrice = accountGoldPrice;
        }
    }

}
