package com.msa.jewelry.account.api;

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
public record StoreView(
        Long storeId,
        String storeName,
        String storeGrade,
        String storeHarry,
        String tradeType,
        boolean applyPastSales
) {
}
