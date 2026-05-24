package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다른 모듈이 거래처를 참조할 때 사용하는 불변 view DTO.
 *
 * <p>internal 패키지의 {@code Store} 엔티티를 직접 노출하면 모듈 경계가
 * 붕괴되므로 반드시 view 로 변환해 반환한다.
 *
 * <p>주의: {@code storeHarry} 는 {@code String} 타입이다 — 원본 엔티티
 * {@code CommonOption.goldHarryLoss} 가 String 으로 저장되어 있으므로 그대로 노출.
 * 호출 측에서 BigDecimal 이 필요하면 {@code new BigDecimal(view.storeHarry())} 로 변환.
 */
@Schema(description = "거래처(매장) 정보 외부 노출용 view — 다른 모듈이 거래처를 참조할 때 사용")
public record StoreView(
        @Schema(description = "거래처 PK", example = "10")
        Long storeId,
        @Schema(description = "거래처명", example = "강남금은방")
        String storeName,
        @Schema(description = "거래처 등급 (A/B/C 등)", example = "A")
        String storeGrade,
        @Schema(description = "거래처 수수료(허리) — 문자열로 보관된 손모율", example = "1.5")
        String storeHarry,
        @Schema(description = "거래 유형 (BUY/SELL)", example = "SELL")
        String tradeType,
        @Schema(description = "과거 판매분 적용 여부", example = "false")
        boolean applyPastSales
) {
}
