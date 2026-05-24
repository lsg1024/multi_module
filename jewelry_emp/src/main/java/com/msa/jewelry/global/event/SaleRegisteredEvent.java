package com.msa.jewelry.global.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleRegisteredEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String saleCode,
        Long storeId,
        String storeName,
        String materialName,
        BigDecimal pureGoldDelta,
        Long moneyDelta
) implements DomainEvent {
}
