package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 판매 시점의 거래처 미수금 잔액 변화를 외부 모듈(order)에 노출하는 view.
 *
 * <p>기존 MSA 의 {@code AccountFeignClient.getStoreReceivableDetailLog} 응답
 * ({@code StoreDto.accountResponse}) 의 동기 등가물이며, 호출자가 실제 쓰는
 * 5개 필드만 추려서 노출한다.
 *
 * <p>모든 잔액 필드가 {@code String} 인 이유는 원본 엔티티가 정밀도/단위를
 * 문자열로 저장해왔기 때문이며, 호출 측은 그대로 출력에 사용한다.
 *
 * @param previousGoldBalance  판매 직전 금 잔액
 * @param previousMoneyBalance 판매 직전 현금 잔액
 * @param afterGoldBalance     판매 직후 금 잔액
 * @param afterMoneyBalance    판매 직후 현금 잔액
 * @param lastSaleDate         최종 판매일 (yyyy-MM-dd 등 표시용 문자열)
 */
@Schema(description = "판매 시점 거래처 미수금 잔액 변화 view — 판매 직전/직후 금/현금 잔액 + 최종 판매일")
public record StoreReceivableLogView(
        @Schema(description = "판매 직전 금 잔액 (문자열)", example = "12.345")
        String previousGoldBalance,
        @Schema(description = "판매 직전 현금 잔액 (문자열)", example = "1500000")
        String previousMoneyBalance,
        @Schema(description = "판매 직후 금 잔액 (문자열)", example = "15.678")
        String afterGoldBalance,
        @Schema(description = "판매 직후 현금 잔액 (문자열)", example = "2000000")
        String afterMoneyBalance,
        @Schema(description = "최종 판매일 (표시용 문자열)", example = "2026-05-16")
        String lastSaleDate
) {
}
