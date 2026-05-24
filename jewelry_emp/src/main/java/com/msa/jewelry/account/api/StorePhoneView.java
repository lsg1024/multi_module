package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 거래처 전화번호 view (SMS 발송 시 user 모듈이 조회).
 *
 * <p>기존 MSA 의 {@code AccountClient.getStorePhones} 응답
 * ({@code MessageDto.StorePhoneInfo}) 의 동기 등가물.
 */
@Schema(description = "거래처 전화번호 외부 노출용 view — user 모듈 SMS 발송 시 사용")
public record StorePhoneView(
        @Schema(description = "거래처 PK", example = "10")
        Long storeId,
        @Schema(description = "거래처명", example = "강남금은방")
        String storeName,
        @Schema(description = "거래처 전화번호", example = "010-1234-5678")
        String storePhoneNumber
) {
}
