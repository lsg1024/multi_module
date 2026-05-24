package com.msa.jewelry.local.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

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
