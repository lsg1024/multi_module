package com.msa.jewelry.account.internal.global.kafka_dto_legacy;

public record GoldHarryLossUpdatedEvent(String tenantId, Long goldHarryId, String newGoldHarryLoss) {}
