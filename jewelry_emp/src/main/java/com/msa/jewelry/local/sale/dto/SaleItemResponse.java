package com.msa.jewelry.local.sale.dto;

import com.msa.jewelry.local.order.dto.StatusHistoryDto;
import com.msa.jewelry.global.util.GoldUtils;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@NoArgsConstructor
@Schema(description = "판매 영수증 그룹 응답 — 같은 거래처+주문장 단위로 묶인 판매 라인들.")
public class SaleItemResponse {

    @Schema(description = "거래 일시 (문자열)", example = "2026-05-16 14:30")
    private String createAt;
    @Schema(description = "거래 등록자 (사용자 이름)", example = "홍길동")
    private String createBy;
    @Schema(description = "거래처(매장) ID", example = "10")
    private String storeId;
    @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
    private String storeName;
    @Schema(description = "거래처 수수료(허리, 문자열)", example = "1.50")
    private String storeHarry;
    @Schema(description = "거래처 코드", example = "S0001")
    private String storeCode;
    @Schema(description = "사용자 표시용 주문장 코드 (YYMMDDNN)", example = "2605160001")
    private String displayCode;
    @Schema(description = "판매 세션 금 시세 (원/g, 문자열)", example = "85000")
    private String accountGoldPrice;
    @Schema(description = "이 영수증의 판매 라인 목록")
    private List<SaleItem> saleItems;

    @Builder
    @QueryProjection
    public SaleItemResponse(String createAt, String createBy, String storeId, String storeName, String storeHarry, String storeCode, String displayCode, String accountGoldPrice, List<SaleItem> saleItems) {
        this.createAt = createAt;
        this.createBy = createBy;
        this.storeId = storeId;
        this.storeName = storeName;
        this.storeHarry = storeHarry;
        this.storeCode = storeCode;
        this.displayCode = displayCode;
        this.accountGoldPrice = accountGoldPrice;
        this.saleItems = saleItems;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "판매 라인 상세 — 한 라인의 영수증/이력/이미지 포함 응답.")
    public static class SaleItem {
        @Schema(description = "거래 일시 (문자열)", example = "2026-05-16 14:30")
        private String createAt;
        @Schema(description = "거래 등록자", example = "홍길동")
        private String createBy;
        @Schema(description = "거래 유형 표시명", example = "판매")
        private String saleType;
        @Schema(description = "거래처(매장) ID", example = "10")
        private String storeId;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "상품 ID — 대표 이미지 매핑 키", example = "501")
        private Long productId;
        @Schema(description = "판매 세션 코드 (TSID)", example = "445823472384938240")
        private String saleCode;
        @Schema(description = "사용자 표시용 주문장 코드", example = "2605160001")
        private String displayCode;
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "상품 이미지 경로", example = "/images/products/501.jpg")
        private String imagePath = "";
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "비고 (재고+메인+보조 비고를 줄바꿈으로 결합)")
        private String note;
        @Schema(description = "보조석 포함 여부", example = "true")
        private Boolean assistantStone;
        @Schema(description = "보조석 이름", example = "큐빅")
        private String assistantName;
        @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
        private String assistantCreateAt;
        @Schema(description = "금 무게 (g)", example = "3.250")
        private BigDecimal goldWeight;
        @Schema(description = "스톤 무게 (g)", example = "0.500")
        private BigDecimal stoneWeight;
        @Schema(description = "순금 환산 무게 (g) — 재질/해리 적용", example = "2.500")
        private BigDecimal pureGoldWeight;
        @Schema(description = "상품 공임 + 추가 공임 합계", example = "140000")
        private Integer totalProductLaborCost;
        @Schema(description = "메인 스톤 공임", example = "200000")
        private Long mainStoneLaborCost;
        @Schema(description = "보조 스톤 공임", example = "50000")
        private Long assistanceStoneLaborCost;
        @Schema(description = "추가 스톤 공임", example = "30000")
        private Integer stoneAddLaborCost;
        @Schema(description = "메인 스톤 개수", example = "1")
        private Long mainStoneQuantity;
        @Schema(description = "보조 스톤 개수", example = "12")
        private Long assistanceStoneQuantity;
        @Schema(description = "상태 이력 목록 (시계열)")
        private List<StatusHistoryDto> statusHistories;

        @QueryProjection
        public SaleItem(String createAt, String createBy, String saleType, String storeId, String storeName, String saleCode, String displayCode, String flowCode, String productName, String materialName, String colorName, String stockNote, String mainNote, String subNote, Boolean assistantStone, String assistantName, BigDecimal goldWeight, BigDecimal stoneWeight, BigDecimal harry, Integer productLabor, Integer productAddLabor, String assistantCreateAt, Long mainStoneLabor, Long asstStoneLabor, Integer stoneAddLabor, Long mainQty, Long asstQty, Long productId) {
            this.createAt = createAt;
            this.createBy = createBy;
            this.saleType = saleType;
            this.storeId = storeId;
            this.storeName = storeName;
            this.saleCode = saleCode;
            this.displayCode = displayCode;
            this.flowCode = flowCode;
            this.productName = productName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.assistantCreateAt = assistantCreateAt;
            this.note = Stream.of(stockNote, mainNote, subNote)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n"));
            this.assistantStone = assistantStone;
            this.assistantName = assistantName;
            BigDecimal validGoldWeight = (goldWeight == null) ? BigDecimal.ZERO : goldWeight;
            BigDecimal validStoneWeight = (stoneWeight == null) ? BigDecimal.ZERO : stoneWeight;
            this.goldWeight = validGoldWeight;
            this.stoneWeight = validStoneWeight;
            this.pureGoldWeight = GoldUtils.calculatePureGoldWeightAndHarry(validGoldWeight, materialName, harry);
            int pLabor = (productLabor == null) ? 0 : productLabor;
            int pAddLabor = (productAddLabor == null) ? 0 : productAddLabor;
            this.totalProductLaborCost = pLabor + pAddLabor;
            this.mainStoneLaborCost = (mainStoneLabor == null) ? 0 : mainStoneLabor;
            this.assistanceStoneLaborCost = (asstStoneLabor == null) ? 0 : asstStoneLabor;
            this.stoneAddLaborCost = (stoneAddLabor == null) ? 0 : stoneAddLabor;
            this.mainStoneQuantity = (mainQty == null) ? 0 : mainQty;
            this.assistanceStoneQuantity = (asstQty == null) ? 0 : asstQty;
            this.productId = productId;
        }

        public void updateImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public void updateHistory(List<StatusHistoryDto> statusHistories) {
            this.statusHistories = statusHistories;
        }
    }

}